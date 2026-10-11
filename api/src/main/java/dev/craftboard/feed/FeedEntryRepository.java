package dev.craftboard.feed;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedEntryRepository extends JpaRepository<FeedEntry, Long> {

    boolean existsByOccurredAtAndMessage(Instant occurredAt, String message);

    List<FeedEntry> findTop100ByOrderByOccurredAtDescIdDesc();
}
