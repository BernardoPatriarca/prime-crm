import { formatCompactCurrencyBRL, formatCurrencyBRL, formatInstant, formatIsoDate, initialsOf } from './format.util';

describe('formatCurrencyBRL', () => {
  it('formats a number as BRL currency', () => {
    expect(formatCurrencyBRL(1000)).toContain('1.000,00');
  });

  it('falls back on null', () => {
    expect(formatCurrencyBRL(null)).toBe('-');
  });

  it('falls back on undefined (field omitted by the API)', () => {
    expect(formatCurrencyBRL(undefined)).toBe('-');
  });

  it('respects a custom fallback', () => {
    expect(formatCurrencyBRL(null, 'sem valor')).toBe('sem valor');
  });

  it('formats zero as currency instead of falling back', () => {
    expect(formatCurrencyBRL(0)).toContain('0,00');
  });
});

describe('formatCompactCurrencyBRL', () => {
  it('formats a large number compactly', () => {
    expect(formatCompactCurrencyBRL(1500)).toContain('mil');
  });

  it('falls back on null', () => {
    expect(formatCompactCurrencyBRL(null)).toBe('-');
  });

  it('falls back on undefined', () => {
    expect(formatCompactCurrencyBRL(undefined)).toBe('-');
  });
});

describe('formatIsoDate', () => {
  it('formats an ISO date as dd/mm/yyyy', () => {
    expect(formatIsoDate('2026-09-16')).toBe('16/09/2026');
  });

  it('falls back on null', () => {
    expect(formatIsoDate(null)).toBe('-');
  });

  it('falls back on undefined', () => {
    expect(formatIsoDate(undefined)).toBe('-');
  });

  it('falls back on empty string', () => {
    expect(formatIsoDate('')).toBe('-');
  });
});

describe('formatInstant', () => {
  it('formats an ISO instant as a localized date/time', () => {
    const result = formatInstant('2026-09-16T10:30:00Z');
    expect(result).toContain('2026');
  });

  it('falls back on null', () => {
    expect(formatInstant(null)).toBe('-');
  });

  it('falls back on undefined', () => {
    expect(formatInstant(undefined)).toBe('-');
  });

  it('falls back on an invalid date string', () => {
    expect(formatInstant('not-a-date')).toBe('-');
  });
});

describe('initialsOf', () => {
  it('returns the first two letters for a single name', () => {
    expect(initialsOf('Cliente')).toBe('CL');
  });

  it('returns first and last initials for a full name', () => {
    expect(initialsOf('Joao Silva')).toBe('JS');
  });

  it('falls back to ? on null', () => {
    expect(initialsOf(null)).toBe('?');
  });

  it('falls back to ? on undefined', () => {
    expect(initialsOf(undefined)).toBe('?');
  });

  it('falls back to ? on blank string', () => {
    expect(initialsOf('   ')).toBe('?');
  });
});
