import { useEffect, useState } from 'react';
import { MapContainer, Polyline, CircleMarker } from 'react-leaflet';
import { useParams } from 'react-router-dom';
import type { TrackPoint, TripResponse } from '../api/types';
import { getTrackPoints, getTrip } from '../api/trips';
import { errorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { KartverketTileLayer } from '../components/KartverketTileLayer';
import { FitBounds } from '../components/FitBounds';
import { formatDistance, formatDuration, formatPace } from '../utils/pace';

export function TripDetailPage() {
  const { tripId } = useParams();
  const { user } = useAuth();

  const [trip, setTrip] = useState<TripResponse | null>(null);
  const [points, setPoints] = useState<TrackPoint[]>([]);
  const [error, setError] = useState<string | null>(null);

  const id = Number(tripId);

  useEffect(() => {
    if (!user || !id) return;
    Promise.all([getTrip(user.id, id), getTrackPoints(id)])
      .then(([t, pts]) => {
        setTrip(t);
        setPoints(pts);
      })
      .catch((err) => setError(errorMessage(err)));
  }, [user, id]);

  const positions = points.map((p) => [p.latitude, p.longitude] as [number, number]);
  const avgMps =
    trip?.distanceMeters && trip?.durationSeconds && trip.durationSeconds > 0
      ? trip.distanceMeters / trip.durationSeconds
      : 0;

  return (
    <div className="detail-layout">
      <aside className="detail-sidebar">
        {error && <p className="error-text">{error}</p>}
        {trip && (
          <>
            <h1>{trip.name}</h1>
            <p className="muted">
              {new Date(trip.startedAt).toLocaleString('nb-NO', {
                dateStyle: 'long',
                timeStyle: 'short',
              })}
              {trip.notes ? ` — ${trip.notes}` : ''}
            </p>

            <div className="stats-grid">
              <div className="stat">
                <span className="stat-label">Distanse</span>
                <span className="stat-value">
                  {trip.distanceMeters != null ? formatDistance(trip.distanceMeters) : '–'}
                </span>
              </div>
              <div className="stat">
                <span className="stat-label">Varighet</span>
                <span className="stat-value">
                  {trip.durationSeconds != null ? formatDuration(trip.durationSeconds) : '–'}
                </span>
              </div>
              <div className="stat">
                <span className="stat-label">Snitt-tempo</span>
                <span className="stat-value">{formatPace(avgMps)}</span>
              </div>
            </div>

            {points.length === 0 && (
              <p className="muted small">Denne turen har ingen GPS-punkter.</p>
            )}
          </>
        )}
      </aside>

      <div className="detail-map">
        <MapContainer center={[59.913, 10.752]} zoom={12} style={{ height: '100%', width: '100%' }}>
          <KartverketTileLayer />
          {positions.length > 1 && (
            <>
              <Polyline positions={positions} pathOptions={{ color: '#2E7D32', weight: 5 }} />
              <FitBounds positions={positions} />
              <CircleMarker
                center={positions[0]}
                radius={8}
                pathOptions={{ color: '#fff', weight: 2, fillColor: '#2E7D32', fillOpacity: 1 }}
              />
              <CircleMarker
                center={positions[positions.length - 1]}
                radius={8}
                pathOptions={{ color: '#fff', weight: 2, fillColor: '#C62828', fillOpacity: 1 }}
              />
            </>
          )}
        </MapContainer>
      </div>
    </div>
  );
}
