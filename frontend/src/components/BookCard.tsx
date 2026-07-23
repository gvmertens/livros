import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import type { Book } from '../types';
import { theme } from '../theme';

interface Props {
  book: Book;
}

/**
 * Card component displaying a book's title, author name, and ISBN.
 * Clicking the card navigates to the book detail page.
 *
 * Requirements: 7.5
 */
export default function BookCard({ book }: Props) {
  const { t } = useTranslation();

  return (
    <Link
      to={`/books/${book.id}`}
      style={{ textDecoration: 'none', color: 'inherit' }}
      aria-label={`View details for ${book.title}`}
    >
      <article
        style={{
          background: '#1E1E1E',
          border: `1px solid ${theme.colors.neutral800}`,
          borderRadius: theme.radius.md,
          padding: `${theme.spacing.base}px ${theme.spacing.lg}px`,
          cursor: 'pointer',
          transition: 'border-color 0.15s, box-shadow 0.15s',
        }}
        onMouseEnter={(e) => {
          const el = e.currentTarget as HTMLElement;
          el.style.borderColor = theme.colors.primary;
          el.style.boxShadow = `0 0 0 1px ${theme.colors.primary}`;
        }}
        onMouseLeave={(e) => {
          const el = e.currentTarget as HTMLElement;
          el.style.borderColor = theme.colors.neutral800;
          el.style.boxShadow = 'none';
        }}
      >
        <h3
          style={{
            margin: '0 0 6px',
            fontSize: theme.fontSizes.md,
            fontWeight: 600,
            color: theme.colors.white,
          }}
        >
          {book.title}
        </h3>
        <p
          style={{
            margin: '0 0 4px',
            fontSize: theme.fontSizes.sm,
            color: theme.colors.neutral400,
          }}
        >
          {t('books.by')} {book.author.name}
        </p>
        <p
          style={{
            margin: 0,
            fontSize: theme.fontSizes.xs,
            color: theme.colors.neutral600,
            fontFamily: 'monospace',
          }}
        >
          {t('books.isbn')}: {book.isbn}
        </p>
      </article>
    </Link>
  );
}
