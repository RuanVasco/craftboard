import { useState, type PointerEvent } from 'react';
import { dateTimeFormat } from '../format';

export interface ChartPoint {
  time: number;
  value: number;
}

interface Props {
  title: string;
  points: ChartPoint[];
  formatValue: (value: number) => string;
}

// Single series on a 0-based axis. The SVG stretches to the box and labels live in HTML, so
// text never gets distorted by the non-uniform scale.
export function LineChart({ title, points, formatValue }: Props) {
  const [hovered, setHovered] = useState<number | null>(null);

  const start = points[0].time;
  const span = Math.max(1, points[points.length - 1].time - start);
  const top = niceMax(Math.max(...points.map((p) => p.value)));
  const x = (p: ChartPoint) => ((p.time - start) / span) * 100;
  const y = (p: ChartPoint) => 100 - (p.value / top) * 100;
  const path = points.map((p, i) => `${i === 0 ? 'M' : 'L'}${x(p)},${y(p)}`).join(' ');

  const onPointerMove = (event: PointerEvent<HTMLDivElement>) => {
    const rect = event.currentTarget.getBoundingClientRect();
    const target = ((event.clientX - rect.left) / rect.width) * 100;
    let nearest = 0;
    points.forEach((p, i) => {
      if (Math.abs(x(p) - target) < Math.abs(x(points[nearest]) - target)) nearest = i;
    });
    setHovered(nearest);
  };

  const active = hovered === null ? null : points[hovered];

  return (
    <figure className="flex flex-col gap-2">
      <figcaption className="text-sm font-medium">{title}</figcaption>
      <div className="flex gap-2">
        <div className="flex h-36 w-10 shrink-0 flex-col justify-between text-right text-xs tabular-nums text-ink-muted">
          <span>{formatValue(top)}</span>
          <span>{formatValue(top / 2)}</span>
          <span>{formatValue(0)}</span>
        </div>
        <div
          className="relative h-36 flex-1 touch-none"
          onPointerMove={onPointerMove}
          onPointerDown={onPointerMove}
          onPointerLeave={() => setHovered(null)}
        >
          <svg viewBox="0 0 100 100" preserveAspectRatio="none" className="absolute inset-0 size-full overflow-visible" aria-hidden>
            {[0, 50, 100].map((gridY) => (
              <line key={gridY} x1={0} x2={100} y1={gridY} y2={gridY} stroke="var(--line)" vectorEffect="non-scaling-stroke" />
            ))}
            <path
              d={path}
              fill="none"
              stroke="var(--primary)"
              strokeWidth={2}
              strokeLinejoin="round"
              strokeLinecap="round"
              vectorEffect="non-scaling-stroke"
            />
          </svg>
          {active && (
            <>
              <div className="pointer-events-none absolute inset-y-0 w-px bg-ink-muted/40" style={{ left: `${x(active)}%` }} />
              <div
                className="pointer-events-none absolute size-2.5 -translate-x-1/2 -translate-y-1/2 rounded-full bg-primary ring-2 ring-surface"
                style={{ left: `${x(active)}%`, top: `${y(active)}%` }}
              />
              <div
                className="pointer-events-none absolute -top-2 z-10 -translate-x-1/2 -translate-y-full whitespace-nowrap rounded-md border border-line bg-surface px-2 py-1 text-xs shadow"
                style={{ left: `${Math.min(85, Math.max(15, x(active)))}%` }}
              >
                <span className="font-semibold tabular-nums">{formatValue(active.value)}</span>
                <span className="ml-2 text-ink-muted">{dateTimeFormat.format(active.time)}</span>
              </div>
            </>
          )}
        </div>
      </div>
      <div className="flex justify-between pl-12 text-xs text-ink-muted">
        <span>{dateTimeFormat.format(start)}</span>
        <span>agora</span>
      </div>
    </figure>
  );
}

function niceMax(value: number): number {
  if (value <= 0) return 1;
  const magnitude = 10 ** Math.floor(Math.log10(value));
  // Steps whose half is still a round number, since the middle gridline is labeled too.
  const step = [1, 2, 4, 6, 8, 10].find((s) => s * magnitude >= value) ?? 10;
  return step * magnitude;
}
