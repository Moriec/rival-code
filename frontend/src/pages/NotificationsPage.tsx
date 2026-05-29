import { useCallback } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CheckCheck } from 'lucide-react';
import { notificationApi } from '../api/endpoints';
import { Panel } from '../components/Panel';
import { ErrorState, LoadingState } from '../components/StateBlocks';
import { StatusBadge } from '../components/StatusBadge';
import { useAuth } from '../features/auth/AuthProvider';
import { useNotificationStream } from '../hooks/useNotificationStream';
import type { NotificationDto } from '../types/contracts';
import { formatDate } from '../utils/format';

export function NotificationsPage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const notificationsQuery = useQuery({
    queryKey: ['notifications', user?.userId],
    queryFn: () => notificationApi.list(user?.userId),
    enabled: Boolean(user?.userId),
    refetchInterval: 30_000
  });

  const onNotification = useCallback(
    (notification: NotificationDto) => {
      queryClient.setQueryData<NotificationDto[]>(['notifications', user?.userId], (current) => {
        if (!current) {
          return [notification];
        }
        if (current.some((item) => item.notificationId === notification.notificationId)) {
          return current;
        }
        return [notification, ...current];
      });
    },
    [queryClient, user?.userId]
  );

  useNotificationStream(Boolean(user?.userId), onNotification);

  const unread = (notificationsQuery.data ?? []).filter((item) => item.status === 'NEW');
  const markReadMutation = useMutation({
    mutationFn: () =>
      notificationApi.markRead({
        userId: user?.userId ?? '',
        notificationIds: unread.map((item) => item.notificationId),
        readAt: new Date().toISOString()
      }),
    onSuccess: (notifications) => {
      queryClient.setQueryData(['notifications', user?.userId], notifications);
    }
  });

  return (
    <Panel
      title="Notifications"
      actions={
        <button type="button" onClick={() => markReadMutation.mutate()} disabled={unread.length === 0 || markReadMutation.isPending}>
          <CheckCheck size={14} /> Mark all read
        </button>
      }
    >
      {notificationsQuery.isLoading ? (
        <LoadingState />
      ) : notificationsQuery.isError ? (
        <ErrorState error={notificationsQuery.error} />
      ) : (
        <div className="notification-list">
          {(notificationsQuery.data ?? []).map((notification) => (
            <article
              className={`notification-item ${notification.status === 'NEW' ? 'notification-item--new' : ''}`}
              key={notification.notificationId}
            >
              <div className="inline-actions">
                <StatusBadge status={notification.status} />
                <strong>{notification.title}</strong>
                <span className="muted">{formatDate(notification.createdAt)}</span>
              </div>
              {notification.body && <p>{notification.body}</p>}
              {notification.payload?.duelId ? <span className="muted">Duel: {String(notification.payload.duelId)}</span> : null}
            </article>
          ))}
          {!notificationsQuery.data?.length && <div className="state-block">No notifications</div>}
        </div>
      )}
    </Panel>
  );
}
