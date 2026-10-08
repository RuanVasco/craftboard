package dev.craftboard.world;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

import dev.craftboard.nbt.NbtWriter;

class LevelDatParserTest {

    private final LevelDatParser parser = new LevelDatParser();

    @Test
    void readsClockWeatherAndVersion() throws IOException {
        var info = parser.parse(NbtWriter.gzipped(Map.of("Data", Map.of(
                "DayTime", 24_000L * 187 + 13_500,
                "raining", (byte) 1,
                "thundering", (byte) 0,
                "Version", Map.of("Name", "1.21.10")))), Instant.EPOCH);

        assertThat(info).isEqualTo(new WorldInfo("1.21.10", 188L, 13_500L, true, false, Instant.EPOCH));
    }

    @Test
    void missingFieldsAreNull() throws IOException {
        var info = parser.parse(NbtWriter.gzipped(Map.of("Data", Map.of())), Instant.EPOCH);

        assertThat(info).isEqualTo(new WorldInfo(null, null, null, false, false, Instant.EPOCH));
    }
}
