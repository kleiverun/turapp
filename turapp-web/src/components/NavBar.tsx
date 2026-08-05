import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export function NavBar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  if (!user) return null;

  return (
    <nav className="navbar">
      <span className="navbar-brand">Turapp</span>
      <div className="navbar-links">
        <NavLink to="/planner">Planlegg</NavLink>
        <NavLink to="/routes">Mine ruter</NavLink>
        <NavLink to="/trips">Mine turer</NavLink>
      </div>
      <div className="navbar-user">
        <span className="navbar-name">{user.displayName}</span>
        <button
          className="btn-ghost"
          onClick={() => {
            logout();
            navigate('/login');
          }}
        >
          Logg ut
        </button>
      </div>
    </nav>
  );
}
