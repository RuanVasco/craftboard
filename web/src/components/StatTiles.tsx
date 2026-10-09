import { Clock, Pickaxe, Route, Skull, Sword, Users, type LucideIcon } from 'lucide-react';
import type { PlayerStats } from '../api';
import { compactParts, numberFormat, ticksToHours } from '../format';

export function StatTiles({ players }: { players: PlayerStats[] }) {
  const sum = (pick: (p: PlayerStats) => number) => players.reduce((total, p) => total + pick(p), 0);
  const km = compactParts(Math.round(sum((p) => p.distanceCm) / 100_000));

  return (
    <section className="grid grid-cols-2 gap-2 sm:grid-cols-3 sm:gap-3 lg:grid-cols-6">
      <Tile icon={Users} label="Jogadores" value={numberFormat.format(players.length)} />
      <Tile icon={Clock} label="Horas" value={numberFormat.format(ticksToHours(sum((p) => p.playTimeTicks)))} />
      <Tile icon={Skull} label="Mortes" value={numberFormat.format(sum((p) => p.deaths))} />
      <Tile icon={Sword} label="Abates" {...compact(sum((p) => p.mobKills))} />
      <Tile icon={Pickaxe} label="Blocos" {...compact(sum((p) => p.blocksMined))} />
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
