import { Clock, Skull, Users, type LucideIcon } from 'lucide-react';
import type { PlayerStats } from '../api';
import { numberFormat, ticksToHours } from '../format';

export function StatTiles({ players }: { players: PlayerStats[] }) {
  const totalTicks = players.reduce((sum, p) => sum + p.playTimeTicks, 0);
  const totalDeaths = players.reduce((sum, p) => sum + p.deaths, 0);

  return (
    <section className="grid grid-cols-3 gap-2 sm:gap-3">
      <Tile icon={Users} label="Jogadores" value={numberFormat.format(players.length)} />
      <Tile icon={Clock} label="Horas" value={numberFormat.format(ticksToHours(totalTicks))} />
      <Tile icon={Skull} label="Mortes" value={numberFormat.format(totalDeaths)} />
    </section>
  );
}

function Tile({ icon: Icon, label, value }: { icon: LucideIcon; label: string; value: string }) {
  return (
    <div className="rounded-xl border border-line bg-surface p-3 sm:p-4">
      <p className="flex items-center gap-1.5 text-xs text-ink-muted sm:gap-2 sm:text-sm">
        <Icon className="size-4" aria-hidden />
        {label}
      </p>
      <p className="mt-1 text-2xl font-semibold tabular-nums sm:text-3xl">{value}</p>
    </div>
  );
}
