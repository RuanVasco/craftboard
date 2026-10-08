import { LogIn, LogOut, Power, PowerOff, Radio, Skull, Trophy, type LucideIcon } from 'lucide-react';
import type { FeedEvent, FeedEventType } from '../api';
import { formatRelative, timeFormat } from '../format';
import { Section } from './Section';

const STYLE: Record<FeedEventType, { icon: LucideIcon; tone: string }> = {
  JOIN: { icon: LogIn, tone: 'text-good' },
  LEAVE: { icon: LogOut, tone: 'text-ink-muted' },
  ADVANCEMENT: { icon: Trophy, tone: 'text-primary' },
  DEATH: { icon: Skull, tone: 'text-bad' },
  SERVER_START: { icon: Power, tone: 'text-good' },
  SERVER_STOP: { icon: PowerOff, tone: 'text-ink-muted' },
};

function describe(event: FeedEvent): string {
  switch (event.type) {
    case 'JOIN':
      return `${event.player} entrou no servidor`;
    case 'LEAVE':
      return `${event.player} saiu do servidor`;
    case 'ADVANCEMENT':
      return `${event.player} conquistou [${event.detail}]`;
    case 'SERVER_START':
      return 'Servidor iniciado';
    case 'SERVER_STOP':
      return 'Servidor desligado';
    // Death messages stay as the game wrote them, they are the fun part.
    case 'DEATH':
      return event.message;
  }
}

export function FeedSection({ events }: { events: FeedEvent[] }) {
  return (
    <Section title="Acontecendo agora" icon={Radio} subtitle="Desde o último restart do servidor">
      {events.length === 0 ? (
        <p className="py-6 text-center text-sm text-ink-muted">Nada por aqui ainda.</p>
      ) : (
        <ol className="flex max-h-96 flex-col gap-2.5 overflow-y-auto pr-1 text-sm">
          {events.map((event, index) => {
            const { icon: Icon, tone } = STYLE[event.type];
            return (
              <li key={`${event.at}-${index}`} className="flex items-start gap-2.5">
                <Icon className={`mt-0.5 size-4 shrink-0 ${tone}`} aria-hidden />
                <span className="min-w-0 flex-1">{describe(event)}</span>
                <time
                  dateTime={event.at}
                  title={timeFormat.format(Date.parse(event.at))}
                  className="shrink-0 text-xs text-ink-muted"
                >
                  {formatRelative(event.at)}
                </time>
              </li>
            );
          })}
        </ol>
      )}
    </Section>
  );
}
