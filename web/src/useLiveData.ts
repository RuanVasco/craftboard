import { useEffect, useRef, useState } from 'react';
import {
  fetchActivity,
  fetchAdvancements,
  fetchFeed,
  fetchOverview,
  fetchPlayers,
  fetchServer,
  STREAM_URL,
  type Activity,
  type AdvancementsOverview,
  type FeedEvent,
  type Overview,
  type Period,
  type PlayerStats,
  type ServerInfo,
} from './api';

// A save flushes every online player at once, so their events are folded into one request.
const RELOAD_DEBOUNCE_MS = 1000;
// Highlights move slowly and cost more to build, so they refresh less eagerly than the ranking.
const HIGHLIGHTS_DEBOUNCE_MS = 5000;
const FEED_SIZE = 100;

export function useLiveData(period: Period) {
  const [players, setPlayers] = useState<PlayerStats[]>([]);
  const [server, setServer] = useState<ServerInfo | null>(null);
  const [overview, setOverview] = useState<Overview | null>(null);
  const [advancements, setAdvancements] = useState<AdvancementsOverview | null>(null);
  const [activity, setActivity] = useState<Activity | null>(null);
  const [feed, setFeed] = useState<FeedEvent[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [live, setLive] = useState(false);
  // Bumped on every stats event, so open panels know to refetch.
  const [statsVersion, setStatsVersion] = useState(0);
  const periodRef = useRef(period);
  const latestRequest = useRef(0);
  const reloadTimer = useRef<ReturnType<typeof setTimeout>>(undefined);
  const highlightsTimer = useRef<ReturnType<typeof setTimeout>>(undefined);

  const fail = (e: Error) => setError(e.message);

  // Only the most recent request may update the list, whatever order the responses arrive in.
  function loadPlayers() {
    const request = ++latestRequest.current;
    fetchPlayers(periodRef.current)
      .then((list) => {
        if (request === latestRequest.current) setPlayers(list);
      })
      .catch(fail);
  }

  function loadHighlights() {
    fetchOverview().then(setOverview).catch(fail);
    fetchAdvancements().then(setAdvancements).catch(fail);
    fetchActivity().then(setActivity).catch(fail);
  }

  useEffect(() => {
    periodRef.current = period;
    clearTimeout(reloadTimer.current);
    loadPlayers();
  }, [period]);

  useEffect(() => {
    fetchServer().then(setServer).catch(fail);
    fetchFeed().then(setFeed).catch(fail);
    loadHighlights();

    // EventSource reconnects on its own, so only the indicator needs handling.
    const source = new EventSource(STREAM_URL);
    source.onopen = () => setLive(true);
    source.onerror = () => setLive(false);
    source.addEventListener('player', (event) => {
      setStatsVersion((v) => v + 1);
      clearTimeout(highlightsTimer.current);
      highlightsTimer.current = setTimeout(loadHighlights, HIGHLIGHTS_DEBOUNCE_MS);
      // Events carry totals, so a period view has to ask the API for the new deltas.
      if (periodRef.current !== 'all') {
        clearTimeout(reloadTimer.current);
        reloadTimer.current = setTimeout(loadPlayers, RELOAD_DEBOUNCE_MS);
        return;
      }
      const updated: PlayerStats = JSON.parse(event.data);
      setPlayers((current) => [...current.filter((p) => p.uuid !== updated.uuid), updated]);
    });
    source.addEventListener('server', (event) => setServer(JSON.parse(event.data)));
    source.addEventListener('feed', (event) => {
      const added: FeedEvent = JSON.parse(event.data);
      setFeed((current) => [added, ...current].slice(0, FEED_SIZE));
    });
    return () => {
      source.close();
      clearTimeout(reloadTimer.current);
      clearTimeout(highlightsTimer.current);
    };
  }, []);

  return { players, server, overview, advancements, activity, feed, error, live, statsVersion };
}
