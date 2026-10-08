const TICKS_PER_HOUR = 20 * 60 * 60;
const TICKS_PER_MINUTE = 20 * 60;

export function formatPlayTime(ticks: number): string {
  const hours = Math.floor(ticks / TICKS_PER_HOUR);
  const minutes = Math.floor((ticks % TICKS_PER_HOUR) / TICKS_PER_MINUTE);
  return hours > 0 ? `${hours}h ${minutes}m` : `${minutes}m`;
}

export function ticksToHours(ticks: number): number {
  return Math.floor(ticks / TICKS_PER_HOUR);
}

export function ticksToHoursPrecise(ticks: number): number {
  return ticks / TICKS_PER_HOUR;
}

export const numberFormat = new Intl.NumberFormat('pt-BR');

export const dateTimeFormat = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
