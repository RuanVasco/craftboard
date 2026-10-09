import { Search, X } from 'lucide-react';
import { useMemo, useState } from 'react';
import type { PlayerStats } from '../api';
import { compactFormat, formatDistance, formatPlayTime, numberFormat } from '../format';
import { Avatar } from './Avatar';

type MetricKey = 'playTime' | 'deaths' | 'mobKills' | 'playerKills' | 'blocksMined' | 'distance' | 'advancements';

interface Metric {
  key: MetricKey;
  label: string;
  value: (p: PlayerStats, advancements: Record<string, number>) => number;
  format: (value: number) => string;
}

const METRICS: Metric[] = [
  { key: 'playTime', label: 'Tempo jogado', value: (p) => p.playTimeTicks, format: formatPlayTime },
  { key: 'deaths', label: 'Mortes', value: (p) => p.deaths, format: (v) => numberFormat.format(v) },
  { key: 'mobKills', label: 'Abates', value: (p) => p.mobKills, format: (v) => numberFormat.format(v) },
  { key: 'playerKills', label: 'PvP', value: (p) => p.playerKills, format: (v) => numberFormat.format(v) },
  { key: 'blocksMined', label: 'Blocos', value: (p) => p.blocksMined, format: (v) => compactFormat.format(v) },
  { key: 'distance', label: 'Distância', value: (p) => p.distanceCm, format: formatDistance },
  { key: 'advancements', label: 'Conquistas', value: (p, a) => a[p.uuid] ?? 0, format: (v) => numberFormat.format(v) },
];

interface Props {
  players: PlayerStats[];
  advancements: Record<string, number>;
  // Advancement counts are totals, so they only make sense next to totals.
  showAdvancements: boolean;
  onlinePlayers: string[] | null;
  emptyMessage: string;
  onSelect: (player: PlayerStats) => void;
}

export function PlayerRanking({ players, advancements, showAdvancements, onlinePlayers, emptyMessage, onSelect }: Props) {
  const [selectedKey, setSelectedKey] = useState<MetricKey>('playTime');
  const [query, setQuery] = useState('');
  const metrics = showAdvancements ? METRICS : METRICS.filter((m) => m.key !== 'advancements');
  const active = metrics.find((m) => m.key === selectedKey) ?? metrics[0];

  const sorted = useMemo(
    () =>
      [...players].sort(
        (a, b) => active.value(b, advancements) - active.value(a, advancements) || a.name.localeCompare(b.name),
      ),
    [players, advancements, active],
  );
  // Ranks come from the full list, so a filtered row still shows the player's real position.
  const visible = useMemo(() => {
    const ranked = sorted.map((player, index) => ({ player, rank: index + 1 }));
    const needle = normalize(query);
    return needle ? ranked.filter(({ player }) => matches(player, needle)) : ranked;
  }, [sorted, query]);
  const max = Math.max(1, ...players.map((p) => active.value(p, advancements)));
  const online = new Set(onlinePlayers ?? []);

  return (
    <section className="flex flex-col gap-2">
      <div className="flex flex-col-reverse gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div role="radiogroup" aria-label="Ordenar ranking por" className="flex flex-wrap gap-1.5">
          {metrics.map((metric) => {
            const checked = metric.key === active.key;
            return (
              <button
                key={metric.key}
                type="button"
                role="radio"
                aria-checked={checked}
                onClick={() => setSelectedKey(metric.key)}
                className={`rounded-full border px-3 py-1 text-xs font-medium transition sm:text-sm ${
                  checked ? 'border-primary bg-primary text-surface' : 'border-line bg-surface text-ink-muted hover:text-ink'
                }`}
              >
                {metric.label}
              </button>
            );
          })}
        </div>
        <SearchBox value={query} onChange={setQuery} />
      </div>
      <div className="overflow-x-auto rounded-xl border border-line bg-surface">
        <table className="w-full text-sm">
          <thead className="border-b border-line text-left text-ink-muted">
            <tr>
              <th className="w-10 px-3 py-3 font-medium sm:px-4">#</th>
              <th className="px-2 py-3 font-medium">Jogador</th>
              <th className="px-3 py-3 font-medium text-ink sm:px-4" aria-sort="descending">
                {active.label}
              </th>
              {metrics
                .filter((m) => m.key !== active.key)
                .map((m) => (
                  <th key={m.key} className="hidden px-3 py-3 text-right font-medium lg:table-cell">
                    {m.label}
                  </th>
                ))}
            </tr>
          </thead>
          <tbody>
            {visible.map(({ player, rank }) => (
              <tr key={player.uuid} className="border-b border-line last:border-0">
                <td className="px-3 py-3 tabular-nums text-ink-muted sm:px-4">{rank}</td>
                <td className="px-2 py-3">
                  <PlayerName name={player.name} online={online.has(player.name)} onClick={() => onSelect(player)} />
                </td>
                <td className="px-3 py-3 sm:px-4">
                  <BarCell
                    value={active.format(active.value(player, advancements))}
                    ratio={active.value(player, advancements) / max}
                  />
                </td>
                {metrics
                  .filter((m) => m.key !== active.key)
                  .map((m) => (
                    <td key={m.key} className="hidden px-3 py-3 text-right tabular-nums text-ink-muted lg:table-cell">
                      {m.format(m.value(player, advancements))}
                    </td>
                  ))}
              </tr>
            ))}
            {visible.length === 0 && (
              <tr>
                <td colSpan={3 + metrics.length} className="px-4 py-10 text-center text-ink-muted">
                  {sorted.length === 0 ? emptyMessage : 'Nenhum jogador encontrado.'}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}

// Dashes are dropped so a UUID matches whether it is pasted with or without them.
function normalize(text: string): string {
  return text.trim().toLowerCase().replaceAll('-', '');
}

function matches(player: PlayerStats, needle: string): boolean {
  return normalize(player.name).includes(needle) || normalize(player.uuid).includes(needle);
}

function SearchBox({ value, onChange }: { value: string; onChange: (value: string) => void }) {
  return (
    <label className="relative flex items-center sm:w-64">
      <Search className="pointer-events-none absolute left-3 size-4 text-ink-muted" aria-hidden />
      <input
        type="search"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder="Buscar por nick ou UUID"
        aria-label="Buscar jogador por nick ou UUID"
        className="w-full rounded-lg border border-line bg-surface py-1.5 pr-8 pl-9 text-sm placeholder:text-ink-muted focus:border-primary focus:outline-none [&::-webkit-search-cancel-button]:hidden"
      />
      {value && (
        <button
          type="button"
          onClick={() => onChange('')}
          className="absolute right-2 rounded p-0.5 text-ink-muted hover:text-ink"
          aria-label="Limpar busca"
        >
          <X className="size-4" aria-hidden />
        </button>
      )}
    </label>
  );
}

function PlayerName({ name, online, onClick }: { name: string; online: boolean; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="flex max-w-full items-center gap-2.5 text-left hover:text-primary"
      title={`Ver perfil de ${name}`}
    >
      <Avatar name={name} />
      <span className="truncate font-medium">{name}</span>
      {online && <span className="rounded-full bg-good/15 px-2 py-0.5 text-xs font-medium text-good">online</span>}
    </button>
  );
}

function BarCell({ value, ratio }: { value: string; ratio: number }) {
  return (
    <div className="flex items-center gap-3 sm:min-w-36">
      <span className="shrink-0 tabular-nums sm:w-20">{value}</span>
      <div className="hidden h-2 flex-1 rounded bg-primary-soft sm:block">
        <div className="h-2 rounded bg-primary" style={{ width: `${Math.max(ratio * 100, ratio > 0 ? 2 : 0)}%` }} />
      </div>
    </div>
  );
}
