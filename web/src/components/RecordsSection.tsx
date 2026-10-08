import { Award } from 'lucide-react';
import type { StatRecord } from '../api';
import { STAT_DEFS } from '../statDefs';
import { Avatar } from './Avatar';
import { Section } from './Section';

interface Props {
  records: StatRecord[];
  onSelect: (uuid: string, name: string) => void;
}

export function RecordsSection({ records, onSelect }: Props) {
  const known = records.filter((r) => STAT_DEFS[r.key]);
  return (
    <Section title="Recordes do servidor" icon={Award} subtitle="Quem lidera cada estatística">
      {known.length === 0 ? (
        <p className="py-6 text-center text-sm text-ink-muted">Ninguém marcou ponto ainda.</p>
      ) : (
        <ul className="grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-3">
          {known.map((record) => {
            const def = STAT_DEFS[record.key];
            const Icon = def.icon;
            return (
              <li key={record.key}>
                <button
                  type="button"
                  onClick={() => onSelect(record.uuid, record.name)}
                  className="flex w-full items-center gap-3 rounded-lg border border-line p-3 text-left transition hover:border-primary"
                >
                  <span className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary-soft text-primary">
                    <Icon className="size-4" aria-hidden />
                  </span>
                  <span className="min-w-0 flex-1">
                    <span className="block truncate text-xs text-ink-muted">{def.label}</span>
                    <span className="flex items-center gap-1.5">
                      <Avatar name={record.name} size={18} />
                      <span className="truncate text-sm font-medium">{record.name}</span>
                    </span>
                  </span>
                  <span className="shrink-0 text-sm font-semibold tabular-nums">{def.format(record.value)}</span>
                </button>
              </li>
            );
          })}
        </ul>
      )}
    </Section>
  );
}
