import type { ReadingStatus } from '../types';

interface Props {
  status: ReadingStatus;
}

const STATUS_CONFIG: Record<ReadingStatus, { label: string; bg: string; color: string }> = {
  WANT_TO_READ: { label: 'Want to Read', bg: '#e8f4fd', color: '#1565c0' },
  READING:      { label: 'Reading',       bg: '#fff8e1', color: '#f57f17' },
  FINISHED:     { label: 'Finished',      bg: '#e8f5e9', color: '#2e7d32' },
  ABANDONED:    { label: 'Abandoned',     bg: '#fce4ec', color: '#c62828' },
};

/**
 * Colored badge displaying a reading status.
 * Requirements: 8.3
 */
export default function ReadingStatusBadge({ status }: Props) {
  const config = STATUS_CONFIG[status] ?? { label: status, bg: '#f5f5f5', color: '#333' };

  return (
    <span
      style={{
        display: 'inline-block',
        padding: '2px 10px',
        borderRadius: 12,
        fontSize: 12,
        fontWeight: 600,
        backgroundColor: config.bg,
        color: config.color,
        whiteSpace: 'nowrap',
      }}
      aria-label={`Reading status: ${config.label}`}
    >
      {config.label}
    </span>
  );
}
