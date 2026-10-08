package dev.tickboard.rcon;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;

import dev.tickboard.config.MinecraftProperties;
import lombok.RequiredArgsConstructor;

/**
 * Minimal Source RCON client. One connection, reopened lazily after any failure.
 */
@Component
@RequiredArgsConstructor
public class RconClient {

    private static final int TYPE_RESPONSE = 0;
    private static final int TYPE_COMMAND = 2;
    private static final int TYPE_LOGIN = 3;
    private static final int AUTH_FAILED_ID = -1;
    private static final int TIMEOUT_MS = 5_000;

    private final MinecraftProperties properties;

    private Socket socket;
    private int nextRequestId = 1;

    public synchronized String command(String command) throws IOException {
        try {
            ensureConnected();
            return send(TYPE_COMMAND, command);
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    public synchronized void close() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // nothing left to release
            }
            socket = null;
        }
    }

    private void ensureConnected() throws IOException {
        if (socket != null && socket.isConnected() && !socket.isClosed()) {
            return;
        }
        var rcon = properties.rcon();
        socket = new Socket();
        socket.connect(new InetSocketAddress(rcon.host(), rcon.port()), TIMEOUT_MS);
        socket.setSoTimeout(TIMEOUT_MS);
        send(TYPE_LOGIN, rcon.password());
    }

    private String send(int type, String payload) throws IOException {
        int requestId = nextRequestId++;
        byte[] body = payload.getBytes(StandardCharsets.UTF_8);
        var packet = ByteBuffer.allocate(4 + 4 + 4 + body.length + 2).order(ByteOrder.LITTLE_ENDIAN);
        packet.putInt(4 + 4 + body.length + 2).putInt(requestId).putInt(type).put(body).put((byte) 0).put((byte) 0);

        OutputStream out = socket.getOutputStream();
        out.write(packet.array());
        out.flush();

        var in = new DataInputStream(socket.getInputStream());
        int length = Integer.reverseBytes(in.readInt());
        byte[] response = in.readNBytes(length);
        if (response.length != length) {
            throw new IOException("RCON connection closed mid packet");
        }
        var buffer = ByteBuffer.wrap(response).order(ByteOrder.LITTLE_ENDIAN);
        int responseId = buffer.getInt();
        int responseType = buffer.getInt();
        if (responseId == AUTH_FAILED_ID) {
            throw new IOException("RCON authentication failed");
        }
        if (type == TYPE_COMMAND && responseType != TYPE_RESPONSE) {
            throw new IOException("Unexpected RCON packet type " + responseType);
        }
        return new String(response, 8, length - 10, StandardCharsets.UTF_8);
    }
}
