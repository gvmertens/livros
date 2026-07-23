import { theme } from '../theme';

interface Props {
  /** 'horizontal' shows the book icon + "GMLib" text side by side.
   *  'icon' shows only the book-stack SVG symbol. */
  variant: 'horizontal' | 'icon';
  /** Controls the height of the SVG icon in pixels. Defaults to 32. */
  size?: number;
}

/**
 * GMLib brand logo.
 *
 * The SVG depicts three stacked rectangles (books on a shelf) in orange —
 * geometric and flat, inspired by a side-on bookshelf view.
 */
export default function Logo({ variant, size = 32 }: Props) {
  const iconSize = size;
  // Keep the text proportional to the icon
  const textSize = Math.round(iconSize * 0.65);

  const bookIcon = (
    <svg
      width={iconSize}
      height={iconSize}
      viewBox="0 0 32 32"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
      focusable="false"
    >
      {/* Bottom book — widest */}
      <rect x="2"  y="22" width="28" height="6" rx="1.5" fill={theme.colors.primary} />
      {/* Middle book */}
      <rect x="5"  y="13" width="22" height="6" rx="1.5" fill={theme.colors.primaryLight} />
      {/* Top book — narrowest */}
      <rect x="9"  y="4"  width="14" height="6" rx="1.5" fill={theme.colors.primaryDark} />
    </svg>
  );

  if (variant === 'icon') {
    return bookIcon;
  }

  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: Math.round(iconSize * 0.3),
        textDecoration: 'none',
        userSelect: 'none',
      }}
      aria-label="GMLib — Personal Library Manager"
    >
      {bookIcon}
      <span
        style={{
          fontSize: textSize,
          fontWeight: 700,
          lineHeight: 1,
          letterSpacing: '-0.02em',
        }}
      >
        <span style={{ color: theme.colors.primary }}>GM</span>
        <span style={{ color: theme.colors.white }}>Lib</span>
      </span>
    </span>
  );
}
