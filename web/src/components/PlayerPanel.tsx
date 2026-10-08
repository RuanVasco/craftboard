import {
  Backpack,
  CalendarDays,
  Clock,
  Drumstick,
  Globe,
  Hand,
  Heart,
  LoaderCircle,
  LogIn,
  Shirt,
  Star,
  Timer,
  X,
  type LucideIcon,
} from 'lucide-react';
import { useEffect, useState, type ReactNode } from 'react';
import {
  fetchHistory,
  fetchPlayerActivity,
  fetchPlayerAdvancements,
  fetchPlayerState,
  fetchPlayerStats,
  fetchPlayerSummary,
  type Activity,
  type Advancement,
  type ItemStack,
  type PlayerState,
  type PlayerSummary,
  type SnapshotPoint,
  type StatCategories,
} from '../api';
import {
  dateFormat,
  dateTimeFormat,
  formatDistance,
  formatPlayTime,
  formatRelative,
  formatSeconds,
  numberFormat,
  ticksToHoursPrecise,
} from '../format';
import { ADVANCEMENT_CATEGORIES, advancementTitle, DIMENSIONS, GAME_MODES, humanize, movementLabel } from '../names';
import { STAT_DEFS } from '../statDefs';
import { ActivityCharts } from './ActivityCharts';
import { Avatar } from './Avatar';
import { BarList, toItems } from './BarList';
import { LineChart, type ChartPoint } from './LineChart';

export interface SelectedPlayer {
  uuid: string;
  name: string;
}

interface Props {
  player: SelectedPlayer;
  online: boolean;
  statsVersion: number;
  onClose: () => void;
}

interface PanelData {
  history: SnapshotPoint[];
  stats: StatCategories | null;
  state: PlayerState | null;
  summary: PlayerSummary | null;
  advancements: Advancement[];
  activity: Activity;
}

type Tab = 'overview' | 'stats' | 'advancements' | 'history';

const TABS: { value: Tab; label: string }[] = [
  { value: 'overview', label: 'Visão geral' },
  { value: 'stats', label: 'Estatísticas' },
  { value: 'advancements', label: 'Conquistas' },
  { value: 'history', label: 'Histórico' },
];

export function PlayerPanel({ player, online, statsVersion, onClose }: Props) {
  const [data, setData] = useState<PanelData | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [tab, setTab] = useState<Tab>('overview');

  useEffect(() => {
    setData(null);
    setError(null);
    setTab('overview');
  }, [player.uuid]);

  // Refetched on every stats event too, since a save rewrites the player files together.
  useEffect(() => {
    let active = true;
    Promise.all([
      fetchHistory(player.uuid),
      fetchPlayerStats(player.uuid),
      fetchPlayerState(player.uuid),
      fetchPlayerSummary(player.uuid),
      fetchPlayerAdvancements(player.uuid),
      fetchPlayerActivity(player.uuid),
    ])
      .then(([history, stats, state, summary, advancements, activity]) => {
        if (active) setData({ history, stats, state, summary, advancements, activity });
      })
      .catch((e: Error) => active && setError(e.message));
    return () => {
      active = false;
    };
  }, [player.uuid, statsVersion]);

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => event.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);

  return (
    <div className="fixed inset-0 z-20 flex items-end justify-center bg-black/40 sm:items-center sm:p-4" onClick={onClose}>
      <section
        role="dialog"
        aria-modal="true"
        aria-label={`Perfil de ${player.name}`}
        className="flex max-h-[92vh] w-full max-w-3xl flex-col overflow-hidden rounded-t-xl bg-surface sm:rounded-xl"
        onClick={(event) => event.stopPropagation()}
      >
        <header className="flex shrink-0 items-center gap-3 border-b border-line p-4 sm:px-6">
          <Avatar name={player.name} size={44} />
          <div className="min-w-0 flex-1">
            <h2 className="flex items-center gap-2 text-lg font-semibold">
              <span className="truncate">{player.name}</span>
              {online && <span className="rounded-full bg-good/15 px-2 py-0.5 text-xs font-medium text-good">online</span>}
            </h2>
            {data?.state && (
              <p className="text-sm text-ink-muted">
                {DIMENSIONS[data.state.dimension] ?? humanize(data.state.dimension)},{' '}
                {(GAME_MODES[data.state.gameMode] ?? data.state.gameMode).toLowerCase()}
              </p>
            )}
          </div>
          <button type="button" onClick={onClose} className="rounded-md p-1 text-ink-muted hover:text-ink" aria-label="Fechar">
            <X className="size-5" aria-hidden />
          </button>
        </header>
        <nav role="tablist" className="flex shrink-0 gap-1 overflow-x-auto overflow-y-hidden [scrollbar-width:none] border-b border-line px-4 sm:px-6">
          {TABS.map((t) => (
            <button
              key={t.value}
              type="button"
              role="tab"
              aria-selected={tab === t.value}
              onClick={() => setTab(t.value)}
              className={`-mb-px shrink-0 border-b-2 px-3 py-2.5 text-sm font-medium transition ${
                tab === t.value ? 'border-primary text-ink' : 'border-transparent text-ink-muted hover:text-ink'
              }`}
            >
              {t.label}
            </button>
          ))}
        </nav>
        <div key={tab} className="flex flex-col gap-6 overflow-y-auto p-4 sm:p-6">
          {error ? (
            <p className="text-sm text-bad">Não foi possível carregar o perfil ({error}).</p>
          ) : !data ? (
            <LoaderCircle className="mx-auto size-6 animate-spin text-ink-muted" aria-label="Carregando" />
          ) : (
            <TabBody tab={tab} data={data} />
          )}
        </div>
      </section>
    </div>
  );
}

function TabBody({ tab, data }: { tab: Tab; data: PanelData }) {
  switch (tab) {
    case 'overview':
      return <OverviewTab data={data} />;
    case 'stats':
      return <StatsTab stats={data.stats} />;
    case 'advancements':
      return <AdvancementsTab advancements={data.advancements} />;
    case 'history':
      return <HistoryTab history={data.history} />;
  }
}

function OverviewTab({ data }: { data: PanelData }) {
  const { state, summary, stats, activity } = data;
  const custom = stats?.custom ?? {};
  const playTime = custom.play_time ?? custom.play_one_minute ?? 0;

  return (
    <>
      <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
        <Fact icon={Clock} label="Tempo jogado" value={formatPlayTime(playTime)} />
        {state && <Fact icon={Star} label="Nível de XP" value={numberFormat.format(state.xpLevel)} />}
        {state && <Fact icon={Heart} label="Vida" value={`${numberFormat.format(Math.ceil(state.health) / 2)} / 10`} />}
        {state && <Fact icon={Drumstick} label="Fome" value={`${numberFormat.format(state.foodLevel / 2)} / 10`} />}
        {summary && <Fact icon={CalendarDays} label="Visto pela primeira vez" value={dateFormat.format(Date.parse(summary.firstSeen))} />}
        {summary && <Fact icon={Globe} label="Última atividade" value={formatRelative(summary.lastSeen)} />}
        {summary?.sessions != null && <Fact icon={LogIn} label="Sessões" value={numberFormat.format(summary.sessions)} />}
        {summary?.longestSessionSeconds != null && (
          <Fact icon={Timer} label="Sessão mais longa" value={formatSeconds(summary.longestSessionSeconds)} />
        )}
      </div>

      {state && <Equipment state={state} />}

      <Block title="Atividade nos últimos 30 dias">
        <ActivityCharts activity={activity} />
      </Block>
    </>
  );
}

const EQUIPMENT_SLOTS: { key: keyof PlayerState['equipment']; label: string }[] = [
  { key: 'head', label: 'Cabeça' },
  { key: 'chest', label: 'Peito' },
  { key: 'legs', label: 'Pernas' },
  { key: 'feet', label: 'Pés' },
  { key: 'offhand', label: 'Mão secundária' },
];

function Equipment({ state }: { state: PlayerState }) {
  return (
    <div className="grid gap-4 sm:grid-cols-2">
      <Block title="Equipado" icon={Shirt} note={`Último save ${formatRelative(state.updatedAt)}`}>
        <dl className="flex flex-col gap-1.5 text-sm">
          <Slot label="Na mão" icon={Hand} item={state.selectedItem} />
          {EQUIPMENT_SLOTS.map((slot) => (
            <Slot key={slot.key} label={slot.label} item={state.equipment[slot.key] ?? null} />
          ))}
        </dl>
      </Block>
      <Block title="Inventário" icon={Backpack}>
        {state.inventory.length === 0 ? (
          <p className="text-sm text-ink-muted">Vazio.</p>
        ) : (
          <ul className="flex flex-wrap gap-1.5 text-xs">
            {state.inventory.map((item) => (
              <li key={item.id} className="rounded-md border border-line px-2 py-1">
                <span className="tabular-nums text-ink-muted">{numberFormat.format(item.count)}x</span> {humanize(item.id)}
              </li>
            ))}
          </ul>
        )}
      </Block>
    </div>
  );
}

function Slot({ label, item, icon: Icon }: { label: string; item: ItemStack | null; icon?: LucideIcon }) {
  return (
    <div className="flex items-baseline justify-between gap-3">
      <dt className="flex items-center gap-1.5 text-ink-muted">
        {Icon && <Icon className="size-3.5" aria-hidden />}
        {label}
      </dt>
      <dd className={`truncate text-right ${item ? '' : 'text-ink-muted'}`}>
        {item ? `${humanize(item.id)}${item.count > 1 ? ` (${item.count})` : ''}` : 'nada'}
      </dd>
    </div>
  );
}

// Custom stats worth a tile, in this order. Others still show up in the full list below.
const HEADLINE_STATS = [
  'mob_kills',
  'player_kills',
  'deaths',
  'time_since_death',
  'jump',
  'damage_dealt',
  'damage_taken',
  'fish_caught',
  'animals_bred',
  'traded_with_villager',
  'enchant_item',
  'sleep_in_bed',
  'open_chest',
  'leave_game',
];

function StatsTab({ stats }: { stats: StatCategories | null }) {
  if (!stats) {
    return <p className="text-sm text-ink-muted">Ainda não há estatísticas para este jogador.</p>;
  }
  const custom = stats.custom ?? {};
  const mined = stats.mined ?? {};
  const distances = Object.entries(custom).filter(([key, value]) => key.endsWith('_one_cm') && value > 0);
  const totalDistance = distances.reduce((sum, [, value]) => sum + value, 0);
  const blocksMined = Object.values(mined).reduce((sum, value) => sum + value, 0);
  const tiles: { key: string; value: number }[] = [
    { key: 'distance', value: totalDistance },
    { key: 'blocks_mined', value: blocksMined },
    { key: 'diamonds', value: (mined.diamond_ore ?? 0) + (mined.deepslate_diamond_ore ?? 0) },
    ...HEADLINE_STATS.map((key) => ({ key, value: custom[key] ?? 0 })),
  ].filter((t) => t.value > 0);

  return (
    <>
      <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
        {tiles.map((tile) => {
          const def = STAT_DEFS[tile.key];
          return <Fact key={tile.key} icon={def.icon} label={def.label} value={def.format(tile.value)} />;
        })}
      </div>

      {distances.length > 0 && (
        <Block title="Como se locomove">
          <ul className="flex flex-col gap-2 text-sm">
            {distances
              .sort((a, b) => b[1] - a[1])
              .map(([key, value]) => (
                <li key={key} className="flex flex-col gap-1">
                  <div className="flex justify-between gap-2">
                    <span>{movementLabel(key)}</span>
                    <span className="tabular-nums text-ink-muted">{formatDistance(value)}</span>
                  </div>
                  <div className="h-1.5 rounded bg-primary-soft">
                    <div className="h-1.5 rounded bg-primary" style={{ width: `${Math.max((value / totalDistance) * 100, 2)}%` }} />
                  </div>
                </li>
              ))}
          </ul>
        </Block>
      )}

      <div className="grid gap-6 sm:grid-cols-2">
        <Block title="Mais minerados">
          <BarList items={toItems(stats.mined)} emptyMessage="Nada minerado." />
        </Block>
        <Block title="Mobs abatidos">
          <BarList items={toItems(stats.killed)} emptyMessage="Nenhum abate." />
        </Block>
        <Block title="Morreu para">
          <BarList items={toItems(stats.killed_by)} emptyMessage="Nunca morreu para um mob." />
        </Block>
        <Block title="Mais craftados">
          <BarList items={toItems(stats.crafted)} emptyMessage="Nada craftado." />
        </Block>
        <Block title="Mais usados">
          <BarList items={toItems(stats.used)} emptyMessage="Nada usado." />
        </Block>
        <Block title="Ferramentas quebradas">
          <BarList items={toItems(stats.broken)} emptyMessage="Nada quebrado." />
        </Block>
      </div>
    </>
  );
}

function AdvancementsTab({ advancements }: { advancements: Advancement[] }) {
  if (advancements.length === 0) {
    return <p className="text-sm text-ink-muted">Nenhuma conquista ainda.</p>;
  }
  const byCategory = new Map<string, Advancement[]>();
  for (const a of advancements) {
    byCategory.set(a.category, [...(byCategory.get(a.category) ?? []), a]);
  }
  const order = Object.keys(ADVANCEMENT_CATEGORIES);
  const rank = (category: string) => (order.includes(category) ? order.indexOf(category) : order.length);
  const categories = [...byCategory.keys()].sort((a, b) => rank(a) - rank(b));

  return (
    <>
      <p className="text-sm text-ink-muted">
        {numberFormat.format(advancements.length)} conquistas, da mais recente para a mais antiga em cada grupo.
      </p>
      {categories.map((category) => {
        const items = [...byCategory.get(category)!].reverse();
        return (
          <Block key={category} title={`${ADVANCEMENT_CATEGORIES[category] ?? humanize(category)} (${items.length})`}>
            <ul className="grid gap-1.5 text-sm sm:grid-cols-2">
              {items.map((a) => (
                <li key={a.id} className="flex items-baseline justify-between gap-2 rounded-md border border-line px-2.5 py-1.5">
                  <span className="truncate">{advancementTitle(a.id)}</span>
                  {a.doneAt && (
                    <time dateTime={a.doneAt} className="shrink-0 text-xs text-ink-muted">
                      {dateFormat.format(Date.parse(a.doneAt))}
                    </time>
                  )}
                </li>
              ))}
            </ul>
          </Block>
        );
      })}
    </>
  );
}

function HistoryTab({ history }: { history: SnapshotPoint[] }) {
  if (history.length === 0) {
    return <p className="text-sm text-ink-muted">Ainda não há histórico para este jogador.</p>;
  }

  // Values stay the same until the next snapshot, so the line runs flat up to now.
  const now = Date.now();
  const series = (pick: (s: SnapshotPoint) => number | null): ChartPoint[] | null => {
    const points = history
      .filter((s) => pick(s) !== null)
      .map((s) => ({ time: Date.parse(s.capturedAt), value: pick(s)! }));
    if (points.length === 0) return null;
    return [...points, { time: now, value: points[points.length - 1].value }];
  };
  const kills = series((s) => s.mobKills);
  const mined = series((s) => s.blocksMined);
  const count = (value: number) => numberFormat.format(Math.round(value));

  return (
    <>
      <LineChart
        title="Horas jogadas"
        points={series((s) => ticksToHoursPrecise(s.playTimeTicks))!}
        formatValue={(hours) => `${numberFormat.format(Math.round(hours * 10) / 10)}h`}
      />
      <LineChart title="Mortes" points={series((s) => s.deaths)!} formatValue={count} integer />
      {kills && <LineChart title="Mobs abatidos" points={kills} formatValue={count} integer />}
      {mined && <LineChart title="Blocos minerados" points={mined} formatValue={count} integer />}
      <details className="text-sm">
        <summary className="cursor-pointer text-ink-muted hover:text-ink">Ver dados</summary>
        <table className="mt-2 w-full tabular-nums">
          <thead className="text-left text-ink-muted">
            <tr>
              <th className="py-1 font-medium">Data</th>
              <th className="py-1 font-medium">Horas</th>
              <th className="py-1 font-medium">Mortes</th>
              <th className="py-1 font-medium">Abates</th>
              <th className="py-1 font-medium">Blocos</th>
            </tr>
          </thead>
          <tbody>
            {[...history].reverse().map((s, i) => (
              <tr key={i} className="border-t border-line">
                <td className="py-1">{dateTimeFormat.format(Date.parse(s.capturedAt))}</td>
                <td className="py-1">{numberFormat.format(Math.round(ticksToHoursPrecise(s.playTimeTicks) * 10) / 10)}</td>
                <td className="py-1">{numberFormat.format(s.deaths)}</td>
                <td className="py-1">{s.mobKills === null ? '-' : numberFormat.format(s.mobKills)}</td>
                <td className="py-1">{s.blocksMined === null ? '-' : numberFormat.format(s.blocksMined)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </details>
    </>
  );
}

function Fact({ icon: Icon, label, value }: { icon: LucideIcon; label: string; value: string }) {
  return (
    <div className="rounded-lg border border-line p-3">
      <p className="flex items-center gap-1.5 truncate text-xs text-ink-muted">
        <Icon className="size-3.5 shrink-0" aria-hidden />
        {label}
      </p>
      <p className="mt-0.5 truncate font-semibold tabular-nums">{value}</p>
    </div>
  );
}

function Block({ title, icon: Icon, note, children }: { title: string; icon?: LucideIcon; note?: string; children: ReactNode }) {
  return (
    <section className="flex flex-col gap-2.5">
      <header className="flex flex-wrap items-baseline justify-between gap-x-3">
        <h3 className="flex items-center gap-1.5 text-sm font-medium">
          {Icon && <Icon className="size-4 text-primary" aria-hidden />}
          {title}
        </h3>
        {note && <p className="text-xs text-ink-muted">{note}</p>}
      </header>
      {children}
    </section>
  );
}
