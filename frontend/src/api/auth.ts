import client from './client';
import type { User, LoginResponse, Profile } from '../types';

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface ProfileUpdateRequest {
  displayName?: string;
  bio?: string;
  favoriteGenres?: string[];
}

export interface UpdateRoleRequest {
  role: 'USER' | 'ADMIN';
}

/** POST /auth/register */
export async function register(data: RegisterRequest): Promise<User> {
  const res = await client.post<User>('/auth/register', data);
  return res.data;
}

/** POST /auth/login */
export async function login(data: LoginRequest): Promise<LoginResponse> {
  const res = await client.post<LoginResponse>('/auth/login', data);
  return res.data;
}

/** GET /users/me */
export async function getMe(): Promise<User> {
  const res = await client.get<User>('/users/me');
  return res.data;
}

/** GET /users/me/profile */
export async function getMyProfile(): Promise<Profile> {
  const res = await client.get<Profile>('/users/me/profile');
  return res.data;
}

/** PUT /users/me/profile */
export async function updateMyProfile(data: ProfileUpdateRequest): Promise<Profile> {
  const res = await client.put<Profile>('/users/me/profile', data);
  return res.data;
}

/** PUT /users/{id}/role */
export async function updateUserRole(userId: string, data: UpdateRoleRequest): Promise<User> {
  const res = await client.put<User>(`/users/${userId}/role`, data);
  return res.data;
}
