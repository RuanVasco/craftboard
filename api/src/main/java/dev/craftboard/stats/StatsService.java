package dev.craftboard.stats;

import java.util.List;
import java.util.UUID;

public interface StatsService {

    List<PlayerStats> all();

    void reloadAll();

    void refresh(UUID uuid);
}
