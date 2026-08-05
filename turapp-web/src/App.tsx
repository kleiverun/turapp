import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import { NavBar } from './components/NavBar';
import { RequireAuth } from './components/RequireAuth';
import { LoginPage } from './pages/LoginPage';
import { PlannerPage } from './pages/PlannerPage';
import { RoutesPage } from './pages/RoutesPage';
import { RouteDetailPage } from './pages/RouteDetailPage';
import { TripsPage } from './pages/TripsPage';
import { TripDetailPage } from './pages/TripDetailPage';

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <NavBar />
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/planner"
            element={
              <RequireAuth>
                <PlannerPage />
              </RequireAuth>
            }
          />
          <Route
            path="/routes"
            element={
              <RequireAuth>
                <RoutesPage />
              </RequireAuth>
            }
          />
          <Route
            path="/routes/:routeId"
            element={
              <RequireAuth>
                <RouteDetailPage />
              </RequireAuth>
            }
          />
          <Route
            path="/trips"
            element={
              <RequireAuth>
                <TripsPage />
              </RequireAuth>
            }
          />
          <Route
            path="/trips/:tripId"
            element={
              <RequireAuth>
                <TripDetailPage />
              </RequireAuth>
            }
          />
          <Route path="*" element={<Navigate to="/planner" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
