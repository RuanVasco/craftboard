export interface PlayerStats {
  uuid: string;
  name: string;
  deaths: number;
  playTimeTicks: number;
  updatedAt: string;
}

export interface ServerInfo {
  address: string;
  motd: string;
  maxPlayers: number;
  rconEnabled: boolean;
  online: boolean | null;
  onlinePlayers: string[] | null;
}

async function get<T>(path: string): Promise<T> {
  const response = await fetch(path);
  if (!response.ok) {
    throw new Error(`${path} returned ${response.status}`);
  }
  return response.json() as Promise<T>;
}

export const fetchPlayers = () => get<PlayerStats[]>('/api/players');
export const fetchServer = () => get<ServerInfo>('/api/server');
export const STREAM_URL = '/api/stream';
