import { useState, type ReactNode } from 'react';
import type { Activity } from '../api';
import { dateFormat, decimalFormat, formatHours } from '../format';

const WEEKDAYS = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom'];
const WEEKDAYS_LONG = ['segunda', 'terça', 'quarta', 'quinta', 'sexta', 'sábado', 'domingo'];
// One hue, light to dark. The empty step is the soft surface so cells with no play still read as cells.
const STEPS = [0.2, 0.4, 0.6, 0.8, 1];

export function ActivityCharts({ activity }: { activity: Activity }) {
  const total = activity.daily.reduce((sum, d) => sum + d.hours, 0);
  if (total === 0) {
    return <p className="py-6 text-center text-sm text-ink-muted">Sem atividade registrada nos últimos {activity.days} dias.</p>;
  }
  return (
    <div className="flex flex-col gap-6">
      <DailyBars daily={activity.daily} />
      <Heatmap heatmap={activity.heatmap} />
    </div>
  );
}

function DailyBars({ daily }: { daily: Activity['daily'] }) {
  const [hovered, setHovered] = useState<number | null>(null);
  const max = Math.max(...daily.map((d) => d.hours));
  const active = hovered === null ? null : daily[hovered];

  return (
    <figure className="flex flex-col gap-2">
      <figcaption className="flex items-baseline justify-between gap-2 text-sm">
        <span className="font-medium">Horas jogadas por dia</span>
        <span className="text-xs text-ink-muted">máx. {formatHours(max)}</span>
      </figcaption>
      <div className="relative">
        <div className="flex h-32 items-end gap-0.5 border-b border-line" onPointerLeave={() => setHovered(null)}>
          {daily.map((day, index) => (
            <div
              key={day.date}
              className="flex h-full flex-1 items-end"
              onPointerEnter={() => setHovered(index)}
              onPointerDown={() => setHovered(index)}
              aria-label={`${dateFormat.format(parseDate(day.date))}: ${formatHours(day.hours)}`}
            >
              <div
                className={`w-full rounded-t ${hovered === index ? 'bg-primary' : 'bg-primary/75'}`}
                style={{ height: day.hours > 0 ? `${Math.max((day.hours / max) * 100, 3)}%` : 0 }}
              />
            </div>
          ))}
        </div>
        {active && hovered !== null && (
          <div
            className="pointer-events-none absolute -top-2 z-10 -translate-x-1/2 -translate-y-full whitespace-nowrap rounded-md border border-line bg-surface px-2 py-1 text-xs shadow"
            style={{ left: `${Math.min(85, Math.max(15, ((hovered + 0.5) / daily.length) * 100))}%` }}
          >
            <span className="font-semibold tabular-nums">{formatHours(active.hours)}</span>
            <span className="ml-2 text-ink-muted">
              {dateFormat.format(parseDate(active.date))}, {active.players} {active.players === 1 ? 'jogador' : 'jogadores'}
            </span>
          </div>
        )}
      </div>
      <div className="flex justify-between text-xs text-ink-muted">
        <span>{dateFormat.format(parseDate(daily[0].date))}</span>
        <span>hoje</span>
      </div>
    </figure>
  );
}

function Heatmap({ heatmap }: { heatmap: number[][] }) {
  const [hovered, setHovered] = useState<{ day: number; hour: number } | null>(null);
  const max = Math.max(...heatmap.flat());
  const step = (hours: number) => (hours <= 0 ? -1 : Math.min(STEPS.length - 1, Math.floor((hours / max) * STEPS.length)));
  const active = hovered && heatmap[hovered.day][hovered.hour];

  return (
    <figure className="flex flex-col gap-2">
      <figcaption className="flex flex-wrap items-baseline justify-between gap-2 text-sm">
        <span className="font-medium">Quando o servidor joga</span>
        <span className="text-xs text-ink-muted" aria-live="polite">
          {hovered
            ? `${WEEKDAYS_LONG[hovered.day]}, ${hovered.hour}h: ${active ? formatHours(active) : 'ninguém'}`
            : 'Horas somadas por dia da semana e hora'}
        </span>
      </figcaption>
      <div className="overflow-x-auto">
        <div className="grid min-w-[480px] grid-cols-[2.25rem_repeat(24,minmax(0,1fr))] gap-0.5" onPointerLeave={() => setHovered(null)}>
          {heatmap.map((row, day) => (
            <Row key={day} label={WEEKDAYS[day]}>
              {row.map((hours, hour) => {
                const s = step(hours);
                const isActive = hovered?.day === day && hovered.hour === hour;
                return (
                  <div
                    key={hour}
                    role="img"
                    aria-label={`${WEEKDAYS_LONG[day]} ${hour}h: ${decimalFormat.format(hours)} horas`}
                    onPointerEnter={() => setHovered({ day, hour })}
                    onPointerDown={() => setHovered({ day, hour })}
                    className={`aspect-square rounded-sm ${s < 0 ? 'bg-primary-soft' : 'bg-primary'} ${isActive ? 'ring-2 ring-ink' : ''}`}
                    style={s < 0 ? undefined : { opacity: STEPS[s] }}
                  />
                );
              })}
            </Row>
          ))}
          <span />
          {Array.from({ length: 24 }, (_, hour) => (
            <span key={hour} className="text-center text-[10px] text-ink-muted">
              {hour % 3 === 0 ? hour : ''}
            </span>
          ))}
        </div>
      </div>
      <div className="flex items-center justify-end gap-1.5 text-xs text-ink-muted">
        menos
        <span className="size-3 rounded-sm bg-primary-soft" />
        {STEPS.map((opacity) => (
          <span key={opacity} className="size-3 rounded-sm bg-primary" style={{ opacity }} />
        ))}
        mais
      </div>
    </figure>
  );
}

function Row({ label, children }: { label: string; children: ReactNode }) {
  return (
    <>
      <span className="self-center pr-1 text-right text-xs text-ink-muted">{label}</span>
      {children}
    </>
  );
}

// "2026-10-08" parsed as local midnight, not UTC, so the label never shifts a day back.
function parseDate(date: string): Date {
  const [year, month, day] = date.split('-').map(Number);
  return new Date(year, month - 1, day);
}
