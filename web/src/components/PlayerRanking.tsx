import { ArrowDown, User } from 'lucide-react';
import { useMemo, useState } from 'react';
import type { PlayerStats } from '../api';
import { formatPlayTime, numberFormat } from '../format';

type SortKey = 'playTimeTicks' | 'deaths';

interface Props {
  players: PlayerStats[];
  onlinePlayers: string[] | null;
}

export function PlayerRanking({ players, onlinePlayers }: Props) {
  const [sortKey, setSortKey] = useState<SortKey>('playTimeTicks');

  const sorted = useMemo(
    () => [...players].sort((a, b) => b[sortKey] - a[sortKey] || a.name.localeCompare(b.name)),
    [players, sortKey],
  );
  const maxPlayTime = Math.max(1, ...players.map((p) => p.playTimeTicks));
  const maxDeaths = Math.max(1, ...players.map((p) => p.deaths));
  const online = new Set(onlinePlayers ?? []);

  return (
    <section className="overflow-x-auto rounded-xl border border-line bg-surface">
      <table className="w-full text-sm">
        <thead className="border-b border-line text-left text-ink-muted">
          <tr>
            <th className="w-10 px-3 py-3 font-medium sm:px-4">#</th>
            <th className="px-2 py-3 font-medium">Jogador</th>
            <SortHeader label="Tempo jogado" active={sortKey === 'playTimeTicks'} onClick={() => setSortKey('playTimeTicks')} />
            <SortHeader label="Mortes" active={sortKey === 'deaths'} onClick={() => setSortKey('deaths')} />
          </tr>
        </thead>
        <tbody>
          {sorted.map((player, index) => (
            <tr key={player.uuid} className="border-b border-line last:border-0">
              <td className="px-3 py-3 tabular-nums text-ink-muted sm:px-4">{index + 1}</td>
              <td className="px-2 py-3">
                <PlayerName name={player.name} online={online.has(player.name)} />
              </td>
              <td className="px-3 py-3 sm:px-4">
                <BarCell value={formatPlayTime(player.playTimeTicks)} ratio={player.playTimeTicks / maxPlayTime} />
              </td>
              <td className="px-3 py-3 sm:px-4">
                <BarCell value={numberFormat.format(player.deaths)} ratio={player.deaths / maxDeaths} />
              </td>
            </tr>
          ))}
          {sorted.length === 0 && (
            <tr>
              <td colSpan={4} className="px-4 py-10 text-center text-ink-muted">
                Nenhum jogador ainda.
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </section>
  );
}

function SortHeader({ label, active, onClick }: { label: string; active: boolean; onClick: () => void }) {
  return (
    <th className="px-3 py-3 font-medium sm:px-4" aria-sort={active ? 'descending' : 'none'}>
      <button
        type="button"
        onClick={onClick}
        className={`flex items-center gap-1 hover:text-ink ${active ? 'text-ink' : ''}`}
      >
        {label}
        <ArrowDown className={`size-3.5 ${active ? '' : 'invisible'}`} aria-hidden />
      </button>
    </th>
  );
}

function PlayerName({ name, online }: { name: string; online: boolean }) {
  const [avatarFailed, setAvatarFailed] = useState(false);
  return (
    <span className="flex items-center gap-2.5">
      {avatarFailed ? (
        <User className="size-7 rounded bg-primary-soft p-1 text-ink-muted" aria-hidden />
      ) : (
        <img
          src={`https://mc-heads.net/avatar/${encodeURIComponent(name)}/28`}
          alt=""
          width={28}
          height={28}
          className="rounded"
          onError={() => setAvatarFailed(true)}
        />
      )}
      <span className="truncate font-medium">{name}</span>
      {online && (
        <span className="rounded-full bg-good/15 px-2 py-0.5 text-xs font-medium text-good">online</span>
      )}
    </span>
  );
}

function BarCell({ value, ratio }: { value: string; ratio: number }) {
  return (
    <div className="flex items-center gap-3 sm:min-w-28">
      <span className="shrink-0 tabular-nums sm:w-16">{value}</span>
      <div className="hidden h-2 flex-1 rounded bg-primary-soft sm:block">
        <div className="h-2 rounded bg-primary" style={{ width: `${Math.max(ratio * 100, ratio > 0 ? 2 : 0)}%` }} />
      </div>
    </div>
  );
}
