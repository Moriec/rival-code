import { Outlet } from 'react-router-dom';
import { TopNav } from './TopNav';

export function AppLayout() {
  return (
    <div className="app-shell">
      <TopNav />
      <main className="content-shell">
        <Outlet />
      </main>
      <footer className="footer">RivalCode local MVP through gateway-service</footer>
    </div>
  );
}
