const DARK_TEXT = '#111827';
const LIGHT_TEXT = '#ffffff';
const LUMINANCE_THRESHOLD = 0.6;

function normalizeHex(color: string): string | null {
  const value = color.trim().replace('#', '');
  if (value.length === 3) {
    return value
      .split('')
      .map((channel) => channel + channel)
      .join('');
  }
  return value.length === 6 ? value : null;
}

export function contrastTextFor(color: string | null | undefined): string {
  if (!color) {
    return LIGHT_TEXT;
  }
  const hex = normalizeHex(color);
  if (!hex) {
    return LIGHT_TEXT;
  }
  const red = parseInt(hex.slice(0, 2), 16) / 255;
  const green = parseInt(hex.slice(2, 4), 16) / 255;
  const blue = parseInt(hex.slice(4, 6), 16) / 255;
  const luminance = 0.299 * red + 0.587 * green + 0.114 * blue;
  return luminance > LUMINANCE_THRESHOLD ? DARK_TEXT : LIGHT_TEXT;
}

export function domainChipStyle(color: string | null | undefined): Record<string, string> | null {
  if (!color) {
    return null;
  }
  return { background: color, color: contrastTextFor(color) };
}
