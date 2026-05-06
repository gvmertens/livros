import { Link } from 'react-router-dom';
import type { Book } from '../types';

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
  return (
    <Link
      to={`/books/${book.id}`}
      style={{ textDecoration: 'none', color: 'inherit' }}
      aria-label={`View details for ${book.title}`}
    >
      <article
        style={{
          border: '1px solid #ddd',
          borderRadius: 8,
          padding: '16px 20px',
          cursor: 'pointer',
          transition: 'box-shadow 0.15s',
        }}
        onMouseEnter={(e) => {
          (e.currentTarget as HTMLElement).style.boxShadow = '0 2px 8px rgba(0,0,0,0.12)';
        }}
        onMouseLeave={(e) => {
          (e.currentTarget as HTMLElement).style.boxShadow = 'none';
        }}
      >
        <h3 style={{ margin: '0 0 6px', fontSize: 16 }}>{book.title}</h3>
        <p style={{ margin: '0 0 4px', fontSize: 14, opacity: 0.75 }}>
          by {book.author.name}
        </p>
        <p style={{ margin: 0, fontSize: 12, opacity: 0.55, fontFamily: 'monospace' }}>
          ISBN: {book.isbn}
        </p>
      </article>
    </Link>
  );
}
