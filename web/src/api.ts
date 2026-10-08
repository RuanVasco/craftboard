export interface PlayerStats {
  uuid: string;
  name: string;
  deaths: number;
  playTimeTicks: number;
  updatedAt: string;
}

export type Period = 'week' | 'month' | 'all';

export interface SnapshotPoint {
  capturedAt: string;
  deaths: number;
  playTimeTicks: number;
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

export const fetchPlayers = (period: Period) => get<PlayerStats[]>(`/api/players?period=${period}`);
export const fetchHistory = (uuid: string) => get<SnapshotPoint[]>(`/api/players/${uuid}/history`);
export const fetchServer = () => get<ServerInfo>('/api/server');
export const STREAM_URL = '/api/stream';
