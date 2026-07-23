/**
 * TypeScript interfaces mirroring all backend API DTOs.
 * Keep in sync with the Java record types in com.library.*.api.dto
 */

// ── Identity ──────────────────────────────────────────────────────────────────

export interface User {
  id: string;
  name: string;
  email: string;
  role: 'USER' | 'ADMIN';
  createdAt: string; // ISO-8601
}

export interface LoginResponse {
  token: string;
  expiresIn: number; // seconds (3600)
}

export interface Profile {
  id: string;
  userId: string;
  displayName: string | null;
  bio: string | null;
  favoriteGenres: string[] | null;
  createdAt: string;
  updatedAt: string;
}

// ── Catalog ───────────────────────────────────────────────────────────────────

export interface Author {
  id: string;
  name: string;
  createdAt: string;
  updatedAt: string;
}

export interface Publisher {
  id: string;
  name: string;
  createdAt: string;
  updatedAt: string;
}

export interface Book {
  id: string;
  isbn: string;
  title: string;
  author: Author;
  publisher: Publisher;
  createdAt: string;
  updatedAt: string;
}

// ── Reading ───────────────────────────────────────────────────────────────────

export type ReadingStatus = 'WANT_TO_READ' | 'READING' | 'FINISHED' | 'ABANDONED';

export interface BookSummary {
  id: string;
  title: string;
  publisher: string;
}

export interface Reading {
  id: string;
  book: BookSummary;
  status: ReadingStatus;
  rating: number | null;
  review: string | null;
  startedAt: string | null;
  finishedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

// ── Pagination ────────────────────────────────────────────────────────────────

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

// ── Error ─────────────────────────────────────────────────────────────────────

export interface ErrorResponse {
  status: number;
  message: string;
  timestamp: string;
}

// ── Recommendation ────────────────────────────────────────────────────────────

export interface RecommendationResponse {
  criteriaSummary: string;
  recommendations: Array<{
    title: string;
    publisher: string;
  }>;
}

// ── Auth state (decoded JWT payload) ─────────────────────────────────────────

export interface JwtPayload {
  sub: string;       // user UUID
  email: string;
  groups: string[];  // ['USER'] or ['ADMIN']
  iss: string;
  exp: number;
  iat: number;
}
