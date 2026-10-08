package dev.craftboard.feed;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

class LogLineParserTest {

    private static final Set<String> PLAYERS = Set.of("Steve", "Alex");

    @Test
    void readsJoinsLeavesAndAdvancements() {
        assertThat(parse("[18:22:31] [Server thread/INFO]: Steve joined the game"))
                .contains(new LogLine(LocalTime.of(18, 22, 31), FeedEventType.JOIN, "Steve", null, "Steve joined the game"));
        assertThat(parse("[18:30:00] [Server thread/INFO]: Alex left the game"))
                .hasValueSatisfying(line -> assertThat(line.type()).isEqualTo(FeedEventType.LEAVE));
        assertThat(parse("[18:40:00] [Server thread/INFO]: Steve has made the advancement [Diamonds!]"))
                .hasValueSatisfying(line -> {
                    assertThat(line.type()).isEqualTo(FeedEventType.ADVANCEMENT);
                    assertThat(line.detail()).isEqualTo("Diamonds!");
                });
        assertThat(parse("[18:41:00] [Server thread/INFO]: Alex has completed the challenge [Monsters Hunted]"))
                .hasValueSatisfying(line -> assertThat(line.detail()).isEqualTo("Monsters Hunted"));
    }

    @Test
    void readsDeathsOfKnownPlayersOnly() {
        assertThat(parse("[19:00:00] [Server thread/INFO]: Steve was slain by Zombie"))
                .hasValueSatisfying(line -> {
                    assertThat(line.type()).isEqualTo(FeedEventType.DEATH);
                    assertThat(line.message()).isEqualTo("Steve was slain by Zombie");
                });
        assertThat(parse("[19:00:01] [Server thread/INFO]: Alex tried to swim in lava")).isPresent();
        assertThat(parse("[19:00:02] [Server thread/INFO]: Villager was slain by Zombie")).isEmpty();
    }

    @Test
    void readsServerStartAndStop() {
        assertThat(parse("[10:00:00] [Server thread/INFO]: Done (4.123s)! For help, type \"help\""))
                .hasValueSatisfying(line -> assertThat(line.type()).isEqualTo(FeedEventType.SERVER_START));
        assertThat(parse("[23:00:00] [Server thread/INFO]: Stopping the server"))
                .hasValueSatisfying(line -> assertThat(line.type()).isEqualTo(FeedEventType.SERVER_STOP));
    }

    @Test
    void ignoresChatAndOtherThreads() {
        assertThat(parse("[19:00:00] [Server thread/INFO]: <Steve> was slain by my own cat")).isEmpty();
        assertThat(parse("[19:00:00] [Worker-Main-1/INFO]: Steve joined the game")).isEmpty();
        assertThat(parse("garbage")).isEmpty();
    }

    private Optional<LogLine> parse(String line) {
        return LogLineParser.parse(line, PLAYERS::contains);
    }
}
