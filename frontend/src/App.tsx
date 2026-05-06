import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from './auth/AuthContext';
import ProtectedRoute from './auth/ProtectedRoute';

// Public pages
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';

// User pages
import BookListPage from './pages/BookListPage';
import BookDetailPage from './pages/BookDetailPage';
import MyReadingsPage from './pages/MyReadingsPage';
import ReadingDetailPage from './pages/ReadingDetailPage';
import ProfilePage from './pages/ProfilePage';
import RecommendationsPage from './pages/RecommendationsPage';

// Admin pages
import AdminBookFormPage from './pages/admin/AdminBookFormPage';
import AdminAuthorListPage from './pages/admin/AdminAuthorListPage';
import AdminAuthorFormPage from './pages/admin/AdminAuthorFormPage';
import AdminPublisherListPage from './pages/admin/AdminPublisherListPage';
import AdminPublisherFormPage from './pages/admin/AdminPublisherFormPage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 30_000,
    },
  },
});

/**
 * Root application component.
 *
 * Provides:
 * - QueryClientProvider for React Query server state
 * - AuthProvider for JWT-based auth state
 * - BrowserRouter with the full route map
 *
 * Route structure:
 * - Public: /login, /register
 * - USER + ADMIN protected: /books, /books/:id, /readings, /readings/:id, /profile
 * - ADMIN only: /admin/books/*, /admin/authors/*, /admin/publishers/*
 */
export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            {/* Public routes */}
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />

            {/* USER + ADMIN protected routes */}
            <Route element={<ProtectedRoute />}>
              <Route path="/books" element={<BookListPage />} />
              <Route path="/books/:id" element={<BookDetailPage />} />
              <Route path="/readings" element={<MyReadingsPage />} />
              <Route path="/readings/:id" element={<ReadingDetailPage />} />
              <Route path="/profile" element={<ProfilePage />} />
              <Route path="/recommendations" element={<RecommendationsPage />} />
            </Route>

            {/* ADMIN-only routes */}
            <Route element={<ProtectedRoute requiredRole="ADMIN" />}>
              <Route path="/admin/books/new" element={<AdminBookFormPage />} />
              <Route path="/admin/books/:id/edit" element={<AdminBookFormPage />} />
              <Route path="/admin/authors" element={<AdminAuthorListPage />} />
              <Route path="/admin/authors/new" element={<AdminAuthorFormPage />} />
              <Route path="/admin/authors/:id/edit" element={<AdminAuthorFormPage />} />
              <Route path="/admin/publishers" element={<AdminPublisherListPage />} />
              <Route path="/admin/publishers/new" element={<AdminPublisherFormPage />} />
              <Route path="/admin/publishers/:id/edit" element={<AdminPublisherFormPage />} />
            </Route>

            {/* Default redirect */}
            <Route path="/" element={<Navigate to="/books" replace />} />
            <Route path="*" element={<Navigate to="/books" replace />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  );
}
