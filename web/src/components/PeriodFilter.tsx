import type { Period } from '../api';

const OPTIONS: { value: Period; label: string }[] = [
  { value: 'week', label: 'Semana' },
  { value: 'month', label: 'Mês' },
  { value: 'all', label: 'Total' },
];

interface Props {
  value: Period;
  onChange: (period: Period) => void;
}

export function PeriodFilter({ value, onChange }: Props) {
  return (
    <div role="radiogroup" aria-label="Período" className="flex w-fit gap-1 rounded-lg border border-line bg-surface p-1">
      {OPTIONS.map((option) => {
        const active = option.value === value;
        return (
          <button
            key={option.value}
            type="button"
            role="radio"
            aria-checked={active}
            onClick={() => onChange(option.value)}
            className={`rounded-md px-3 py-1.5 text-sm font-medium transition ${
              active ? 'bg-primary text-surface' : 'text-ink-muted hover:text-ink'
            }`}
          >
            {option.label}
          </button>
        );
      })}
    </div>
  );
}
