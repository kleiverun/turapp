import type { TripResponse } from '../api/types';

/** Port av mobilappens PaceEstimator — samme aktivitetstyper og logikk. */

export interface ActivityType {
  key: string;
  label: string;
  defaultMps: number;
}

export const ACTIVITY_TYPES: ActivityType[] = [
  { key: 'WALKING', label: 'Gange', defaultMps: 1.2 },
  { key: 'HIKING', label: 'Fjelltur', defaultMps: 0.85 },
  { key: 'RUNNING', label: 'Løping', defaultMps: 2.8 },
  { key: 'CYCLING', label: 'Sykling', defaultMps: 5.0 },
];

const WALKING_MPS = 1.2;

/**
 * Gjennomsnittsfart (m/s) fra brukerens turhistorikk.
 * Krever minst 2 turer med varighet > 30 s og distanse > 50 m.
 */
export function personalPaceMps(trips: TripResponse[]): number | null {
  const valid = trips.filter(
    (t) =>
      t.distanceMeters != null &&
      t.durationSeconds != null &&
      t.durationSeconds > 30 &&
      t.distanceMeters > 50,
  );
  if (valid.length < 2) return null;
  const sum = valid.reduce((acc, t) => acc + t.distanceMeters! / t.durationSeconds!, 0);
  return sum / valid.length;
}

/** Skalerer aktivitetens standardfart med brukerens formnivå (forhold mot gange). */
export function effectivePaceMps(activity: ActivityType, personal: number | null): number {
  if (personal == null) return activity.defaultMps;
  const ratio = Math.min(Math.max(personal / WALKING_MPS, 0.4), 3.0);
  return activity.defaultMps * ratio;
}

export function estimatedSeconds(
  distanceMeters: number,
  activity: ActivityType,
  personal: number | null,
): number {
  const mps = effectivePaceMps(activity, personal);
  if (mps <= 0 || distanceMeters <= 0) return 0;
  return Math.round(distanceMeters / mps);
}

export function formatDuration(totalSeconds: number): string {
  if (totalSeconds <= 0) return '–';
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.round((totalSeconds % 3600) / 60);
  if (hours > 0 && minutes > 0) return `${hours}t ${minutes} min`;
  if (hours > 0) return `${hours}t`;
  return `${Math.max(minutes, 1)} min`;
}

export function formatPace(mps: number): string {
  if (mps <= 0) return '–';
  const secPerKm = 1000 / mps;
  const min = Math.floor(secPerKm / 60);
  const sec = Math.round(secPerKm % 60);
  return `${min}:${sec.toString().padStart(2, '0')} /km`;
}

export function formatDistance(meters: number): string {
  if (meters <= 0) return '0 m';
  if (meters < 1000) return `${Math.round(meters)} m`;
  return `${(meters / 1000).toFixed(1)} km`;
}
