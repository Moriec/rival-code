import { Navigate, createBrowserRouter, RouterProvider } from 'react-router-dom';
import { AppLayout } from './components/AppLayout';
import { RequireAuth } from './components/RequireAuth';
import { DuelRoomPage } from './pages/DuelRoomPage';
import { DuelsPage } from './pages/DuelsPage';
import { LeaderboardPage } from './pages/LeaderboardPage';
import { LoginPage } from './pages/LoginPage';
import { NotFoundPage } from './pages/NotFoundPage';
import { NotificationsPage } from './pages/NotificationsPage';
import { ProblemDetailsPage } from './pages/ProblemDetailsPage';
import { ProblemsPage } from './pages/ProblemsPage';
import { ProfilePage } from './pages/ProfilePage';
import { RegisterPage } from './pages/RegisterPage';
import { SubmissionDetailsPage } from './pages/SubmissionDetailsPage';
import { SubmissionsPage } from './pages/SubmissionsPage';
import { UserProfilePage } from './pages/UserProfilePage';

const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      { index: true, element: <Navigate to="/problems" replace /> },
      { path: 'login', element: <LoginPage /> },
      { path: 'register', element: <RegisterPage /> },
      { path: 'problems', element: <ProblemsPage /> },
      { path: 'problems/:problemId', element: <ProblemDetailsPage /> },
      {
        path: 'submissions',
        element: (
          <RequireAuth>
            <SubmissionsPage />
          </RequireAuth>
        )
      },
      {
        path: 'submissions/:submissionId',
        element: (
          <RequireAuth>
            <SubmissionDetailsPage />
          </RequireAuth>
        )
      },
      {
        path: 'duels',
        element: (
          <RequireAuth>
            <DuelsPage />
          </RequireAuth>
        )
      },
      {
        path: 'duels/:duelId',
        element: (
          <RequireAuth>
            <DuelRoomPage />
          </RequireAuth>
        )
      },
      { path: 'leaderboard', element: <LeaderboardPage /> },
      {
        path: 'notifications',
        element: (
          <RequireAuth>
            <NotificationsPage />
          </RequireAuth>
        )
      },
      {
        path: 'profile/me',
        element: (
          <RequireAuth>
            <ProfilePage />
          </RequireAuth>
        )
      },
      { path: 'users/:userId', element: <UserProfilePage /> },
      { path: '*', element: <NotFoundPage /> }
    ]
  }
]);

export function App() {
  return <RouterProvider router={router} />;
}
