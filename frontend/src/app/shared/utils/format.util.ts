const CURRENCY_FORMATTER = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const COMPACT_CURRENCY_FORMATTER = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
  notation: 'compact',
  maximumFractionDigits: 1
});

export function formatCurrencyBRL(value: number | null | undefined, fallback = '-'): string {
  if (value === null || value === undefined) {
    return fallback;
  }
  return CURRENCY_FORMATTER.format(value);
}

export function formatCompactCurrencyBRL(value: number | null | undefined, fallback = '-'): string {
  if (value === null || value === undefined) {
    return fallback;
  }
  return COMPACT_CURRENCY_FORMATTER.format(value);
}

export function formatIsoDate(iso: string | null | undefined, fallback = '-'): string {
  if (!iso) {
    return fallback;
  }
  const [year, month, day] = iso.slice(0, 10).split('-');
  return `${day}/${month}/${year}`;
}

export function formatInstant(instant: string | null | undefined, fallback = '-'): string {
  if (!instant) {
    return fallback;
  }
  const parsed = new Date(instant);
  if (Number.isNaN(parsed.getTime())) {
    return fallback;
  }
  return parsed.toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
}

export function initialsOf(name: string | null | undefined): string {
  const parts = (name ?? '')
    .trim()
    .split(/\s+/)
    .filter((part) => part.length > 0);
  if (parts.length === 0) {
    return '?';
  }
  if (parts.length === 1) {
    return parts[0].slice(0, 2).toUpperCase();
  }
  return `${parts[0][0]}${parts[parts.length - 1][0]}`.toUpperCase();
}
