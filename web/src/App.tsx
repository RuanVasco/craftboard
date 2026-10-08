import { Activity as ActivityIcon, CircleAlert, Ghost, Pickaxe, Sword } from 'lucide-react';
import { useCallback, useState } from 'react';
import type { Period } from './api';
import { ActivityCharts } from './components/ActivityCharts';
import { BarList } from './components/BarList';
import { FeedSection } from './components/FeedSection';
import { PeriodFilter } from './components/PeriodFilter';
import { PlayerPanel, type SelectedPlayer } from './components/PlayerPanel';
import { PlayerRanking } from './components/PlayerRanking';
import { RecentAdvancements } from './components/RecentAdvancements';
import { RecordsSection } from './components/RecordsSection';
import { Section } from './components/Section';
import { ServerBanner } from './components/ServerBanner';
import { StatTiles } from './components/StatTiles';
import { useLiveData } from './useLiveData';

export default function App() {
  const [period, setPeriod] = useState<Period>('all');
  const [selected, setSelected] = useState<SelectedPlayer | null>(null);
  const closePanel = useCallback(() => setSelected(null), []);
  const { players, server, overview, advancements, activity, feed, error, live, statsVersion } = useLiveData(period);

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
          advancements={advancements?.counts ?? {}}
          showAdvancements={period === 'all'}
          onlinePlayers={server?.onlinePlayers ?? null}
          emptyMessage={period === 'all' ? 'Nenhum jogador ainda.' : 'Ninguém jogou neste período.'}
          onSelect={(p) => setSelected({ uuid: p.uuid, name: p.name })}
        />

        <div className="grid gap-4 md:grid-cols-2">
          <FeedSection events={feed} />
          <RecentAdvancements advancements={advancements?.recent ?? []} />
        </div>

        {overview && (
          <>
            <RecordsSection records={overview.records} onSelect={(uuid, name) => setSelected({ uuid, name })} />
            <div className="grid gap-4 md:grid-cols-3">
              <Section title="Mais minerados" icon={Pickaxe}>
                <BarList items={overview.topMined} emptyMessage="Nenhum bloco minerado." />
              </Section>
              <Section title="Mais abatidos" icon={Sword}>
                <BarList items={overview.topKilled} emptyMessage="Nenhum mob abatido." />
              </Section>
              <Section title="O que mais mata" icon={Ghost}>
                <BarList items={overview.topKilledBy} emptyMessage="Ninguém morreu para um mob." />
              </Section>
            </div>
          </>
        )}

        {activity && (
          <Section title="Atividade" icon={ActivityIcon} subtitle={`Últimos ${activity.days} dias`}>
            <ActivityCharts activity={activity} />
          </Section>
        )}
      </main>
      {selected && (
        <PlayerPanel
          player={selected}
          online={server?.onlinePlayers?.includes(selected.name) ?? false}
          statsVersion={statsVersion}
          onClose={closePanel}
        />
      )}
    </div>
  );
}
