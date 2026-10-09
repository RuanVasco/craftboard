import { useEffect, useRef, useState } from 'react';
import {
  fetchActivity,
  fetchAdvancements,
  fetchFeed,
  fetchOverview,
  fetchServer,
  STREAM_URL,
  type Activity,
  type AdvancementsOverview,
  type FeedEvent,
  type Overview,
  type ServerInfo,
} from './api';

// Highlights move slowly and cost more to build, so they refresh less eagerly than the ranking.
const HIGHLIGHTS_DEBOUNCE_MS = 5000;
const FEED_SIZE = 100;

export function useLiveData() {
  const [server, setServer] = useState<ServerInfo | null>(null);
  const [overview, setOverview] = useState<Overview | null>(null);
  const [advancements, setAdvancements] = useState<AdvancementsOverview | null>(null);
  const [activity, setActivity] = useState<Activity | null>(null);
  const [feed, setFeed] = useState<FeedEvent[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [live, setLive] = useState(false);
  // Bumped on every stats event, so the ranking and open panels know to refetch.
  const [statsVersion, setStatsVersion] = useState(0);
  const highlightsTimer = useRef<ReturnType<typeof setTimeout>>(undefined);

  const fail = (e: Error) => setError(e.message);

  function loadHighlights() {
    fetchOverview().then(setOverview).catch(fail);
    fetchAdvancements().then(setAdvancements).catch(fail);
    fetchActivity().then(setActivity).catch(fail);
  }

  useEffect(() => {
    fetchServer().then(setServer).catch(fail);
    fetchFeed().then(setFeed).catch(fail);
    loadHighlights();

    // EventSource reconnects on its own, so only the indicator needs handling.
    const source = new EventSource(STREAM_URL);
    source.onopen = () => setLive(true);
    source.onerror = () => setLive(false);
    source.addEventListener('player', () => {
      setStatsVersion((v) => v + 1);
      clearTimeout(highlightsTimer.current);
      highlightsTimer.current = setTimeout(loadHighlights, HIGHLIGHTS_DEBOUNCE_MS);
    });
    source.addEventListener('server', (event) => setServer(JSON.parse(event.data)));
    source.addEventListener('feed', (event) => {
      const added: FeedEvent = JSON.parse(event.data);
      setFeed((current) => [added, ...current].slice(0, FEED_SIZE));
    });
    return () => {
      source.close();
      clearTimeout(highlightsTimer.current);
    };
  }, []);

  return { server, overview, advancements, activity, feed, error, live, statsVersion };
}
