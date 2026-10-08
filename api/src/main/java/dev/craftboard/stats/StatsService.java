package dev.craftboard.stats;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface StatsService {

    List<PlayerStats> all();

    /** Full stats files of every known player, for breakdowns the summary does not carry. */
    Map<UUID, RawStats> raw();

    void reloadAll();

    void refresh(UUID uuid);
}
