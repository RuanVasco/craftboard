import { CircleAlert } from 'lucide-react';
import { useCallback, useState } from 'react';
import type { Period, PlayerStats } from './api';
import { PeriodFilter } from './components/PeriodFilter';
import { PlayerPanel } from './components/PlayerPanel';
import { PlayerRanking } from './components/PlayerRanking';
import { ServerBanner } from './components/ServerBanner';
import { StatTiles } from './components/StatTiles';
import { useLiveData } from './useLiveData';

export default function App() {
  const [period, setPeriod] = useState<Period>('all');
  const [selected, setSelected] = useState<PlayerStats | null>(null);
  const closePanel = useCallback(() => setSelected(null), []);
  const { players, server, error, live } = useLiveData(period);

  return (
    <div className="min-h-screen">
      <ServerBanner server={server} live={live} />
      <main className="mx-auto flex max-w-5xl flex-col gap-4 px-4 py-6">
        {error && (
          <p className="flex items-center gap-2 rounded-lg border border-bad/30 bg-bad/10 px-4 py-3 text-sm text-bad">
            <CircleAlert className="size-4" aria-hidden />
            Não foi possível carregar os dados ({error}).
          </p>
        )}
        <PeriodFilter value={period} onChange={setPeriod} />
        <StatTiles players={players} />
        <PlayerRanking
          players={players}
          onlinePlayers={server?.onlinePlayers ?? null}
          emptyMessage={period === 'all' ? 'Nenhum jogador ainda.' : 'Ninguém jogou neste período.'}
          onSelect={setSelected}
        />
      </main>
      {selected && <PlayerPanel player={selected} onClose={closePanel} />}
    </div>
  );
}
