import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getRecommendations } from '../api/recommendations';
import Navbar from '../components/Navbar';

/**
 * Recommendations page — displays personalised book suggestions from the
 * recommendation service (powered by OpenAI).
 *
 * - Shows a loading state while fetching
 * - Shows an empty state with a prompt to rate books when no history exists
 * - Lists recommendations as book cards when available
 *
 * Requirements: 10.4
 */
export default function RecommendationsPage() {
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['recommendations'],
    queryFn: getRecommendations,
    staleTime: 5 * 60 * 1000, // cache for 5 minutes
  });

  return (
    <div>
      <Navbar />

      <main style={{ maxWidth: 700, margin: '0 auto', padding: '24px 16px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 24 }}>
          <h1 style={{ margin: 0 }}>Recommendations</h1>
          <button
            onClick={() => refetch()}
            disabled={isLoading}
            style={{ padding: '6px 14px', fontSize: 13 }}
            aria-label="Refresh recommendations"
          >
            {isLoading ? 'Loading…' : '↻ Refresh'}
          </button>
        </div>

        {/* Loading */}
        {isLoading && (
          <p aria-live="polite" style={{ opacity: 0.6 }}>
            Generating recommendations…
          </p>
        )}

        {/* Error */}
        {isError && (
          <p role="alert" style={{ color: 'red' }}>
            Could not load recommendations. The recommendation service may be unavailable.
          </p>
        )}

        {/* Empty state */}
        {!isLoading && !isError && data?.recommendations.length === 0 && (
          <div style={{ textAlign: 'center', padding: '40px 0' }}>
            <p style={{ fontSize: 18, marginBottom: 8 }}>📚 No recommendations yet</p>
            <p style={{ opacity: 0.6, marginBottom: 20 }}>
              Rate and review books in your reading list to get personalised suggestions.
            </p>
            <Link to="/readings">
              <button style={{ padding: '10px 20px' }}>Go to My Readings</button>
            </Link>
          </div>
        )}

        {/* Recommendations list */}
        {data && data.recommendations.length > 0 && (
          <>
            <p style={{ opacity: 0.6, fontSize: 13, marginBottom: 20 }}>
              Based on your reading history — powered by AI
            </p>
            <ol style={{ paddingLeft: 0, listStyle: 'none', display: 'flex', flexDirection: 'column', gap: 12 }}>
              {data.recommendations.map((rec, idx) => (
                <li
                  key={idx}
                  style={{
                    display: 'flex',
                    alignItems: 'flex-start',
                    gap: 16,
                    padding: '16px 20px',
                    border: '1px solid #ddd',
                    borderRadius: 8,
                  }}
                >
                  <span
                    style={{
                      minWidth: 28,
                      height: 28,
                      borderRadius: '50%',
                      background: '#1565c0',
                      color: '#fff',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: 13,
                      fontWeight: 700,
                      flexShrink: 0,
                    }}
                    aria-hidden="true"
                  >
                    {idx + 1}
                  </span>
                  <span style={{ fontSize: 15 }}>{rec}</span>
                </li>
              ))}
            </ol>
          </>
        )}
      </main>
    </div>
  );
}
