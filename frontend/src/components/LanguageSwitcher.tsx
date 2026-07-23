import { useTranslation } from 'react-i18next';
import { theme } from '../theme';

/**
 * Inline "PT | EN" language toggle.
 *
 * - Active language is highlighted in brand orange (#E07020) with font-weight 600.
 * - Each button has aria-pressed reflecting whether it is the active locale.
 * - Clicking a language calls i18n.changeLanguage() directly — no dropdown.
 * - Preference is persisted to localStorage via i18next-browser-languagedetector
 *   (key: i18n_language), so it is restored on the next session automatically.
 *
 * Requirements: 13.3, 13.4, 13.7, 13.8, 13.9
 */
export default function LanguageSwitcher() {
  const { i18n } = useTranslation();

  const activeLocale = i18n.language;

  function handleChange(locale: string) {
    if (locale !== activeLocale) {
      i18n.changeLanguage(locale);
    }
  }

  const baseButtonStyle: React.CSSProperties = {
    background: 'none',
    border: 'none',
    cursor: 'pointer',
    padding: `2px ${theme.spacing.xs}px`,
    fontSize: theme.fontSizes.sm,
    lineHeight: 1,
    transition: 'color 0.15s',
  };

  const activeStyle: React.CSSProperties = {
    color: theme.colors.primary,
    fontWeight: 600,
  };

  const inactiveStyle: React.CSSProperties = {
    color: theme.colors.neutral400,
    fontWeight: 400,
  };

  return (
    <span
      style={{ display: 'inline-flex', alignItems: 'center', gap: 0 }}
      aria-label="Language switcher"
    >
      <button
        type="button"
        style={{
          ...baseButtonStyle,
          ...(activeLocale === 'pt-BR' ? activeStyle : inactiveStyle),
        }}
        aria-pressed={activeLocale === 'pt-BR'}
        onClick={() => handleChange('pt-BR')}
      >
        PT
      </button>

      <span
        style={{
          color: theme.colors.neutral600,
          fontSize: theme.fontSizes.sm,
          userSelect: 'none',
          padding: `0 2px`,
        }}
        aria-hidden="true"
      >
        |
      </span>

      <button
        type="button"
        style={{
          ...baseButtonStyle,
          ...(activeLocale === 'en-US' ? activeStyle : inactiveStyle),
        }}
        aria-pressed={activeLocale === 'en-US'}
        onClick={() => handleChange('en-US')}
      >
        EN
      </button>
    </span>
  );
}
