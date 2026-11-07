package ac.deneme.anticheat.internal.storage.codec.bson;

import ac.deneme.anticheat.api.storage.codec.EncodeShape;
import ac.deneme.anticheat.internal.storage.codec.MethodHandleCodec;
import ac.deneme.anticheat.internal.storage.codec.TypeTag;
import org.bson.BsonBinarySubType;
import org.bson.BsonWriter;
import org.bson.Document;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Regular-collection BSON codec. Wraps a base {@link MethodHandleCodec}
 * and adds the streaming encode + Document decode loops.
 * <p>
 * Encode is allocation-free for primitive fields: {@code BsonWriter}
 * primitive-typed writes (writeInt32 / writeInt64 / writeDouble /
 * writeBoolean) take the value directly, no boxing. Reference fields
 * allocate exactly the wire bytes they need (Strings reuse their char
 * array; UUIDs allocate one 16-byte buffer plus a small {@code BsonBinary}
 * wrapper, both poolable but not pooled today).
 * <p>
 * Every encoded document starts with {@code _v} (the codec version) so
 * schema evolution on read can route by version.
 */
@ApiStatus.Internal
public final class BsonCodecImpl<R> extends MethodHandleCodec<R> implements BsonCodec<R> {

    /** Top-level version field name; reserved across every encoded record. */
    public static final String VERSION_FIELD = "_v";

    public BsonCodecImpl(@NotNull Class<R> recordType, @NotNull EncodeShape shape) {
        super(recordType, shape);
    }

    @Override
    public void encode(@NotNull R record, @NotNull BsonWriter w) {
        w.writeStartDocument();
        encodeFields(record, w);
        w.writeEndDocument();
    }

    @Override
    public void encodeFields(@NotNull R record, @NotNull BsonWriter w) {
        w.writeInt32(VERSION_FIELD, version());
        final int n = fieldNames.length;
        for (int i = 0; i < n; i++) {
            writeField(record, i, w);
        }
    }

    @Override
    public Object readField(@NotNull R record, int fieldIndex) {

        return super.readField(record, fieldIndex);
    }

    private void writeField(@NotNull R record, int i, @NotNull BsonWriter w) {
        String name = fieldNames[i];
        switch (typeTags[i]) {
            case INT     -> w.writeInt32(name, readInt(record, i));
            case LONG    -> w.writeInt64(name, readLong(record, i));
            case DOUBLE,
                 FLOAT   -> w.writeDouble(name, readDouble(record, i));
            case ENUM    -> w.writeInt32(name, readEnumOrdinal(record, i));
            case BOOLEAN -> {
                Boolean v = (Boolean) readRef(record, i);
                if (v != null) {
                    w.writeBoolean(name, v);
                } else if (!nullable[i]) {
                    throwMissingNonNullable(i);
                }
            }
            case STRING -> {
                String s = (String) readRef(record, i);
                if (s != null) {
                    w.writeString(name, s);
                } else if (!nullable[i]) {
                    throwMissingNonNullable(i);
                }
            }
            case BYTES -> {
                byte[] b = (byte[]) readRef(record, i);
                if (b != null) {
                    w.writeBinaryData(name, new org.bson.BsonBinary(b));
                } else if (!nullable[i]) {
                    throwMissingNonNullable(i);
                }
            }
            case UUID -> {
                UUID u = (UUID) readRef(record, i);
                if (u != null) {
                    w.writeBinaryData(name, BsonBinaries.uuidBinary(u));
                } else if (!nullable[i]) {
                    throwMissingNonNullable(i);
                }
            }
            case FLOAT_ARRAY -> {
                float[] v = (float[]) readRef(record, i);
                if (v != null) {
                    BsonBinaries.writeFloatArray(name, v, w);
                } else if (!nullable[i]) {
                    throwMissingNonNullable(i);
                }
            }
            case NESTED_SEALED -> {

                Object v = readRef(record, i);
                if (v != null) {
                    throw new UnsupportedOperationException(
                        "NESTED_SEALED encoding not yet implemented (field " + name + "); ships with Phase 6");
                } else if (!nullable[i]) {
                    throwMissingNonNullable(i);
                }
            }
        }
    }

    private void throwMissingNonNullable(int i) {
        throw new IllegalStateException(
            "null value for non-nullable field " + fieldNames[i] + " on " + recordType().getName());
    }

    @Override
    public @NotNull R decode(@NotNull Document source) {

        final int n = fieldNames.length;
        Object[] args = freshArgsArray();
        for (int i = 0; i < n; i++) {
            Object raw = source.get(fieldNames[i]);
            args[recordPositionAt(i)] = BsonDecodeHelper.decodeField(this, i, raw);
        }
        return construct(args);
    }

    @Override
    public @NotNull R decodeFromValues(Object @NotNull [] argsByShapeIndex) {
        if (argsByShapeIndex.length != fieldNames.length) {
            throw new IllegalArgumentException(
                "argsByShapeIndex length " + argsByShapeIndex.length
                    + " != shape.fields() count " + fieldNames.length);
        }
        Object[] args = freshArgsArray();
        for (int i = 0; i < fieldNames.length; i++) {

            args[recordPositionAt(i)] = requireFieldOrDefault(i, argsByShapeIndex[i]);
        }
        return construct(args);
    }
}
