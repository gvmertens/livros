import { useTranslation } from 'react-i18next';
import type { ReadingStatus } from '../types';

interface Props {
  status: ReadingStatus;
}

const STATUS_COLORS: Record<ReadingStatus, { bg: string; color: string }> = {
  WANT_TO_READ: { bg: '#1A2A3A', color: '#5B9BD5' },
  READING:      { bg: '#2A2010', color: '#E07020' },
  FINISHED:     { bg: '#0F2A1A', color: '#27AE60' },
  ABANDONED:    { bg: '#2A1010', color: '#C0392B' },
};

/**
 * Colored badge displaying a reading status.
 * Requirements: 8.3
 */
export default function ReadingStatusBadge({ status }: Props) {
  const { t } = useTranslation();

  const colors = STATUS_COLORS[status] ?? { bg: '#1A1A1A', color: '#8A8A8A' };
  const label = t(`readings.statusLabels.${status}`, { defaultValue: status });

  return (
    <span
      style={{
        display: 'inline-block',
        padding: '2px 10px',
        borderRadius: 12,
        fontSize: 12,
        fontWeight: 600,
        backgroundColor: colors.bg,
        color: colors.color,
        whiteSpace: 'nowrap',
      }}
      aria-label={`Reading status: ${label}`}
    >
      {label}
    </span>
  );
}
