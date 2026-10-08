package dev.craftboard.activity;

import java.util.UUID;

/** Play time a player gained between two consecutive snapshots, at the time of the later one. */
public interface PlayTimeGain {

    UUID getPlayerUuid();

    double getCapturedAtEpoch();

    double getTicks();
}
