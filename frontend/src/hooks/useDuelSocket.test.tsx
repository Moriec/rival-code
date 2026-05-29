import { act, renderHook } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useDuelSocket } from './useDuelSocket';

const activateMock = vi.fn();
const deactivateMock = vi.fn(() => Promise.resolve());
const clientInstances: Array<{
  activate: ReturnType<typeof vi.fn>;
  deactivate: ReturnType<typeof vi.fn>;
  publish: ReturnType<typeof vi.fn>;
  subscribe: ReturnType<typeof vi.fn>;
  connected: boolean;
  onConnect?: () => void;
}> = [];

vi.mock('@stomp/stompjs', () => ({
  Client: vi.fn().mockImplementation(function () {
    const client = {
      activate: activateMock,
      deactivate: deactivateMock,
      publish: vi.fn(),
      subscribe: vi.fn(),
      connected: true
    };
    clientInstances.push(client);
    return client;
  })
}));

describe('useDuelSocket', () => {
  beforeEach(() => {
    activateMock.mockClear();
    deactivateMock.mockClear();
    clientInstances.length = 0;
    window.sessionStorage.clear();
    window.localStorage.clear();
  });

  it('does not reconnect when message handlers get a new identity on rerender', () => {
    const { rerender, unmount } = renderHook(
      ({ onOpponentCode }: { onOpponentCode: () => void }) =>
        useDuelSocket({
          duelId: 'duel-1',
          userId: 'user-1',
          onOpponentCode
        }),
      {
        initialProps: {
          onOpponentCode: vi.fn()
        }
      }
    );

    rerender({ onOpponentCode: vi.fn() });

    expect(activateMock).toHaveBeenCalledTimes(1);
    expect(deactivateMock).not.toHaveBeenCalled();

    unmount();

    expect(deactivateMock).toHaveBeenCalledTimes(1);
  });

  it('delivers snapshots from another participant and ignores own snapshots', () => {
    const onOpponentCode = vi.fn();
    renderHook(() =>
      useDuelSocket({
        duelId: 'duel-1',
        userId: 'user-1',
        onOpponentCode
      })
    );

    act(() => clientInstances[0].onConnect?.());
    const codeSubscription = clientInstances[0].subscribe.mock.calls.find(([destination]) => destination === '/topic/duels/duel-1/code');
    expect(codeSubscription).toBeDefined();

    const callback = codeSubscription?.[1] as (message: { body: string }) => void;
    callback({ body: JSON.stringify({ duelId: 'duel-1', userId: 'user-1', sourceCode: 'self' }) });
    callback({ body: JSON.stringify({ duelId: 'duel-1', userId: 'user-2', sourceCode: 'opponent' }) });

    expect(onOpponentCode).toHaveBeenCalledTimes(1);
    expect(onOpponentCode).toHaveBeenCalledWith(expect.objectContaining({ sourceCode: 'opponent' }));
  });
});
