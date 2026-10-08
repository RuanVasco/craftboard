# Spec 001: Histórico de estatísticas em banco

Status: **Draft** (só especificação, sem implementação)

## 1. Problema

Os arquivos do servidor (`world/stats/<uuid>.json`) guardam apenas o **valor acumulado atual** de cada estatística. Hoje a API mantém esses valores em memória e os reconstrói a partir dos arquivos no startup (`StatsServiceImpl.reloadAll`), o que basta para mostrar o estado atual, mas impede qualquer visão temporal:

- não dá para saber quanto alguém jogou nesta semana ou neste mês;
- não dá para desenhar evolução de horas jogadas ou de mortes;
- a informação de quem estava online em cada momento (via RCON) se perde a cada poll.

## 2. Objetivo

Persistir snapshots das estatísticas para permitir:

1. **Gráfico de evolução** por jogador (horas jogadas e mortes ao longo do tempo).
2. **Rankings por período** (semana, mês), calculados pela diferença entre snapshots.
3. **Sessões** (entrada e saída) quando o RCON estiver habilitado: último acesso, horário de pico.

O estado atual continua vindo dos arquivos; o banco é só histórico. Se o banco estiver vazio, o dashboard atual funciona igual.

## 3. Decisões

### 3.1 Banco: PostgreSQL + Flyway

Mesmo stack do formulei. Toda alteração de schema tem migration em `api/src/main/resources/db/migration/` (`V{N}__descricao.sql`), `ddl-auto=validate` e migrations aplicadas são imutáveis.

### 3.2 Banco próprio, não compartilhado com o formulei

Container `postgres` no compose do craftboard, com volume próprio. Evita acoplar o ciclo de vida (backup, upgrade, downtime) dos dois projetos.

### 3.3 Compose de dev e prod em arquivos separados

Com o Postgres, o dev passa a precisar de Docker para o banco. Mesmo padrão do formulei, um arquivo por ambiente:

- **`docker-compose.dev.yml`:** só o `postgres`, com porta exposta em `localhost:5432`. API e web continuam rodando fora do Docker (`mvn spring-boot:run`, `npm run dev`).
- **`docker-compose.prod.yml`** (já existe, hoje só com `craftboard` e `watchtower`): ganha o `postgres` sem porta exposta no host, tag de imagem pinada (sem `latest`) e fora do watchtower. Upgrade de banco é manual e com backup.

Uso:

- Dev: `docker compose -f docker-compose.dev.yml up -d`
- Prod: `docker compose -f docker-compose.prod.yml up -d`

### 3.4 Quando gravar snapshot

A cada `StatsUpdatedEvent` (já disparado só quando o valor muda). Como o servidor só grava no autosave, logout ou `save-all`, o volume esperado é baixo: no máximo um snapshot por jogador ativo a cada intervalo de save (60 s com RCON, ~5 min sem).

### 3.5 Backfill no startup

No primeiro startup com banco vazio, gravar um snapshot inicial de cada jogador com os valores atuais, para que os deltas de período tenham ponto de partida.

## 4. Schema proposto

```sql
CREATE TABLE player (
    uuid        UUID PRIMARY KEY,
    name        VARCHAR(16) NOT NULL,
    first_seen  TIMESTAMPTZ NOT NULL,
    last_seen   TIMESTAMPTZ NOT NULL
);

CREATE TABLE stat_snapshot (
    id              BIGSERIAL PRIMARY KEY,
    player_uuid     UUID NOT NULL REFERENCES player (uuid),
    deaths          BIGINT NOT NULL,
    play_time_ticks BIGINT NOT NULL,
    captured_at     TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_stat_snapshot_player_time ON stat_snapshot (player_uuid, captured_at);

-- Só com RCON habilitado.
CREATE TABLE player_session (
    id          BIGSERIAL PRIMARY KEY,
    player_uuid UUID NOT NULL REFERENCES player (uuid),
    joined_at   TIMESTAMPTZ NOT NULL,
    left_at     TIMESTAMPTZ
);
```

Sessões entram em migration própria, separada de `player` e `stat_snapshot`.

## 5. API

- `GET /api/players/{uuid}/history?from=&to=`: série de snapshots para o gráfico.
- `GET /api/players?period=week|month|all`: mesmo payload de hoje, com valores do período (delta entre o último snapshot antes de `from` e o atual). `all` é o comportamento atual.

## 6. Front

- Filtro de período acima do ranking (semana, mês, total).
- Página ou painel do jogador com gráfico de linha de horas jogadas e de mortes (uma métrica por gráfico, sem eixo duplo).

## 7. Fora de escopo

- Outras estatísticas (blocos minerados, mobs mortos, distância percorrida): spec própria.
- Retenção e compactação de snapshots antigos: avaliar quando o volume justificar.

## 8. Referências de código

- `api/src/main/java/dev/craftboard/stats/impl/StatsServiceImpl.java`: estado em memória e disparo do `StatsUpdatedEvent`.
- `api/src/main/java/dev/craftboard/server/impl/ServerServiceImpl.java`: poll do RCON (`list`), base para as sessões.
- `docker-compose.prod.yml`: compose de produção atual.
