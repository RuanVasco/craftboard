import { ChevronLeft, ChevronRight, Search, X } from 'lucide-react';
import type { ReactNode } from 'react';
import type { PlayerStats, RankingEntry, RankingPage, RankingSort } from '../api';
import { compactFormat, formatDistance, formatPlayTime, numberFormat } from '../format';
import { Avatar } from './Avatar';

interface Metric {
  key: RankingSort;
  label: string;
  value: (entry: RankingEntry) => number;
  format: (value: number) => string;
}

const METRICS: Metric[] = [
  { key: 'playTime', label: 'Tempo jogado', value: (e) => e.player.playTimeTicks, format: formatPlayTime },
  { key: 'deaths', label: 'Mortes', value: (e) => e.player.deaths, format: (v) => numberFormat.format(v) },
  { key: 'mobKills', label: 'Abates', value: (e) => e.player.mobKills, format: (v) => numberFormat.format(v) },
  { key: 'playerKills', label: 'PvP', value: (e) => e.player.playerKills, format: (v) => numberFormat.format(v) },
  { key: 'blocksMined', label: 'Blocos', value: (e) => e.player.blocksMined, format: (v) => compactFormat.format(v) },
  { key: 'distance', label: 'Distância', value: (e) => e.player.distanceCm, format: formatDistance },
  { key: 'advancements', label: 'Conquistas', value: (e) => e.advancements, format: (v) => numberFormat.format(v) },
];

interface Props {
  ranking: RankingPage | null;
  sort: RankingSort;
  onSortChange: (sort: RankingSort) => void;
  query: string;
  onQueryChange: (query: string) => void;
  onPageChange: (page: number) => void;
  // Advancement counts are totals, so they only make sense next to totals.
  showAdvancements: boolean;
  onlinePlayers: string[] | null;
  emptyMessage: string;
  onSelect: (player: PlayerStats) => void;
}

export function PlayerRanking({
  ranking,
  sort,
  onSortChange,
  query,
  onQueryChange,
  onPageChange,
  showAdvancements,
  onlinePlayers,
  emptyMessage,
  onSelect,
}: Props) {
  const metrics = showAdvancements ? METRICS : METRICS.filter((m) => m.key !== 'advancements');
  const active = metrics.find((m) => m.key === sort) ?? metrics[0];
  const others = metrics.filter((m) => m.key !== active.key);
  const items = ranking?.items ?? [];
  const max = Math.max(1, ranking?.maxValue ?? 0);
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
                onClick={() => onSortChange(metric.key)}
                className={`rounded-full border px-3 py-1 text-xs font-medium transition sm:text-sm ${
                  checked ? 'border-primary bg-primary text-surface' : 'border-line bg-surface text-ink-muted hover:text-ink'
                }`}
              >
                {metric.label}
              </button>
            );
          })}
        </div>
        <SearchBox value={query} onChange={onQueryChange} />
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
              {others.map((m) => (
                <th key={m.key} className="hidden px-3 py-3 text-right font-medium lg:table-cell">
                  {m.label}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {items.map((entry) => (
              <tr key={entry.player.uuid} className="border-b border-line last:border-0">
                <td className="px-3 py-3 tabular-nums text-ink-muted sm:px-4">{entry.rank}</td>
                <td className="px-2 py-3">
                  <PlayerName
                    name={entry.player.name}
                    online={online.has(entry.player.name)}
                    onClick={() => onSelect(entry.player)}
                  />
                </td>
                <td className="px-3 py-3 sm:px-4">
                  <BarCell value={active.format(active.value(entry))} ratio={active.value(entry) / max} />
                </td>
                {others.map((m) => (
                  <td key={m.key} className="hidden px-3 py-3 text-right tabular-nums text-ink-muted lg:table-cell">
                    {m.format(m.value(entry))}
                  </td>
                ))}
              </tr>
            ))}
            {ranking && items.length === 0 && (
              <tr>
                <td colSpan={3 + others.length} className="px-4 py-10 text-center text-ink-muted">
                  {ranking.totals.players === 0 ? emptyMessage : 'Nenhum jogador encontrado.'}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
      {ranking && ranking.totalPages > 1 && <Pagination ranking={ranking} onPageChange={onPageChange} />}
    </section>
  );
}

function Pagination({ ranking, onPageChange }: { ranking: RankingPage; onPageChange: (page: number) => void }) {
  const { page, size, totalItems, totalPages } = ranking;
  const first = page * size + 1;
  const last = Math.min((page + 1) * size, totalItems);

  return (
    <nav aria-label="Páginas do ranking" className="flex items-center justify-between gap-3 text-sm text-ink-muted">
      <span className="tabular-nums">
        {numberFormat.format(first)} a {numberFormat.format(last)} de {numberFormat.format(totalItems)}
      </span>
      <div className="flex items-center gap-1">
        <PageButton label="Página anterior" disabled={page === 0} onClick={() => onPageChange(page - 1)}>
          <ChevronLeft className="size-4" aria-hidden />
        </PageButton>
        <span className="px-2 tabular-nums">
          {page + 1} de {totalPages}
        </span>
        <PageButton label="Próxima página" disabled={page >= totalPages - 1} onClick={() => onPageChange(page + 1)}>
          <ChevronRight className="size-4" aria-hidden />
        </PageButton>
      </div>
    </nav>
  );
}

interface PageButtonProps {
  label: string;
  disabled: boolean;
  onClick: () => void;
  children: ReactNode;
}

function PageButton({ label, disabled, onClick, children }: PageButtonProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      aria-label={label}
      className="rounded-lg border border-line bg-surface p-1.5 transition hover:text-ink disabled:opacity-40 disabled:hover:text-ink-muted"
    >
      {children}
    </button>
  );
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
