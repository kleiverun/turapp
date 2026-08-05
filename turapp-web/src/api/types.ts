export interface UserResponse {
  id: number;
  email: string;
  displayName: string;
  role: string;
  createdAt: string;
  token: string;
}

export interface TripResponse {
  id: number;
  name: string;
  notes: string | null;
  distanceMeters: number | null;
  durationSeconds: number | null;
  visibility: string;
  startedAt: string;
  endedAt: string | null;
}

export interface TrackPoint {
  latitude: number;
  longitude: number;
  recordedAt: string;
}

export interface RouteResponse {
  id: number;
  name: string;
  description: string | null;
  visibility: string | null;
  createdAt: string;
  pointCount: number;
  source: string | null;
}

export interface RoutePoint {
  pointOrder: number;
  latitude: number;
  longitude: number;
}

export interface LatLng {
  latitude: number;
  longitude: number;
}
