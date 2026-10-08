package dev.craftboard.nbt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class NbtTest {

    @Test
    void readsNestedCompoundsListsAndNumbers() throws IOException {
        var root = Nbt.read(NbtWriter.gzipped(Map.of(
                "Data", Map.of(
                        "DayTime", 48_000L,
                        "raining", (byte) 1,
                        "Version", Map.of("Name", "1.21.10"),
                        "Inventory", List.of(Map.of("id", "minecraft:torch", "count", 64))),
                "Health", 17.5f)));

        var data = NbtPath.compound(root, "Data").orElseThrow();
        assertThat(NbtPath.number(data, "DayTime")).contains(48_000L);
        assertThat(NbtPath.flag(data, "raining")).isTrue();
        assertThat(NbtPath.compound(data, "Version").flatMap(v -> NbtPath.string(v, "Name"))).contains("1.21.10");
        assertThat(NbtPath.list(data, "Inventory").orElseThrow()).singleElement()
                .isEqualTo(Map.of("id", "minecraft:torch", "count", 64));
        assertThat(NbtPath.number(root, "Health")).contains(17.5f);
    }

    @Test
    void missingKeysAreEmpty() throws IOException {
        var root = Nbt.read(NbtWriter.gzipped(Map.of()));

        assertThat(NbtPath.compound(root, "Data")).isEmpty();
        assertThat(NbtPath.flag(root, "raining")).isFalse();
    }

    @Test
    void rejectsGarbage() {
        assertThatThrownBy(() -> Nbt.read(new byte[] {1, 2, 3})).isInstanceOf(IOException.class);
    }
}
