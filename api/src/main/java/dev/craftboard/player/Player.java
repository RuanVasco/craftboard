package dev.craftboard.player;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "player")
@Getter
@Setter
@NoArgsConstructor
public class Player {

    @Id
    private UUID uuid;

    private String name;

    private Instant firstSeen;

    private Instant lastSeen;

    public Player(UUID uuid, String name, Instant seenAt) {
        this.uuid = uuid;
        this.name = name;
        this.firstSeen = seenAt;
        this.lastSeen = seenAt;
    }
}
