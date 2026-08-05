import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import type { RouteResponse } from '../api/types';
import { listRoutes } from '../api/routes';
import { errorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';

export function RoutesPage() {
  const { user } = useAuth();
  const [routes, setRoutes] = useState<RouteResponse[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!user) return;
    listRoutes(user.id)
      .then((r) =>
        setRoutes(
          r
            // GPX-importerte turruter er bakgrunnsnett — vises ikke i listen
            .filter((route) => !route.source || route.source === 'PLANNED')
            .sort((a, b) => b.createdAt.localeCompare(a.createdAt)),
        ),
      )
      .catch((err) => setError(errorMessage(err)));
  }, [user]);

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>Mine ruter</h1>
          <p className="muted">Planlagte ruter — synlige på mobilen når du følger dem.</p>
        </div>
        <Link className="btn-primary" to="/planner">
          + Ny rute
        </Link>
      </div>

      {error && <p className="error-text">{error}</p>}
      {routes && routes.length === 0 && (
        <p className="empty-state">Ingen ruter ennå. Planlegg din første rute!</p>
      )}

      <ul className="card-list">
        {routes?.map((route) => (
          <li key={route.id}>
            <Link className="card-row" to={`/routes/${route.id}`}>
              <div>
                <span className="card-title">{route.name}</span>
                <span className="card-subtitle">
                  {route.pointCount} punkter •{' '}
                  {new Date(route.createdAt).toLocaleDateString('nb-NO')}
                  {route.description ? ` • ${route.description}` : ''}
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
