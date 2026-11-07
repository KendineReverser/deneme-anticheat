package ac.deneme.anticheat.utils.anticheat;

import ac.deneme.anticheat.checks.Check;

public final class CheckCategories {

    private CheckCategories() {
    }

    public static String of(Check check) {
        String stableKey = check.getStableKey();
        int first = stableKey.indexOf('.');
        int second = first < 0 ? -1 : stableKey.indexOf('.', first + 1);
        String group = first >= 0 && second > first ? stableKey.substring(first + 1, second) : stableKey;

        return switch (group) {
            case "aim", "combat", "multiactions" -> "killaura";
            case "scaffolding" -> "scaffold";
            case "velocity" -> "velocity";
            case "prediction", "movement", "flight", "elytra", "vehicle", "sprint", "groundspoof", "timer" -> "movement";
            case "badpackets", "packetorder", "crash", "exploit" -> "packet";
            case "breaking" -> "breaking";
            case "baritone" -> "bot";
            case "chat" -> "chat";
            default -> group;
        };
    }
}
