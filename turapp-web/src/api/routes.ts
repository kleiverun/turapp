import { api } from './client';
import type { LatLng, RoutePoint, RouteResponse } from './types';

export async function listRoutes(userId: number): Promise<RouteResponse[]> {
  const res = await api.get<RouteResponse[]>(`/users/${userId}/routes`);
  return res.data;
}

export async function getRoutePoints(routeId: number): Promise<RoutePoint[]> {
  const res = await api.get<{ routeId: number; points: RoutePoint[] }>(`/routes/${routeId}/points`);
  return res.data.points;
}

export async function createRoute(
  userId: number,
  name: string,
  description: string | null,
  points: LatLng[],
): Promise<RouteResponse> {
  const res = await api.post<RouteResponse>(`/users/${userId}/routes`, {
    name,
    description,
    points,
  });
  return res.data;
}

export async function updateRoute(
  userId: number,
  routeId: number,
  name: string,
  description: string | null,
): Promise<RouteResponse> {
  const res = await api.patch<RouteResponse>(`/users/${userId}/routes/${routeId}`, {
    name,
    description,
  });
  return res.data;
}

export async function deleteRoute(userId: number, routeId: number): Promise<void> {
  await api.delete(`/users/${userId}/routes/${routeId}`);
}

export interface TerrainSegment {
  points: LatLng[];
  distanceMeters: number;
  ascentMeters: number;
  descentMeters: number;
}

export async function getTerrainRoute(
  fromLat: number,
  fromLon: number,
  toLat: number,
  toLon: number,
): Promise<TerrainSegment> {
  const res = await api.post<TerrainSegment>('/route/terrain', { fromLat, fromLon, toLat, toLon });
  return res.data;
}
