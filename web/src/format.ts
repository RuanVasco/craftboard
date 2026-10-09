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

export function formatSeconds(seconds: number): string {
  return formatPlayTime(seconds * 20);
}

export function formatDistance(cm: number): string {
  const km = cm / 100_000;
  if (km >= 1000) return `${compactFormat.format(km)} km`;
  return km >= 1 ? `${decimalFormat.format(km)} km` : `${numberFormat.format(Math.round(cm / 100))} m`;
}

export function formatHours(hours: number): string {
  return `${decimalFormat.format(hours)}h`;
}

// Day ticks start at sunrise (06:00), so 0 maps to six in the morning.
export function formatTimeOfDay(ticks: number): string {
  const totalMinutes = Math.floor(((ticks / 1000 + 6) % 24) * 60);
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`;
}

export function isNight(ticks: number): boolean {
  return ticks >= 13000 && ticks < 23000;
}

const relative = new Intl.RelativeTimeFormat('pt-BR', { numeric: 'auto' });
const RELATIVE_STEPS: [Intl.RelativeTimeFormatUnit, number][] = [
  ['second', 60],
  ['minute', 60],
  ['hour', 24],
  ['day', 30],
  ['month', 12],
  ['year', Infinity],
];

export function formatRelative(iso: string, now = Date.now()): string {
  let value = (Date.parse(iso) - now) / 1000;
  for (const [unit, size] of RELATIVE_STEPS) {
    if (Math.abs(value) < size) return relative.format(Math.round(value), unit);
    value /= size;
  }
  return dateTimeFormat.format(Date.parse(iso));
}

export const numberFormat = new Intl.NumberFormat('pt-BR');

export const decimalFormat = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1 });

export const compactFormat = new Intl.NumberFormat('pt-BR', { notation: 'compact', maximumFractionDigits: 1 });

export const dateTimeFormat = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });

export const dateFormat = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit' });

export const timeFormat = new Intl.DateTimeFormat('pt-BR', { timeStyle: 'short' });

// Splits "129,6 mil" into number and suffix so tiles can render the suffix smaller.
export function compactParts(value: number): { number: string; suffix: string } {
  const parts = compactFormat.formatToParts(value);
  const suffix = parts.filter((p) => p.type === 'compact').map((p) => p.value).join('');
  const number = parts.filter((p) => p.type !== 'compact').map((p) => p.value).join('').trim();
  return { number, suffix };
}
