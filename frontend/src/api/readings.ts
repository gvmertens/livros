import client from './client';
import type { Reading, ReadingStatus, PageResponse } from '../types';

export interface CreateReadingRequest {
  bookId: string;
}

export interface UpdateReadingRequest {
  status?: ReadingStatus;
  rating?: number | null;
  review?: string | null;
  startedAt?: string | null;
  finishedAt?: string | null;
}

/** POST /readings */
export async function createReading(data: CreateReadingRequest): Promise<Reading> {
  const res = await client.post<Reading>('/readings', data);
  return res.data;
}

/** GET /readings */
export async function listReadings(
  page = 0,
  size = 20,
  status?: ReadingStatus,
): Promise<PageResponse<Reading>> {
  const res = await client.get<PageResponse<Reading>>('/readings', {
    params: { page, size, ...(status ? { status } : {}) },
  });
  return res.data;
}

/** GET /readings/:id */
export async function getReading(id: string): Promise<Reading> {
  const res = await client.get<Reading>(`/readings/${id}`);
  return res.data;
}

/** PUT /readings/:id */
export async function updateReading(id: string, data: UpdateReadingRequest): Promise<Reading> {
  const res = await client.put<Reading>(`/readings/${id}`, data);
  return res.data;
}

/** DELETE /readings/:id */
export async function deleteReading(id: string): Promise<void> {
  await client.delete(`/readings/${id}`);
}
