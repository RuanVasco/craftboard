import { useEffect, useRef, useState } from 'react';
import { fetchPlayers, fetchServer, STREAM_URL, type Period, type PlayerStats, type ServerInfo } from './api';

// A save flushes every online player at once, so their events are folded into one request.
const RELOAD_DEBOUNCE_MS = 1000;

export function useLiveData(period: Period) {
  const [players, setPlayers] = useState<PlayerStats[]>([]);
  const [server, setServer] = useState<ServerInfo | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [live, setLive] = useState(false);
  const periodRef = useRef(period);
  const latestRequest = useRef(0);
  const reloadTimer = useRef<ReturnType<typeof setTimeout>>(undefined);

  // Only the most recent request may update the list, whatever order the responses arrive in.
  function loadPlayers() {
    const request = ++latestRequest.current;
    fetchPlayers(periodRef.current)
      .then((list) => {
        if (request === latestRequest.current) setPlayers(list);
      })
      .catch((e: Error) => setError(e.message));
  }

  useEffect(() => {
    periodRef.current = period;
    clearTimeout(reloadTimer.current);
    loadPlayers();
  }, [period]);

  useEffect(() => {
    fetchServer()
      .then(setServer)
      .catch((e: Error) => setError(e.message));

    // EventSource reconnects on its own, so only the indicator needs handling.
    const source = new EventSource(STREAM_URL);
    source.onopen = () => setLive(true);
    source.onerror = () => setLive(false);
    source.addEventListener('player', (event) => {
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
    return () => {
      source.close();
      clearTimeout(reloadTimer.current);
    };
  }, []);

  return { players, server, error, live };
}
