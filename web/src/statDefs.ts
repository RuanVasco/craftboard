import {
  ArrowBigUp,
  BedDouble,
  Crosshair,
  Droplets,
  Fish,
  Footprints,
  Gem,
  Handshake,
  HeartPulse,
  Hourglass,
  LogIn,
  Mountain,
  Package,
  Pickaxe,
  Plane,
  Rabbit,
  Route,
  Sailboat,
  Skull,
  Sparkles,
  Sword,
  Swords,
  Timer,
  type LucideIcon,
} from 'lucide-react';
import { formatDistance, formatPlayTime, formatSeconds, numberFormat } from './format';

export interface StatDef {
  label: string;
  icon: LucideIcon;
  format: (value: number) => string;
}

const count = (value: number) => numberFormat.format(value);
// Damage stats are stored in tenths of a health point, and a heart is two points.
const hearts = (value: number) => `${numberFormat.format(Math.round(value / 20))} corações`;

/** Keys match the record keys the API sends, which are stat keys plus a few derived ones. */
export const STAT_DEFS: Record<string, StatDef> = {
  distance: { label: 'Distância percorrida', icon: Route, format: formatDistance },
  blocks_mined: { label: 'Blocos minerados', icon: Pickaxe, format: count },
  diamonds: { label: 'Diamantes minerados', icon: Gem, format: count },
  ancient_debris: { label: 'Detritos antigos', icon: Mountain, format: count },
  deaths: { label: 'Mortes', icon: Skull, format: count },
  time_since_death: { label: 'Vivo há mais tempo', icon: Hourglass, format: formatPlayTime },
  mob_kills: { label: 'Mobs abatidos', icon: Sword, format: count },
  player_kills: { label: 'Jogadores abatidos', icon: Swords, format: count },
  jump: { label: 'Pulos', icon: ArrowBigUp, format: count },
  damage_dealt: { label: 'Dano causado', icon: Crosshair, format: hearts },
  damage_taken: { label: 'Dano sofrido', icon: HeartPulse, format: hearts },
  fish_caught: { label: 'Peixes pescados', icon: Fish, format: count },
  animals_bred: { label: 'Animais criados', icon: Rabbit, format: count },
  traded_with_villager: { label: 'Trocas com aldeões', icon: Handshake, format: count },
  enchant_item: { label: 'Itens encantados', icon: Sparkles, format: count },
  sleep_in_bed: { label: 'Noites dormidas', icon: BedDouble, format: count },
  aviate_one_cm: { label: 'Voo de elytra', icon: Plane, format: formatDistance },
  boat_one_cm: { label: 'De barco', icon: Sailboat, format: formatDistance },
  horse_one_cm: { label: 'A cavalo', icon: Footprints, format: formatDistance },
  swim_one_cm: { label: 'Nadando', icon: Droplets, format: formatDistance },
  open_chest: { label: 'Baús abertos', icon: Package, format: count },
  leave_game: { label: 'Logins', icon: LogIn, format: count },
  longest_session: { label: 'Sess�o mais longa', icon: Timer, format: formatSeconds },
};
