import { humanize } from '../names';
import { numberFormat } from '../format';

interface Props {
  items: { id: string; value: number }[];
  emptyMessage: string;
  limit?: number;
}

/** Ranked counts as horizontal bars, labeled directly so no legend or axis is needed. */
export function BarList({ items, emptyMessage, limit = 8 }: Props) {
  const shown = [...items].sort((a, b) => b.value - a.value).slice(0, limit);
  if (shown.length === 0) {
    return <p className="py-4 text-center text-sm text-ink-muted">{emptyMessage}</p>;
  }
  const max = Math.max(...shown.map((i) => i.value));
  return (
    <ol className="flex flex-col gap-2 text-sm">
      {shown.map((item) => (
        <li key={item.id} className="flex flex-col gap-1">
          <div className="flex items-baseline justify-between gap-2">
            <span className="truncate">{humanize(item.id)}</span>
            <span className="shrink-0 tabular-nums text-ink-muted">{numberFormat.format(item.value)}</span>
          </div>
          <div className="h-1.5 rounded bg-primary-soft">
            <div className="h-1.5 rounded bg-primary" style={{ width: `${Math.max((item.value / max) * 100, 2)}%` }} />
          </div>
        </li>
      ))}
    </ol>
  );
}

export function toItems(category: Record<string, number> | undefined) {
  return Object.entries(category ?? {}).map(([id, value]) => ({ id, value }));
}
