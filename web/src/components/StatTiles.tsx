import { Clock, Pickaxe, Route, Skull, Sword, Users, type LucideIcon } from 'lucide-react';
import type { RankingTotals } from '../api';
import { compactParts, numberFormat, ticksToHours } from '../format';

const EMPTY: RankingTotals = {
  players: 0,
  playTimeTicks: 0,
  deaths: 0,
  mobKills: 0,
  playerKills: 0,
  blocksMined: 0,
  distanceCm: 0,
};

export function StatTiles({ totals = EMPTY }: { totals?: RankingTotals }) {
  const km = compactParts(Math.round(totals.distanceCm / 100_000));

  return (
    <section className="grid grid-cols-2 gap-2 sm:grid-cols-3 sm:gap-3 lg:grid-cols-6">
      <Tile icon={Users} label="Jogadores" value={numberFormat.format(totals.players)} />
      <Tile icon={Clock} label="Horas" value={numberFormat.format(ticksToHours(totals.playTimeTicks))} />
      <Tile icon={Skull} label="Mortes" value={numberFormat.format(totals.deaths)} />
      <Tile icon={Sword} label="Abates" {...compact(totals.mobKills)} />
      <Tile icon={Pickaxe} label="Blocos" {...compact(totals.blocksMined)} />
      <Tile icon={Route} label="Distância" value={km.number} unit={`${km.suffix} km`.trim()} />
    </section>
  );
}

function compact(value: number) {
  const { number, suffix } = compactParts(value);
  return { value: number, unit: suffix };
}

function Tile({ icon: Icon, label, value, unit }: { icon: LucideIcon; label: string; value: string; unit?: string }) {
  return (
    <div className="min-w-0 rounded-xl border border-line bg-surface p-3 sm:p-4">
      <p className="flex items-center gap-1.5 truncate text-xs text-ink-muted sm:gap-2 sm:text-sm">
        <Icon className="size-4 shrink-0" aria-hidden />
        {label}
      </p>
      <p className="mt-1 truncate text-2xl font-semibold tabular-nums sm:text-3xl">
        {value}
        {unit && <span className="ml-1 text-sm font-medium text-ink-muted sm:text-base">{unit}</span>}
      </p>
    </div>
  );
}
