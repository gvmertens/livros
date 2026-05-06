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

  return (
    <nav aria-label="Pagination" style={{ display: 'flex', gap: 4, alignItems: 'center', flexWrap: 'wrap' }}>
      <button
        onClick={() => onPageChange(page - 1)}
        disabled={page === 0}
        aria-label="Previous page"
        style={{ padding: '4px 10px' }}
      >
        ‹
      </button>

      {pages.map((p, idx) => {
        if (p === 'ellipsis-start' || p === 'ellipsis-end') {
          return (
            <span key={`${p}-${idx}`} style={{ padding: '4px 6px', opacity: 0.5 }}>
              …
            </span>
          );
        }
        return (
          <button
            key={p}
            onClick={() => onPageChange(p)}
            disabled={p === page}
            aria-label={`Page ${p + 1}`}
            aria-current={p === page ? 'page' : undefined}
            style={{
              padding: '4px 10px',
              fontWeight: p === page ? 700 : 400,
              textDecoration: p === page ? 'underline' : 'none',
            }}
          >
            {p + 1}
          </button>
        );
      })}

      <button
        onClick={() => onPageChange(page + 1)}
        disabled={page >= totalPages - 1}
        aria-label="Next page"
        style={{ padding: '4px 10px' }}
      >
        ›
      </button>
    </nav>
  );
}
