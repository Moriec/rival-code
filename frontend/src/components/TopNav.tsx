import { Code2, LogIn, LogOut, Swords, Trophy, User } from 'lucide-react';
import { NavLink, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { notificationApi } from '../api/endpoints';
import { useAuth } from '../features/auth/AuthProvider';
import { NotificationBell } from './NotificationBell';

export function TopNav() {
  const { user, isAuthenticated, logout } = useAuth();
  const notificationsQuery = useQuery({
    queryKey: ['notifications', 'header', user?.userId],
    queryFn: () => notificationApi.list(user?.userId),
    enabled: Boolean(user?.userId),
    refetchInterval: 30_000
  });
  const unread = notificationsQuery.data?.filter((item) => item.status === 'NEW').length ?? 0;

  return (
    <header className="top-nav">
      <div className="top-nav__inner">
        <Link to="/problems" className="brand">
          <Code2 size={18} />
          <span>RivalCode</span>
        </Link>
        <nav className="main-nav">
          <NavLink to="/problems">Problems</NavLink>
          <NavLink to="/submissions">Submissions</NavLink>
          <NavLink to="/duels">
            <Swords size={14} />
            Duels
          </NavLink>
          <NavLink to="/leaderboard">
            <Trophy size={14} />
            Leaderboard
          </NavLink>
        </nav>
        <div className="top-nav__user">
          {isAuthenticated ? (
            <>
              <NotificationBell unread={unread} />
              <Link to="/profile/me" className="user-chip">
                <User size={14} />
                {user?.displayName || user?.username || 'profile'}
              </Link>
              <button type="button" className="icon-button" onClick={logout} aria-label="Logout" title="Logout">
                <LogOut size={15} />
              </button>
            </>
          ) : (
            <>
              <NavLink to="/login">
                <LogIn size={14} />
                Login
              </NavLink>
              <NavLink to="/register">Register</NavLink>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
