package dev.craftboard.feed;

import java.time.Instant;

import dev.craftboard.player.OfflineUuid;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "feed_event")
@Getter
@NoArgsConstructor
public class FeedEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private FeedEventType type;

    private String player;

    private String detail;

    private String message;

    private Instant occurredAt;

    public FeedEntry(FeedEvent event) {
        this.type = event.type();
        this.player = event.player();
        this.detail = event.detail();
        this.message = event.message();
        this.occurredAt = event.at();
    }

    public FeedEvent toEvent() {
        return new FeedEvent(type, player, player == null ? null : OfflineUuid.of(player), detail, message, occurredAt);
    }
}
