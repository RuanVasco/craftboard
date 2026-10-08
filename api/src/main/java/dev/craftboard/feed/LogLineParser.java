package dev.craftboard.feed;

import java.time.LocalTime;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Picks feed events out of vanilla server log lines. Chat is ignored on purpose: the dashboard is
 * public and players do not expect their messages to end up on it.
 */
public final class LogLineParser {

    // [18:22:31] [Server thread/INFO]: Steve joined the game
    private static final Pattern LINE = Pattern.compile("^\\[(\\d{2}:\\d{2}:\\d{2})] \\[Server thread/INFO][^:]*: (.+)$");
    private static final Pattern JOIN = Pattern.compile("^(\\S+) joined the game$");
    private static final Pattern LEAVE = Pattern.compile("^(\\S+) left the game$");
    private static final Pattern ADVANCEMENT =
            Pattern.compile("^(\\S+) has (?:made the advancement|completed the challenge|reached the goal) \\[(.+)]$");
    // How vanilla death messages continue after the victim's name. Only checked once the first
    // word is a known player, which keeps other lines from passing as deaths.
    private static final Pattern DEATH = Pattern.compile("^(\\S+) (?:was |were |drowned|died|experienced kinetic"
            + "|blew up|hit the ground|fell |went up in flames|went off with|walked into|burned to death"
            + "|tried to swim|discovered the floor|froze to death|starved|suffocated|withered away"
            + "|didn't want to live|left the confines).*$");

    private LogLineParser() {
    }

    public static Optional<LogLine> parse(String line, Predicate<String> isPlayer) {
        var matcher = LINE.matcher(line.stripTrailing());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        var time = LocalTime.parse(matcher.group(1));
        var message = matcher.group(2);

        if (message.startsWith("Done (")) {
            return Optional.of(new LogLine(time, FeedEventType.SERVER_START, null, null, message));
        }
        if (message.equals("Stopping the server") || message.equals("Stopping server")) {
            return Optional.of(new LogLine(time, FeedEventType.SERVER_STOP, null, null, message));
        }
        var m = JOIN.matcher(message);
        if (m.matches()) {
            return Optional.of(new LogLine(time, FeedEventType.JOIN, m.group(1), null, message));
        }
        m = LEAVE.matcher(message);
        if (m.matches()) {
            return Optional.of(new LogLine(time, FeedEventType.LEAVE, m.group(1), null, message));
        }
        m = ADVANCEMENT.matcher(message);
        if (m.matches()) {
            return Optional.of(new LogLine(time, FeedEventType.ADVANCEMENT, m.group(1), m.group(2), message));
        }
        m = DEATH.matcher(message);
        if (m.matches() && isPlayer.test(m.group(1))) {
            return Optional.of(new LogLine(time, FeedEventType.DEATH, m.group(1), null, message));
        }
        return Optional.empty();
    }
}
