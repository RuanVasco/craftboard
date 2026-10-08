import { Trophy } from 'lucide-react';
import type { PlayerAdvancement } from '../api';
import { dateTimeFormat, formatRelative } from '../format';
import { ADVANCEMENT_CATEGORIES, advancementTitle } from '../names';
import { Avatar } from './Avatar';
import { Section } from './Section';

export function RecentAdvancements({ advancements }: { advancements: PlayerAdvancement[] }) {
  return (
    <Section title="Conquistas recentes" icon={Trophy}>
      {advancements.length === 0 ? (
        <p className="py-6 text-center text-sm text-ink-muted">Nenhuma conquista ainda.</p>
      ) : (
        <ol className="flex max-h-96 flex-col gap-3 overflow-y-auto pr-1 text-sm">
          {advancements.map((a) => (
            <li key={`${a.uuid}-${a.id}`} className="flex items-center gap-2.5">
              <Avatar name={a.name} size={24} />
              <div className="min-w-0 flex-1">
                <p className="truncate">
                  <span className="font-medium">{a.name}</span> conquistou{' '}
                  <span className="font-medium text-primary">{advancementTitle(a.id)}</span>
                </p>
                <p className="text-xs text-ink-muted">{ADVANCEMENT_CATEGORIES[a.category] ?? a.category}</p>
              </div>
              <time
                dateTime={a.doneAt}
                title={dateTimeFormat.format(Date.parse(a.doneAt))}
                className="shrink-0 text-xs text-ink-muted"
              >
                {formatRelative(a.doneAt)}
              </time>
            </li>
          ))}
        </ol>
      )}
    </Section>
  );
}
