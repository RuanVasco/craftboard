package dev.craftboard.world;

import static dev.craftboard.nbt.NbtPath.compound;
import static dev.craftboard.nbt.NbtPath.flag;
import static dev.craftboard.nbt.NbtPath.number;
import static dev.craftboard.nbt.NbtPath.string;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import org.springframework.stereotype.Component;

import dev.craftboard.nbt.Nbt;

@Component
public class LevelDatParser {

    private static final long TICKS_PER_DAY = 24_000;

    public WorldInfo parse(byte[] content, Instant capturedAt) throws IOException {
        var data = compound(Nbt.read(content), "Data").orElse(Map.of());
        // DayTime keeps counting across days, so it carries both the day number and the clock.
        var dayTime = number(data, "DayTime").map(Number::longValue).orElse(null);
        return new WorldInfo(
                compound(data, "Version").flatMap(v -> string(v, "Name")).orElse(null),
                dayTime == null ? null : dayTime / TICKS_PER_DAY + 1,
                dayTime == null ? null : dayTime % TICKS_PER_DAY,
                flag(data, "raining"),
                flag(data, "thundering"),
                capturedAt);
    }
}
