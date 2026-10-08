package dev.craftboard.server;

public interface ServerService {

    ServerInfo info();

    /** Pushes the current info to dashboard clients, for changes that happen on disk. */
    void broadcastInfo();
}
