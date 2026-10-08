import { useEffect, useRef, useState } from 'react';
import { fetchPlayers, fetchServer, STREAM_URL, type Period, type PlayerStats, type ServerInfo } from './api';

export function useLiveData(period: Period) {
  const [players, setPlayers] = useState<PlayerStats[]>([]);
  const [server, setServer] = useState<ServerInfo | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [live, setLive] = useState(false);
  const periodRef = useRef(period);

  useEffect(() => {
    periodRef.current = period;
    loadPlayers(period);
  }, [period]);

  // Drops responses for a period the user already switched away from.
  function loadPlayers(requested: Period) {
    fetchPlayers(requested)
      .then((list) => {
        if (periodRef.current === requested) setPlayers(list);
      })
      .catch((e: Error) => setError(e.message));
  }

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
        loadPlayers(periodRef.current);
        return;
      }
      const updated: PlayerStats = JSON.parse(event.data);
      setPlayers((current) => [...current.filter((p) => p.uuid !== updated.uuid), updated]);
    });
    source.addEventListener('server', (event) => setServer(JSON.parse(event.data)));
    return () => source.close();
  }, []);

  return { players, server, error, live };
}
