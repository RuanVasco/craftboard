import { useEffect, useState } from 'react';
import { fetchPlayers, fetchServer, STREAM_URL, type PlayerStats, type ServerInfo } from './api';

export function useLiveData() {
  const [players, setPlayers] = useState<PlayerStats[]>([]);
  const [server, setServer] = useState<ServerInfo | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [live, setLive] = useState(false);

  useEffect(() => {
    Promise.all([fetchPlayers(), fetchServer()])
      .then(([playerList, serverInfo]) => {
        setPlayers(playerList);
        setServer(serverInfo);
      })
      .catch((e: Error) => setError(e.message));

    // EventSource reconnects on its own, so only the indicator needs handling.
    const source = new EventSource(STREAM_URL);
    source.onopen = () => setLive(true);
    source.onerror = () => setLive(false);
    source.addEventListener('player', (event) => {
      const updated: PlayerStats = JSON.parse(event.data);
      setPlayers((current) => [...current.filter((p) => p.uuid !== updated.uuid), updated]);
    });
    source.addEventListener('server', (event) => setServer(JSON.parse(event.data)));
    return () => source.close();
  }, []);

  return { players, server, error, live };
}
