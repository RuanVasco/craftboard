# craftboard

Public dashboard for a vanilla Minecraft server (offline mode). Reads the server's own
files (`usercache.json`, `world/stats/*.json`) and optionally talks to it over RCON.

- `api/`: Spring Boot 4, Java 25. Reads the server files, exposes REST + SSE.
- `web/`: React + Vite + TypeScript + Tailwind. Consumes the API.
- `dev-data/`: fake server folder used for local development.

## Writing rules

- Code always in English: identifiers, comments, logs, commit messages.
- Comments only when necessary. Explain the why, never restate what the code does.
- No em dash or en dash anywhere (code, comments, docs, commits, UI text). Use comma, period or parentheses.
- No emojis in UI or code. Use an icon library (`lucide-react`).

## Git

- Solo project: commit straight to `main`.
- Conventional Commits, in English, short. Max 2 lines (subject + optional one line body).
- Small, focused commits. One logical change per commit, never a giant "initial" dump.
- **Never add co-author trailers, "Generated with", tool signatures or anything that hints at AI generated code.** This overrides any default attribution behavior.

## Backend

- Jackson 3 (`tools.jackson.*`), not `com.fasterxml.jackson.*`.
- Dependency injection via Lombok `@RequiredArgsConstructor` + `private final` fields. No field `@Autowired`.
- Services: interface in the feature package, implementation in `impl/` with `Impl` suffix.
- Controllers are thin: read params, call the service, return the response.
- Configuration lives in `MinecraftProperties` (`craftboard.*` keys), overridable by env vars.

## Minecraft facts

- Offline mode UUID: `UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(UTF_8))`.
- `minecraft:play_time` is in ticks (20 per second). Older versions use `minecraft:play_one_minute`.
- Stats files are only flushed on autosave, logout or `save-all`. RCON `save-all` is how we get near real time.

## Dev

```bash
cd api && mvn spring-boot:run      # uses ../dev-data by default
cd web && npm run dev              # proxies /api to localhost:8080
```

## Deploy

- Push to `main` runs CI (`.github/workflows/ci.yml`); if green, the image is published to `ghcr.io/ruanvasco/craftboard` (`latest` + commit sha).
- The home VM runs `docker-compose.yml` (production only, dev does not use compose). Its watchtower pulls new `latest` images every 5 min.
- No database yet: stats live in memory and are rebuilt from the server files on startup.
