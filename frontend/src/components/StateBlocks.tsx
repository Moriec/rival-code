import type { ReactNode } from 'react';
import { ApiError } from '../api/client';

export function LoadingState({ text = 'Loading...' }: { text?: string }) {
  return <div className="state-block">{text}</div>;
}

export function EmptyState({ text }: { text: string }) {
  return <div className="state-block state-block--muted">{text}</div>;
}

export function ErrorState({ error }: { error: unknown }) {
  const message = error instanceof Error ? error.message : 'Request failed';
  const traceId = error instanceof ApiError ? error.traceId : undefined;
  return (
    <div className="state-block state-block--error">
      <strong>{message}</strong>
      {traceId && <span>Trace: {traceId}</span>}
    </div>
  );
}

export function FieldError({ children }: { children?: ReactNode }) {
  if (!children) {
    return null;
  }
  return <div className="field-error">{children}</div>;
}
