import { fetchEventSource } from '@microsoft/fetch-event-source';
import { useEffect } from 'react';
import { API_BASE_URL } from '../api/client';
import { getAccessToken } from '../features/auth/tokenStorage';
import type { NotificationDto } from '../types/contracts';

export function useNotificationStream(enabled: boolean, onNotification: (notification: NotificationDto) => void) {
  useEffect(() => {
    if (!enabled) {
      return undefined;
    }

    const controller = new AbortController();
    void fetchEventSource(`${API_BASE_URL}/api/notifications/stream`, {
      signal: controller.signal,
      headers: {
        Authorization: `Bearer ${getAccessToken() ?? ''}`
      },
      onmessage(event) {
        if (!event.data) {
          return;
        }
        try {
          onNotification(JSON.parse(event.data) as NotificationDto);
        } catch {
          // Ignore malformed SSE events; the normal inbox polling remains the fallback.
        }
      },
      onerror(error) {
        throw error;
      }
    }).catch(() => undefined);

    return () => controller.abort();
  }, [enabled, onNotification]);
}
