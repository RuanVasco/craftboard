export interface PlayerStats {
  uuid: string;
  name: string;
  deaths: number;
  playTimeTicks: number;
  mobKills: number;
  playerKills: number;
  distanceCm: number;
  blocksMined: number;
  updatedAt: string;
}

export type Period = 'week' | 'month' | 'all';

export interface SnapshotPoint {
  capturedAt: string;
  deaths: number;
  playTimeTicks: number;
  // Null on points captured before these counters were tracked.
  mobKills: number | null;
  playerKills: number | null;
  distanceCm: number | null;
  blocksMined: number | null;
}

export interface WorldInfo {
  version: string | null;
  day: number | null;
  timeOfDay: number | null;
  raining: boolean;
  thundering: boolean;
  capturedAt: string;
}

export interface ServerInfo {
  address: string;
  motd: string;
  maxPlayers: number;
  difficulty: string;
  gameMode: string;
  hardcore: boolean;
  pvp: boolean;
  rconEnabled: boolean;
  online: boolean | null;
  onlinePlayers: string[] | null;
  world: WorldInfo | null;
}

/** Every stat of a player, by category (custom, mined, killed, ...) and key, namespace dropped. */
export type StatCategories = Record<string, Record<string, number>>;

export interface Advancement {
  id: string;
  category: string;
  doneAt: string | null;
}

export interface PlayerAdvancement extends Advancement {
  uuid: string;
  name: string;
  doneAt: string;
}

export interface AdvancementsOverview {
  counts: Record<string, number>;
  recent: PlayerAdvancement[];
}

export interface ItemStack {
  id: string;
  count: number;
}

export interface PlayerState {
  xpLevel: number;
  health: number;
  foodLevel: number;
  dimension: string;
  gameMode: string;
  selectedItem: ItemStack | null;
  equipment: Partial<Record<'head' | 'chest' | 'legs' | 'feet' | 'offhand', ItemStack>>;
  inventory: ItemStack[];
  updatedAt: string;
}

export type FeedEventType = 'JOIN' | 'LEAVE' | 'ADVANCEMENT' | 'DEATH' | 'SERVER_START' | 'SERVER_STOP';

export interface FeedEvent {
  type: FeedEventType;
  player: string | null;
  uuid: string | null;
  detail: string | null;
  message: string;
  at: string;
}

export interface Activity {
  days: number;
  /** Seven rows, Monday first, of 24 hourly totals in hours. */
  heatmap: number[][];
  daily: { date: string; hours: number; players: number }[];
}

export interface PlayerSummary {
  firstSeen: string;
  lastSeen: string;
  sessions: number | null;
  longestSessionSeconds: number | null;
  averageSessionSeconds: number | null;
}

export interface StatRecord {
  key: string;
  uuid: string;
  name: string;
  value: number;
}

export interface Count {
  id: string;
  value: number;
}

export interface Overview {
  records: StatRecord[];
  topMined: Count[];
  topKilled: Count[];
  topKilledBy: Count[];
}

async function get<T>(path: string): Promise<T> {
  const response = await fetch(path);
  if (!response.ok) {
    throw new Error(`${path} returned ${response.status}`);
  }
  return response.json() as Promise<T>;
}

// Endpoints that answer 404 when the server files know nothing about the player.
async function getOptional<T>(path: string): Promise<T | null> {
  const response = await fetch(path);
  if (response.status === 404) return null;
  if (!response.ok) {
    throw new Error(`${path} returned ${response.status}`);
  }
  return response.json() as Promise<T>;
}

export const fetchPlayers = (period: Period) => get<PlayerStats[]>(`/api/players?period=${period}`);
export const fetchHistory = (uuid: string) => get<SnapshotPoint[]>(`/api/players/${uuid}/history`);
export const fetchPlayerStats = (uuid: string) => getOptional<StatCategories>(`/api/players/${uuid}/stats`);
export const fetchPlayerState = (uuid: string) => getOptional<PlayerState>(`/api/players/${uuid}/state`);
export const fetchPlayerSummary = (uuid: string) => getOptional<PlayerSummary>(`/api/players/${uuid}/summary`);
export const fetchPlayerAdvancements = (uuid: string) => get<Advancement[]>(`/api/players/${uuid}/advancements`);
export const fetchPlayerActivity = (uuid: string) => get<Activity>(`/api/players/${uuid}/activity`);
export const fetchServer = () => get<ServerInfo>('/api/server');
export const fetchOverview = () => get<Overview>('/api/overview');
export const fetchAdvancements = () => get<AdvancementsOverview>('/api/advancements?limit=12');
export const fetchFeed = () => get<FeedEvent[]>('/api/feed');
export const fetchActivity = () => get<Activity>('/api/activity');
export const STREAM_URL = '/api/stream';
