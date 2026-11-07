package ac.deneme.anticheat.utils.data.webhook.discord;

import ac.deneme.anticheat.DenemeAPI;
import ac.deneme.anticheat.api.DenemeUser;
import ac.deneme.anticheat.player.DenemePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record CompiledDiscordTemplate(Segment[] segments) {

    /**
     * Markdown context as determined by a state-machine scan of the template.
     * Used only at compile time.
     */
    private enum MarkdownContext {
        NORMAL,
        INLINE_CODE,
        CODE_BLOCK
    }

    /**
     * Escape strategy assigned to each placeholder at compile time.
     */
    public enum EscapeMode {
        /**
         * Full Markdown escaping: \, `, *, _, ~, |, [], (), line-start chars, newlines
         */
        FULL_MARKDOWN,
        /**
         * Inside a code span: only backtick substitution (backslash escaping doesn't work in Discord code spans)
         */
        CODE_SPAN,
        /**
         * No escaping (placeholder left raw for PAPI)
         */
        NONE
    }

    private static final Pattern PLACEHOLDER = Pattern.compile("%([a-zA-Z0-9_]+)%");

    private sealed interface Segment permits Literal, Placeholder {}

    private record Literal(String text) implements Segment {}

    private record Placeholder(String key, EscapeMode mode) implements Segment {}

    public static CompiledDiscordTemplate compile(String template) {
        List<Segment> parts = new ArrayList<>();
        Matcher m = PLACEHOLDER.matcher(template);
        MarkdownContext ctx = MarkdownContext.NORMAL;
        int lastEnd = 0;

        while (m.find()) {
            String gap = template.substring(lastEnd, m.start());
            if (!gap.isEmpty()) parts.add(new Literal(gap));

            ctx = advanceContext(ctx, gap);

            EscapeMode mode = switch (ctx) {
                case NORMAL -> EscapeMode.FULL_MARKDOWN;
                case INLINE_CODE,
                     CODE_BLOCK -> EscapeMode.CODE_SPAN;
            };
            parts.add(new Placeholder(m.group(0), mode));
            lastEnd = m.end();
        }

        if (lastEnd < template.length()) {
            parts.add(new Literal(template.substring(lastEnd)));
        }
        return new CompiledDiscordTemplate(parts.toArray(Segment[]::new));
    }

    /**
     * @param player              Deneme player (nullable)
     * @param statics             Static replacements (may include per-alert overrides like %check%)
     * @param dynamics            Dynamic replacements (lazy functions like %tps%)
     * @param backtickReplacement Char to substitute for backticks inside code spans (loaded from config)
     */
    public String render(@NotNull DenemePlayer player,
                         @NotNull Map<String, String> statics,
                         @NotNull Map<String, Function<DenemeUser, String>> dynamics,
                         char backtickReplacement) {
        StringBuilder sb = new StringBuilder(segments.length * 32);
        for (Segment seg : segments) {
            if (seg instanceof Literal l) {
                sb.append(l.text);
            } else if (seg instanceof Placeholder p) {

                String val = statics.get(p.key);

                if (val == null) {
                    Function<DenemeUser, String> fn = dynamics.get(p.key);
                    if (fn != null) val = fn.apply(player);
                }

                if (val == null) {
                    String resolved = DenemeAPI.INSTANCE.getMessagePlaceHolderManager()
                            .replacePlaceholders(player.platformPlayer, p.key);
                    if (!resolved.equals(p.key)) val = resolved;
                }

                if (val != null) {
                    sb.append(escape(val, p.mode, backtickReplacement));
                } else {
                    sb.append(p.key);
                }
            }
        }
        return sb.toString();
    }

    private static String escape(String value, EscapeMode mode, char backtickReplacement) {
        if (mode == EscapeMode.NONE) return value;
        if (mode == EscapeMode.CODE_SPAN) return escapeCodeSpan(value, backtickReplacement);
        return escapeMarkdown(value);
    }

    /**
     * Full Discord Markdown escaping for untrusted data values.
     * <p>
     * This method treats input as <b>raw data</b>, not pre-formatted markdown.
     * A literal backslash in a player name is data and must render as a visible backslash.
     * This means "already escaped" input like {@code \*} correctly becomes {@code \\*}
     * (rendering as {@code \*}), which is the intended behavior — identical to SQL parameterization.
     * <p>
     * All escaped characters ({@code \X}) render identically to their unescaped form ({@code X})
     * in Discord's CommonMark variant, so escaping is invisible to end users.
     */
    public static String escapeMarkdown(String s) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {

                case '\\' -> sb.append("\\\\");

                case '`' -> sb.append("\\`");

                case '*' -> sb.append("\\*");

                case '_' -> sb.append("\\_");

                case '~' -> sb.append("\\~");

                case '|' -> sb.append("\\|");

                case '[' -> sb.append("\\[");
                case ']' -> sb.append("\\]");
                case '(' -> sb.append("\\(");
                case ')' -> sb.append("\\)");

                case ':' -> sb.append("\\:");

                case '<' -> sb.append("\\<");

                case '#' -> sb.append("\\#");
                case '>' -> sb.append("\\>");

                case '-' -> sb.append("\\-");

                case '.' -> sb.append("\\.");

                case '\n' -> sb.append("\\n");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Code span escaping: Discord ignores backslash escapes inside code spans,
     * so backticks are replaced with a visually-similar substitute character.
     * If replacement == '`', this is a no-op (user disabled the feature).
     */
    public static String escapeCodeSpan(String s, char replacement) {
        if (s == null || s.isEmpty() || replacement == '`') return s;
        return s.replace('`', replacement);
    }

    /**
     * Advances Markdown context through literal template text.
     * Handles: backslash escapes (NORMAL only), single-` inline code, triple-` code blocks.
     * <p>
     * Edge cases resolved:
     * <pre>
     *   `text %p% text`  → INLINE_CODE  (` opens before placeholder)
     *   `a`%p%`b`        → NORMAL       (` opens then ` closes before placeholder)
     *   \`%p%`           → NORMAL       (backtick is backslash-escaped)
     * </pre>
     * <b>Known limitation:</b> Double-backtick code spans ({@code `` text ``}) are not tracked.
     */
    private static MarkdownContext advanceContext(MarkdownContext ctx, String text) {
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (ctx == MarkdownContext.NORMAL) {
                if (c == '\\' && i + 1 < text.length()) {
                    i += 2;
                    continue;
                }
                if (c == '`') {
                    if (i + 2 < text.length()
                            && text.charAt(i + 1) == '`'
                            && text.charAt(i + 2) == '`') {
                        ctx = MarkdownContext.CODE_BLOCK;
                        i += 3;
                        continue;
                    }
                    ctx = MarkdownContext.INLINE_CODE;
                }
            } else if (ctx == MarkdownContext.INLINE_CODE) {
                if (c == '`') ctx = MarkdownContext.NORMAL;
            } else {
                if (c == '`'
                        && i + 2 < text.length()
                        && text.charAt(i + 1) == '`'
                        && text.charAt(i + 2) == '`') {
                    ctx = MarkdownContext.NORMAL;
                    i += 3;
                    continue;
                }
            }
            i++;
        }
        return ctx;
    }
}
