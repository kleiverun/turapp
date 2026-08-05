import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import type { TripResponse } from '../api/types';
import { listTrips } from '../api/trips';
import { errorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { formatDistance, formatDuration } from '../utils/pace';

export function TripsPage() {
  const { user } = useAuth();
  const [trips, setTrips] = useState<TripResponse[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!user) return;
    listTrips(user.id)
      .then((t) => setTrips([...t].sort((a, b) => b.startedAt.localeCompare(a.startedAt))))
      .catch((err) => setError(errorMessage(err)));
  }, [user]);

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>Mine turer</h1>
          <p className="muted">Turer registrert med mobilappen.</p>
        </div>
      </div>

      {error && <p className="error-text">{error}</p>}
      {trips && trips.length === 0 && (
        <p className="empty-state">Ingen turer ennå. Start en tur fra mobilappen!</p>
      )}

      <ul className="card-list">
        {trips?.map((trip) => (
          <li key={trip.id}>
            <Link className="card-row" to={`/trips/${trip.id}`}>
              <div>
                <span className="card-title">{trip.name}</span>
                <span className="card-subtitle">
                  {new Date(trip.startedAt).toLocaleString('nb-NO', {
                    dateStyle: 'short',
                    timeStyle: 'short',
                  })}
                  {trip.distanceMeters != null && ` • ${formatDistance(trip.distanceMeters)}`}
                  {trip.durationSeconds != null && ` • ${formatDuration(trip.durationSeconds)}`}
                </span>
              </div>
              <span className="card-chevron">›</span>
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
}
