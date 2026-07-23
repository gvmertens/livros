/**
 * GMLib design tokens — single source of truth for all visual styles.
 * Use these values in inline style objects throughout the app.
 */
export const theme = {
  colors: {
    // Primary orange
    primary:       '#E07020',
    primaryDark:   '#C45C10',
    primaryLight:  '#F08030',

    // Neutrals (dark → light)
    black:         '#0D0D0D',
    neutral900:    '#1A1A1A',
    neutral800:    '#2C2C2C',
    neutral600:    '#4A4A4A',
    neutral400:    '#8A8A8A',
    neutral200:    '#D0D0D0',
    neutral100:    '#F0F0F0',
    white:         '#FFFFFF',

    // Semantic
    danger:        '#C0392B',
    success:       '#27AE60',
  },

  spacing: {
    xs:   4,
    sm:   8,
    md:  12,
    base: 16,
    lg:  20,
    xl:  24,
    xxl: 32,
    '3xl': 40,
    '4xl': 48,
    '5xl': 64,
  },

  radius: {
    sm:  4,
    md:  8,
    lg: 12,
  },

  shadows: {
    card:  '0 2px 8px rgba(0,0,0,0.4)',
    focus: '0 0 0 3px rgba(224,112,32,0.4)',
  },

  fontSizes: {
    xs:  12,
    sm:  13,
    md:  15,
    lg:  18,
    xl:  24,
    xxl: 32,
  },
} as const;

export type Theme = typeof theme;
