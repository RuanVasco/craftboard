package dev.craftboard.rcon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import dev.craftboard.config.MinecraftProperties;

class RconClientTest {

    private ServerSocket server;

    @AfterEach
    void tearDown() throws IOException {
        server.close();
    }

    @Test
    void logsInAndRunsCommand() throws IOException {
        startFakeServer("secret");
        var client = new RconClient(properties("secret"));

        assertThat(client.command("list")).isEqualTo("echo:list");
    }

    @Test
    void failsOnWrongPassword() throws IOException {
        startFakeServer("secret");
        var client = new RconClient(properties("wrong"));

        assertThatThrownBy(() -> client.command("list")).hasMessageContaining("authentication");
    }

    private MinecraftProperties properties(String password) {
        var rcon = new MinecraftProperties.Rcon(true, "127.0.0.1", server.getLocalPort(), password,
                Duration.ofSeconds(10), Duration.ofSeconds(60));
        return new MinecraftProperties("../dev-data", "world", "localhost", rcon);
    }

    private void startFakeServer(String password) throws IOException {
        server = new ServerSocket(0);
        Thread.ofVirtual().start(() -> {
            try (var socket = server.accept()) {
                var in = new DataInputStream(socket.getInputStream());
                var out = socket.getOutputStream();
                while (true) {
                    int length = Integer.reverseBytes(in.readInt());
                    var packet = ByteBuffer.wrap(in.readNBytes(length)).order(ByteOrder.LITTLE_ENDIAN);
                    int id = packet.getInt();
                    int type = packet.getInt();
                    var payload = new String(packet.array(), 8, length - 10, StandardCharsets.UTF_8);
                    if (type == 3) {
                        out.write(packet(payload.equals(password) ? id : -1, 2, ""));
                    } else {
                        out.write(packet(id, 0, "echo:" + payload));
                    }
                }
            } catch (IOException ignored) {
                // client disconnected
            }
        });
    }

    private static byte[] packet(int id, int type, String payload) {
        byte[] body = payload.getBytes(StandardCharsets.UTF_8);
        return ByteBuffer.allocate(14 + body.length).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(10 + body.length).putInt(id).putInt(type).put(body).put((byte) 0).put((byte) 0)
                .array();
    }
}
