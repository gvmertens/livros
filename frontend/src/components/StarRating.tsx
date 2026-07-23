import { useTranslation } from 'react-i18next';
import { theme } from '../theme';

interface Props {
  /** Current rating value (0.0–10.0), or null if unset. */
  value: number | null;
  /** Called when the user changes the rating. Pass null to clear. */
  onChange?: (value: number | null) => void;
  /** When true, renders as read-only display (no slider). */
  readOnly?: boolean;
}

/**
 * Rating component for the 0–10 scale.
 *
 * In edit mode: renders a range slider (step 0.1) with a numeric display
 * and a "Clear" button to remove the rating.
 *
 * In read-only mode: renders the numeric value with a /10 suffix.
 *
 * Requirements: 8.3, 8.9
 */
export default function StarRating({ value, onChange, readOnly = false }: Props) {
  const { t } = useTranslation();

  if (readOnly) {
    return (
      <span
        aria-label={
          value !== null
            ? t('common.rating.readOnlyAriaLabel', { value })
            : t('common.rating.readOnlyNotRated')
        }
      >
        {value !== null ? (
          <>
            <strong style={{ color: theme.colors.primary, fontWeight: 700 }}>{value}</strong>
            <span style={{ color: theme.colors.neutral600, fontSize: theme.fontSizes.sm }}>
              {' '}{t('common.rating.outOf')}
            </span>
          </>
        ) : (
          <span style={{ color: theme.colors.neutral600, fontSize: theme.fontSizes.sm }}>
            {t('common.rating.notRated')}
          </span>
        )}
      </span>
    );
  }

  const displayValue = value ?? 0;

  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
      <input
        type="range"
        min={0}
        max={10}
        step={0.1}
        value={displayValue}
        onChange={(e) => onChange?.(parseFloat(e.target.value))}
        style={{ width: 200, accentColor: theme.colors.primary }}
        aria-label={t('common.rating.ariaLabel')}
        aria-valuemin={0}
        aria-valuemax={10}
        aria-valuenow={displayValue}
        aria-valuetext={t('common.rating.ariaValueText', { value: displayValue })}
      />
      <span style={{ minWidth: 36, fontWeight: 600, color: theme.colors.primary }}>
        {displayValue.toFixed(1)}
      </span>
      <span style={{ color: theme.colors.neutral600, fontSize: theme.fontSizes.sm }}>
        {t('common.rating.outOf')}
      </span>
      {value !== null && (
        <button
          type="button"
          onClick={() => onChange?.(null)}
          style={{
            fontSize: 12,
            padding: '2px 8px',
            color: theme.colors.neutral600,
            background: 'transparent',
            border: `1px solid ${theme.colors.neutral800}`,
            borderRadius: theme.radius.sm,
            transition: 'color 0.15s, border-color 0.15s',
          }}
          onMouseEnter={(e) => {
            (e.currentTarget as HTMLElement).style.color = theme.colors.danger;
            (e.currentTarget as HTMLElement).style.borderColor = theme.colors.danger;
          }}
          onMouseLeave={(e) => {
            (e.currentTarget as HTMLElement).style.color = theme.colors.neutral600;
            (e.currentTarget as HTMLElement).style.borderColor = theme.colors.neutral800;
          }}
          aria-label={t('common.rating.clearAriaLabel')}
        >
          {t('common.rating.clear')}
        </button>
      )}
    </div>
  );
}
