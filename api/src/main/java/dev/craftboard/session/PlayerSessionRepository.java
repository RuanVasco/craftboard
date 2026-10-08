package dev.craftboard.session;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerSessionRepository extends JpaRepository<PlayerSession, Long> {

    List<PlayerSession> findByLeftAtIsNull();
}
