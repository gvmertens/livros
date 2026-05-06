import {
  createContext,
  useContext,
  useReducer,
  useEffect,
  type ReactNode,
} from 'react';
import type { JwtPayload } from '../types';

// ── Types ─────────────────────────────────────────────────────────────────────

export interface AuthUser {
  id: string;
  email: string;
  role: 'USER' | 'ADMIN';
}

interface AuthState {
  user: AuthUser | null;
}

interface AuthContextValue extends AuthState {
  login: (token: string) => void;
  logout: () => void;
}

// ── Reducer ───────────────────────────────────────────────────────────────────

type AuthAction =
  | { type: 'LOGIN'; user: AuthUser }
  | { type: 'LOGOUT' };

function authReducer(state: AuthState, action: AuthAction): AuthState {
  switch (action.type) {
    case 'LOGIN':
      return { user: action.user };
    case 'LOGOUT':
      return { user: null };
    default:
      return state;
  }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

/**
 * Decodes the JWT payload (middle base64url segment) without verifying the
 * signature. Used only to read claims for UI state — the backend validates
 * the signature on every protected request.
 */
function decodeJwtPayload(token: string): JwtPayload | null {
  try {
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    const payload = parts[1];
    // base64url → base64 → JSON
    const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
    return JSON.parse(json) as JwtPayload;
  } catch {
    return null;
  }
}

function userFromToken(token: string): AuthUser | null {
  const payload = decodeJwtPayload(token);
  if (!payload) return null;
  const role = payload.groups?.[0];
  if (role !== 'USER' && role !== 'ADMIN') return null;
  return { id: payload.sub, email: payload.email, role };
}

// ── Context ───────────────────────────────────────────────────────────────────

const AuthContext = createContext<AuthContextValue | null>(null);

// ── Provider ──────────────────────────────────────────────────────────────────

/**
 * Wraps the application and provides authentication state.
 *
 * On mount, restores the user from a previously stored JWT in localStorage.
 * Requirements: 2.1
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(authReducer, { user: null });

  // Restore session from localStorage on first render
  useEffect(() => {
    const token = localStorage.getItem('jwt');
    if (token) {
      const user = userFromToken(token);
      if (user) {
        dispatch({ type: 'LOGIN', user });
      } else {
        // Token is malformed — clear it
        localStorage.removeItem('jwt');
      }
    }
  }, []);

  function login(token: string) {
    const user = userFromToken(token);
    if (!user) return;
    localStorage.setItem('jwt', token);
    dispatch({ type: 'LOGIN', user });
  }

  function logout() {
    localStorage.removeItem('jwt');
    dispatch({ type: 'LOGOUT' });
    window.location.href = '/login';
  }

  return (
    <AuthContext.Provider value={{ user: state.user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

// ── Hook ──────────────────────────────────────────────────────────────────────

/**
 * Returns the current auth context. Must be used inside <AuthProvider>.
 */
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return ctx;
}
