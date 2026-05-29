import { Client, type IMessage } from '@stomp/stompjs';
import { useCallback, useEffect, useRef, useState } from 'react';
import { WS_BASE_URL } from '../api/client';
import { getAccessToken } from '../features/auth/tokenStorage';
import type { CodeSnapshotMessage, DuelFinishedEvent, DuelRoomStateDto } from '../types/contracts';

interface DuelSocketOptions {
  duelId?: string;
  userId?: string;
  onState?: (state: DuelRoomStateDto) => void;
  onFinished?: (event: DuelFinishedEvent) => void;
  onOpponentCode?: (message: CodeSnapshotMessage) => void;
}

export function useDuelSocket({ duelId, userId, onState, onFinished, onOpponentCode }: DuelSocketOptions) {
  const clientRef = useRef<Client | null>(null);
  const handlersRef = useRef({ onState, onFinished, onOpponentCode });
  const userIdRef = useRef(userId);
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    handlersRef.current = { onState, onFinished, onOpponentCode };
  }, [onFinished, onOpponentCode, onState]);

  useEffect(() => {
    userIdRef.current = userId;
  }, [userId]);

  useEffect(() => {
    if (!duelId || !userId) {
      return undefined;
    }

    const token = getAccessToken();
    const client = new Client({
      brokerURL: `${WS_BASE_URL}/ws/duels`,
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: () => undefined
    });

    client.onConnect = () => {
      setConnected(true);
      client.subscribe(`/topic/duels/${duelId}/state`, (message) => parseMessage(message, handlersRef.current.onState));
      client.subscribe(`/topic/duels/${duelId}/finished`, (message) => parseMessage(message, handlersRef.current.onFinished));
      client.subscribe(`/topic/duels/${duelId}/code`, (message) => {
        const snapshot = parsePayload<CodeSnapshotMessage>(message);
        if (!snapshot || snapshot.userId === userIdRef.current) {
          return;
        }
        handlersRef.current.onOpponentCode?.(snapshot);
      });
    };
    client.onDisconnect = () => setConnected(false);
    client.onStompError = () => setConnected(false);
    client.onWebSocketClose = () => setConnected(false);

    client.activate();
    clientRef.current = client;

    return () => {
      setConnected(false);
      void client.deactivate();
      clientRef.current = null;
    };
  }, [duelId, userId]);

  const sendCodeSnapshot = useCallback((message: CodeSnapshotMessage) => {
    const client = clientRef.current;
    if (!client?.connected || !duelId) {
      return;
    }
    client.publish({
      destination: `/app/duels/${duelId}/code`,
      body: JSON.stringify(message)
    });
  }, [duelId]);

  return { connected, sendCodeSnapshot };
}

function parseMessage<T>(message: IMessage, handler?: (payload: T) => void) {
  if (!handler) {
    return;
  }
  const payload = parsePayload<T>(message);
  if (payload) {
    handler(payload);
  }
}

function parsePayload<T>(message: IMessage): T | null {
  try {
    return JSON.parse(message.body) as T;
  } catch {
    // Keep the socket alive if one message is malformed.
    return null;
  }
}
