import { api } from './client';
import type { TrackPoint, TripResponse } from './types';

export async function listTrips(userId: number): Promise<TripResponse[]> {
  const res = await api.get<TripResponse[]>(`/users/${userId}/trips`);
  return res.data;
}

export async function getTrip(userId: number, tripId: number): Promise<TripResponse> {
  const res = await api.get<TripResponse>(`/users/${userId}/trips/${tripId}`);
  return res.data;
}

export async function getTrackPoints(tripId: number): Promise<TrackPoint[]> {
  const res = await api.get<{ tripId: number; points: TrackPoint[] }>(
    `/trips/${tripId}/trackpoints`,
  );
  return res.data.points;
}
