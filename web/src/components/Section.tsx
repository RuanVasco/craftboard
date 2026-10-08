import type { LucideIcon } from 'lucide-react';
import type { ReactNode } from 'react';

interface Props {
  title: string;
  icon: LucideIcon;
  subtitle?: string;
  children: ReactNode;
  className?: string;
}

export function Section({ title, icon: Icon, subtitle, children, className = '' }: Props) {
  return (
    <section className={`flex min-w-0 flex-col gap-3 rounded-xl border border-line bg-surface p-4 ${className}`}>
      <header className="flex flex-wrap items-baseline justify-between gap-x-3 gap-y-1">
        <h2 className="flex items-center gap-2 font-semibold">
          <Icon className="size-4 text-primary" aria-hidden />
          {title}
        </h2>
        {subtitle && <p className="text-xs text-ink-muted">{subtitle}</p>}
      </header>
      {children}
    </section>
  );
}
