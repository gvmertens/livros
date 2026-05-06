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
 * In edit mode: renders a range slider (step 0.5) with a numeric display
 * and a "Clear" button to remove the rating.
 *
 * In read-only mode: renders the numeric value with a /10 suffix.
 *
 * Requirements: 8.3, 8.9
 */
export default function StarRating({ value, onChange, readOnly = false }: Props) {
  if (readOnly) {
    return (
      <span aria-label={`Rating: ${value !== null ? `${value} out of 10` : 'not rated'}`}>
        {value !== null ? (
          <>
            <strong>{value}</strong>
            <span style={{ opacity: 0.5, fontSize: 13 }}> / 10</span>
          </>
        ) : (
          <span style={{ opacity: 0.5, fontSize: 13 }}>Not rated</span>
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
        step={0.5}
        value={displayValue}
        onChange={(e) => onChange?.(parseFloat(e.target.value))}
        style={{ width: 200 }}
        aria-label="Rating (0 to 10)"
        aria-valuemin={0}
        aria-valuemax={10}
        aria-valuenow={displayValue}
        aria-valuetext={`${displayValue} out of 10`}
      />
      <span style={{ minWidth: 36, fontWeight: 600 }}>
        {displayValue.toFixed(1)}
      </span>
      <span style={{ opacity: 0.5, fontSize: 13 }}>/ 10</span>
      {value !== null && (
        <button
          type="button"
          onClick={() => onChange?.(null)}
          style={{ fontSize: 12, padding: '2px 8px', opacity: 0.7 }}
          aria-label="Clear rating"
        >
          Clear
        </button>
      )}
    </div>
  );
}
