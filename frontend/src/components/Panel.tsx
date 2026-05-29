import type { ReactNode } from 'react';

interface PanelProps {
  title?: ReactNode;
  actions?: ReactNode;
  children: ReactNode;
  className?: string;
}

export function Panel({ title, actions, children, className }: PanelProps) {
  return (
    <section className={`panel ${className ?? ''}`}>
      {(title || actions) && (
        <div className="panel__header">
          <h2>{title}</h2>
          <div className="panel__actions">{actions}</div>
        </div>
      )}
      <div className="panel__body">{children}</div>
    </section>
  );
}
