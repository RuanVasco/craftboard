import { LoaderCircle, X } from 'lucide-react';
import { useEffect, useState } from 'react';
import { fetchHistory, type PlayerStats, type SnapshotPoint } from '../api';
import { dateTimeFormat, numberFormat, ticksToHoursPrecise } from '../format';
import { LineChart, type ChartPoint } from './LineChart';

interface Props {
  player: PlayerStats;
  onClose: () => void;
}

export function PlayerPanel({ player, onClose }: Props) {
  const [history, setHistory] = useState<SnapshotPoint[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchHistory(player.uuid)
      .then(setHistory)
      .catch((e: Error) => setError(e.message));
  }, [player.uuid]);

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
        aria-label={`Histórico de ${player.name}`}
        className="flex max-h-[90vh] w-full max-w-2xl flex-col gap-5 overflow-y-auto rounded-t-xl bg-surface p-4 sm:rounded-xl sm:p-6"
        onClick={(event) => event.stopPropagation()}
      >
        <header className="flex items-center justify-between gap-4">
          <h2 className="truncate text-lg font-semibold">{player.name}</h2>
          <button type="button" onClick={onClose} className="rounded-md p-1 text-ink-muted hover:text-ink" aria-label="Fechar">
            <X className="size-5" aria-hidden />
          </button>
        </header>
        <PanelBody history={history} error={error} />
      </section>
    </div>
  );
}

function PanelBody({ history, error }: { history: SnapshotPoint[] | null; error: string | null }) {
  if (error) {
    return <p className="text-sm text-bad">Não foi possível carregar o histórico ({error}).</p>;
  }
  if (!history) {
    return <LoaderCircle className="mx-auto size-6 animate-spin text-ink-muted" aria-label="Carregando" />;
  }
  if (history.length === 0) {
    return <p className="text-sm text-ink-muted">Ainda não há histórico para este jogador.</p>;
  }

  // Values stay the same until the next snapshot, so the line runs flat up to now.
  const now = Date.now();
  const series = (pick: (s: SnapshotPoint) => number): ChartPoint[] => [
    ...history.map((s) => ({ time: Date.parse(s.capturedAt), value: pick(s) })),
    { time: now, value: pick(history[history.length - 1]) },
  ];

  return (
    <>
      <LineChart
        title="Horas jogadas"
        points={series((s) => ticksToHoursPrecise(s.playTimeTicks))}
        formatValue={(hours) => `${numberFormat.format(Math.round(hours * 10) / 10)}h`}
      />
      <LineChart
        title="Mortes"
        points={series((s) => s.deaths)}
        formatValue={(deaths) => numberFormat.format(Math.round(deaths))}
      />
      <details className="text-sm">
        <summary className="cursor-pointer text-ink-muted hover:text-ink">Ver dados</summary>
        <table className="mt-2 w-full tabular-nums">
          <thead className="text-left text-ink-muted">
            <tr>
              <th className="py-1 font-medium">Data</th>
              <th className="py-1 font-medium">Horas</th>
              <th className="py-1 font-medium">Mortes</th>
            </tr>
          </thead>
          <tbody>
            {[...history].reverse().map((s, i) => (
              <tr key={i} className="border-t border-line">
                <td className="py-1">{dateTimeFormat.format(Date.parse(s.capturedAt))}</td>
                <td className="py-1">{numberFormat.format(Math.round(ticksToHoursPrecise(s.playTimeTicks) * 10) / 10)}</td>
                <td className="py-1">{numberFormat.format(s.deaths)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </details>
    </>
  );
}
