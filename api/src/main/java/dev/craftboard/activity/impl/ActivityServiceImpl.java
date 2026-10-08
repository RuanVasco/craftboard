package dev.craftboard.activity.impl;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import org.springframework.stereotype.Service;

import dev.craftboard.activity.Activity;
import dev.craftboard.activity.ActivityService;
import dev.craftboard.activity.PlayTimeGain;
import dev.craftboard.activity.PlayTimeGainRepository;
import dev.craftboard.activity.PlayerSummary;
import dev.craftboard.config.MinecraftProperties;
import dev.craftboard.player.PlayerRepository;
import dev.craftboard.session.PlayerSession;
import dev.craftboard.session.PlayerSessionRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private static final double TICKS_PER_HOUR = 20 * 60 * 60;

    private final MinecraftProperties properties;
    private final PlayTimeGainRepository gains;
    private final PlayerRepository players;
    private final PlayerSessionRepository sessions;

    @Override
    public Activity server(int days) {
        return activity(days, gain -> true);
    }

    @Override
    public Activity player(UUID uuid, int days) {
        return activity(days, gain -> gain.getPlayerUuid().equals(uuid));
    }

    @Override
    public Optional<PlayerSummary> summary(UUID uuid) {
        return players.findById(uuid).map(player -> {
            var playerSessions = sessions.findByPlayerUuid(uuid);
            if (playerSessions.isEmpty()) {
                return new PlayerSummary(player.getFirstSeen(), player.getLastSeen(), null, null, null);
            }
            var now = Instant.now();
            var lengths = playerSessions.stream()
                    .mapToLong(s -> Duration.between(s.getJoinedAt(), endOf(s, now)).toSeconds())
                    .toArray();
            long total = 0;
            long longest = 0;
            for (var length : lengths) {
                total += length;
                longest = Math.max(longest, length);
            }
            return new PlayerSummary(player.getFirstSeen(), player.getLastSeen(), lengths.length, longest,
                    total / lengths.length);
        });
    }

    private Activity activity(int days, Predicate<PlayTimeGain> filter) {
        var zone = properties.timezone();
        var today = LocalDate.now(zone);
        var firstDay = today.minusDays(days - 1L);
        var from = firstDay.atStartOfDay(zone).toInstant();

        var heatmap = new double[7][24];
        Map<LocalDate, Double> hoursByDay = new HashMap<>();
        Map<LocalDate, Set<UUID>> playersByDay = new HashMap<>();
        for (var gain : gains.findSince(from)) {
            if (!filter.test(gain)) {
                continue;
            }
            var at = ZonedDateTime.ofInstant(Instant.ofEpochMilli((long) (gain.getCapturedAtEpoch() * 1000)), zone);
            double hours = gain.getTicks() / TICKS_PER_HOUR;
            heatmap[at.getDayOfWeek().getValue() - 1][at.getHour()] += hours;
            hoursByDay.merge(at.toLocalDate(), hours, Double::sum);
            playersByDay.computeIfAbsent(at.toLocalDate(), d -> new HashSet<>()).add(gain.getPlayerUuid());
        }

        List<Activity.Day> daily = new ArrayList<>(days);
        for (var date = firstDay; !date.isAfter(today); date = date.plusDays(1)) {
            daily.add(new Activity.Day(date, hoursByDay.getOrDefault(date, 0.0),
                    playersByDay.getOrDefault(date, Set.of()).size()));
        }
        return new Activity(days, heatmap, daily);
    }

    private static Instant endOf(PlayerSession session, Instant now) {
        return session.getLeftAt() != null ? session.getLeftAt() : now;
    }
}
