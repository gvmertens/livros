import client from './client';
import type { RecommendationResponse } from '../types';

/** GET /recommendations */
export async function getRecommendations(): Promise<RecommendationResponse> {
  const res = await client.get<RecommendationResponse>('/recommendations');
  return res.data;
}
