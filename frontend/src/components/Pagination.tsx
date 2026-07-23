import { useTranslation } from 'react-i18next';
import { theme } from '../theme';

interface Props {
  /** Zero-based current page index. */
  page: number;
  /** Total number of pages. */
  totalPages: number;
  /** Called when the user navigates to a different page. */
  onPageChange: (page: number) => void;
}

/**
 * Simple pagination control.
 *
 * Renders Previous / page-number buttons / Next.
 * Emits `onPageChange` with the new zero-based page index.
 *
 * Requirements: 11.1
 */
export default function Pagination({ page, totalPages, onPageChange }: Props) {
  const { t } = useTranslation();

  if (totalPages <= 1) return null;

  // Build a compact window: always show first, last, current ±1, with ellipsis
  function buildPages(): (number | 'ellipsis-start' | 'ellipsis-end')[] {
    if (totalPages <= 7) {
      return Array.from({ length: totalPages }, (_, i) => i);
    }
    const pages: (number | 'ellipsis-start' | 'ellipsis-end')[] = [0];
    if (page > 2) pages.push('ellipsis-start');
    for (let i = Math.max(1, page - 1); i <= Math.min(totalPages - 2, page + 1); i++) {
      pages.push(i);
    }
    if (page < totalPages - 3) pages.push('ellipsis-end');
    pages.push(totalPages - 1);
    return pages;
  }

  const pages = buildPages();

  const baseBtn: React.CSSProperties = {
    background: 'transparent',
    border: `1px solid ${theme.colors.neutral800}`,
    color: theme.colors.neutral400,
    borderRadius: 6,
    padding: '4px 10px',
    fontSize: theme.fontSizes.sm,
    cursor: 'pointer',
    transition: 'border-color 0.15s, color 0.15s',
  };

  const activeBtn: React.CSSProperties = {
    ...baseBtn,
    background: theme.colors.primary,
    borderColor: theme.colors.primary,
    color: theme.colors.white,
    fontWeight: 700,
    cursor: 'default',
  };

  const disabledBtn: React.CSSProperties = {
    ...baseBtn,
    opacity: 0.3,
    cursor: 'not-allowed',
  };

  function hoverOn(e: React.MouseEvent<HTMLButtonElement>) {
    const btn = e.currentTarget;
    if (!btn.disabled) {
      btn.style.borderColor = theme.colors.primary;
      btn.style.color = theme.colors.primary;
    }
  }

  function hoverOff(e: React.MouseEvent<HTMLButtonElement>) {
    const btn = e.currentTarget;
    if (!btn.disabled) {
      btn.style.borderColor = theme.colors.neutral800;
      btn.style.color = theme.colors.neutral400;
    }
  }

  return (
    <nav aria-label={t('common.pagination.nav')} style={{ display: 'flex', gap: 4, alignItems: 'center', flexWrap: 'wrap' }}>
      <button
        onClick={() => onPageChange(page - 1)}
        disabled={page === 0}
        aria-label={t('common.pagination.previous')}
        style={page === 0 ? disabledBtn : baseBtn}
        onMouseEnter={hoverOn}
        onMouseLeave={hoverOff}
      >
        ‹
      </button>

      {pages.map((p, idx) => {
        if (p === 'ellipsis-start' || p === 'ellipsis-end') {
          return (
            <span
              key={`${p}-${idx}`}
              style={{ padding: '4px 6px', color: theme.colors.neutral600, fontSize: theme.fontSizes.sm }}
            >
              …
            </span>
          );
        }
        const isActive = p === page;
        return (
          <button
            key={p}
            onClick={() => onPageChange(p)}
            disabled={isActive}
            aria-label={t('common.pagination.page', { number: p + 1 })}
            aria-current={isActive ? 'page' : undefined}
            style={isActive ? activeBtn : baseBtn}
            onMouseEnter={isActive ? undefined : hoverOn}
            onMouseLeave={isActive ? undefined : hoverOff}
          >
            {p + 1}
          </button>
        );
      })}

      <button
        onClick={() => onPageChange(page + 1)}
        disabled={page >= totalPages - 1}
        aria-label={t('common.pagination.next')}
        style={page >= totalPages - 1 ? disabledBtn : baseBtn}
        onMouseEnter={hoverOn}
        onMouseLeave={hoverOff}
      >
        ›
      </button>
    </nav>
  );
}
