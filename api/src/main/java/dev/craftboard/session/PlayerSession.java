package dev.craftboard.session;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "player_session")
@Getter
@NoArgsConstructor
public class PlayerSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID playerUuid;

    private Instant joinedAt;

    @Setter
    private Instant leftAt;

    public PlayerSession(UUID playerUuid, Instant joinedAt) {
        this.playerUuid = playerUuid;
        this.joinedAt = joinedAt;
    }
}
