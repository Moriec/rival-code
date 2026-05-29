import { Bell } from 'lucide-react';
import { Link } from 'react-router-dom';

export function NotificationBell({ unread }: { unread: number }) {
  return (
    <Link to="/notifications" className="icon-link" aria-label="Notifications">
      <Bell size={16} />
      {unread > 0 && <span className="notification-dot">{unread > 99 ? '99+' : unread}</span>}
    </Link>
  );
}
