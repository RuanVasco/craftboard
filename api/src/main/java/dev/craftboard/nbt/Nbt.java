package dev.craftboard.nbt;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Minimal reader for the named binary tag format used by {@code level.dat} and player data.
 * Compounds become maps, lists become lists, numbers keep their Java type.
 */
public final class Nbt {

    private static final int END = 0;
    private static final int BYTE = 1;
    private static final int SHORT = 2;
    private static final int INT = 3;
    private static final int LONG = 4;
    private static final int FLOAT = 5;
    private static final int DOUBLE = 6;
    private static final int BYTE_ARRAY = 7;
    private static final int STRING = 8;
    private static final int LIST = 9;
    private static final int COMPOUND = 10;
    private static final int INT_ARRAY = 11;
    private static final int LONG_ARRAY = 12;

    // Real files stay far below this. It only guards against a corrupt length allocating gigabytes.
    private static final int MAX_LENGTH = 1 << 24;

    private Nbt() {
    }

    /** Reads the root compound, gzipped (as the server writes it) or not. */
    public static Map<String, Object> read(byte[] content) throws IOException {
        InputStream in = new ByteArrayInputStream(content);
        if (content.length > 1 && (content[0] & 0xff) == 0x1f && (content[1] & 0xff) == 0x8b) {
            in = new GZIPInputStream(in);
        }
        try (var data = new DataInputStream(in)) {
            if (data.readByte() != COMPOUND) {
                throw new IOException("Root tag is not a compound");
            }
            data.readUTF();
            return readCompound(data);
        }
    }

    private static Object readPayload(DataInputStream in, int type) throws IOException {
        return switch (type) {
            case BYTE -> in.readByte();
            case SHORT -> in.readShort();
            case INT -> in.readInt();
            case LONG -> in.readLong();
            case FLOAT -> in.readFloat();
            case DOUBLE -> in.readDouble();
            case BYTE_ARRAY -> {
                var bytes = new byte[length(in)];
                in.readFully(bytes);
                yield bytes;
            }
            case STRING -> in.readUTF();
            case LIST -> readList(in);
            case COMPOUND -> readCompound(in);
            case INT_ARRAY -> {
                var ints = new int[length(in)];
                for (int i = 0; i < ints.length; i++) {
                    ints[i] = in.readInt();
                }
                yield ints;
            }
            case LONG_ARRAY -> {
                var longs = new long[length(in)];
                for (int i = 0; i < longs.length; i++) {
                    longs[i] = in.readLong();
                }
                yield longs;
            }
            default -> throw new IOException("Unknown tag type " + type);
        };
    }

    private static Map<String, Object> readCompound(DataInputStream in) throws IOException {
        Map<String, Object> compound = new LinkedHashMap<>();
        int type;
        while ((type = in.readUnsignedByte()) != END) {
            var name = in.readUTF();
            compound.put(name, readPayload(in, type));
        }
        return compound;
    }

    private static List<Object> readList(DataInputStream in) throws IOException {
        int type = in.readUnsignedByte();
        int size = length(in);
        List<Object> list = new ArrayList<>(Math.min(size, 1024));
        for (int i = 0; i < size; i++) {
            list.add(readPayload(in, type));
        }
        return list;
    }

    private static int length(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length < 0 || length > MAX_LENGTH) {
            throw new IOException("Invalid length " + length);
        }
        return length;
    }
}
