package dev.craftboard.nbt;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/** Test helper that writes plain Java values as gzipped NBT, the way the server stores them. */
public final class NbtWriter {

    private NbtWriter() {
    }

    public static byte[] gzipped(Map<String, Object> root) {
        try {
            var bytes = new ByteArrayOutputStream();
            try (var out = new DataOutputStream(new GZIPOutputStream(bytes))) {
                out.writeByte(10);
                out.writeUTF("");
                writeCompound(out, root);
            }
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writeCompound(DataOutputStream out, Map<?, ?> compound) throws IOException {
        for (var entry : compound.entrySet()) {
            out.writeByte(type(entry.getValue()));
            out.writeUTF((String) entry.getKey());
            writePayload(out, entry.getValue());
        }
        out.writeByte(0);
    }

    private static void writePayload(DataOutputStream out, Object value) throws IOException {
        switch (value) {
            case Byte b -> out.writeByte(b);
            case Short s -> out.writeShort(s);
            case Integer i -> out.writeInt(i);
            case Long l -> out.writeLong(l);
            case Float f -> out.writeFloat(f);
            case Double d -> out.writeDouble(d);
            case String s -> out.writeUTF(s);
            case List<?> list -> {
                out.writeByte(list.isEmpty() ? 0 : type(list.getFirst()));
                out.writeInt(list.size());
                for (var item : list) {
                    writePayload(out, item);
                }
            }
            case Map<?, ?> map -> writeCompound(out, map);
            case long[] longs -> {
                out.writeInt(longs.length);
                for (var l : longs) {
                    out.writeLong(l);
                }
            }
            default -> throw new IllegalArgumentException("Unsupported " + value.getClass());
        }
    }

    private static int type(Object value) {
        return switch (value) {
            case Byte b -> 1;
            case Short s -> 2;
            case Integer i -> 3;
            case Long l -> 4;
            case Float f -> 5;
            case Double d -> 6;
            case String s -> 8;
            case List<?> l -> 9;
            case Map<?, ?> m -> 10;
            case long[] l -> 12;
            default -> throw new IllegalArgumentException("Unsupported " + value.getClass());
        };
    }
}
