import {
  CalendarDays,
  Check,
  CloudLightning,
  CloudRain,
  Copy,
  Gamepad2,
  Moon,
  Radio,
  Server,
  Skull,
  Sun,
  Swords,
  Users,
  type LucideIcon,
} from 'lucide-react';
import { useEffect, useState } from 'react';
import type { ServerInfo, WorldInfo } from '../api';
import { formatTimeOfDay, isNight, numberFormat } from '../format';
import { DIFFICULTIES, GAME_MODES } from '../names';

const TICKS_PER_DAY = 24000;
const MS_PER_TICK = 50;

interface Props {
  server: ServerInfo | null;
  live: boolean;
}

export function ServerBanner({ server, live }: Props) {
  const [copied, setCopied] = useState(false);

  const copyAddress = async () => {
    if (!server) return;
    try {
      await navigator.clipboard.writeText(server.address);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      // Clipboard needs https or localhost; the address is still visible to copy by hand.
    }
  };

  return (
    <header className="bg-banner text-banner-ink">
      <div className="mx-auto flex max-w-5xl flex-wrap items-center justify-between gap-4 px-4 py-5">
        <div className="flex min-w-0 items-center gap-3">
          <Server className="size-6 shrink-0 opacity-80" aria-hidden />
          <div className="min-w-0">
            <p className="truncate text-lg font-semibold">{server?.motd ?? 'Carregando...'}</p>
            <ServerStatus server={server} />
          </div>
        </div>

        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={copyAddress}
            disabled={!server}
            className="flex items-center gap-2 rounded-lg bg-white/10 px-3 py-2 font-mono text-sm transition hover:bg-white/20"
            aria-label="Copiar endereço do servidor"
          >
            <span>{server?.address ?? '...'}</span>
            {copied ? <Check className="size-4" aria-hidden /> : <Copy className="size-4" aria-hidden />}
          </button>
          <span
            className="flex items-center gap-1.5 text-xs opacity-80"
            title={live ? 'Atualizando em tempo real' : 'Conectando ao servidor'}
          >
            <Radio className={`size-4 ${live ? '' : 'opacity-40'}`} aria-hidden />
            {live ? 'ao vivo' : 'conectando'}
          </span>
        </div>
      </div>
      {server && <WorldStrip server={server} />}
    </header>
  );
}

function WorldStrip({ server }: { server: ServerInfo }) {
  const world = server.world;
  // The clock only advances while someone keeps the server ticking (it pauses when empty).
  const ticking = (server.onlinePlayers?.length ?? 0) > 0;
  const timeOfDay = useWorldClock(world, ticking);

  return (
    <div className="border-t border-white/10">
      <ul className="mx-auto flex max-w-5xl flex-wrap items-center gap-x-5 gap-y-1.5 px-4 py-2.5 text-xs opacity-90 sm:text-sm">
        {world?.version && <Fact icon={Server} label={`Versão ${world.version}`} />}
        {world?.day != null && <Fact icon={CalendarDays} label={`Dia ${numberFormat.format(world.day)}`} />}
        {timeOfDay != null && (
          <Fact
            icon={isNight(timeOfDay) ? Moon : Sun}
            label={`${formatTimeOfDay(timeOfDay)} no jogo`}
            title={ticking ? 'Horário estimado desde o último save' : 'Horário do último save'}
          />
        )}
        {world && <Fact icon={world.thundering ? CloudLightning : world.raining ? CloudRain : Sun} label={weather(world)} />}
        <Fact icon={Gamepad2} label={`${GAME_MODES[server.gameMode] ?? server.gameMode}, ${DIFFICULTIES[server.difficulty] ?? server.difficulty}`} />
        <Fact icon={Swords} label={server.pvp ? 'PvP ligado' : 'PvP desligado'} />
        {server.hardcore && <Fact icon={Skull} label="Hardcore" />}
      </ul>
    </div>
  );
}

function useWorldClock(world: WorldInfo | null, ticking: boolean): number | null {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    if (!ticking) return;
    const timer = setInterval(() => setNow(Date.now()), 10_000);
    return () => clearInterval(timer);
  }, [ticking]);

  if (world?.timeOfDay == null) return null;
  if (!ticking) return world.timeOfDay;
  const elapsedTicks = Math.max(0, (now - Date.parse(world.capturedAt)) / MS_PER_TICK);
  return Math.floor(world.timeOfDay + elapsedTicks) % TICKS_PER_DAY;
}

function weather(world: WorldInfo): string {
  if (world.thundering) return 'Tempestade';
  return world.raining ? 'Chovendo' : 'Céu limpo';
}

function Fact({ icon: Icon, label, title }: { icon: LucideIcon; label: string; title?: string }) {
  return (
    <li className="flex items-center gap-1.5" title={title}>
      <Icon className="size-3.5 opacity-80" aria-hidden />
      {label}
    </li>
  );
}

function ServerStatus({ server }: { server: ServerInfo | null }) {
  if (!server || server.online === null) {
    return <p className="text-sm opacity-70">Minecraft vanilla</p>;
  }
  if (!server.online) {
    return <p className="text-sm opacity-70">Servidor offline</p>;
  }
  const players = server.onlinePlayers ?? [];
  return (
    <p className="flex items-center gap-1.5 text-sm opacity-90" title={players.join(', ')}>
      <Users className="size-4" aria-hidden />
      {players.length} de {server.maxPlayers} online
    </p>
  );
}
