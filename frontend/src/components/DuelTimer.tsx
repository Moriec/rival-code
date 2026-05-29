import { useEffect, useMemo, useState } from 'react';

export function DuelTimer({ endsAt, stopped }: { endsAt?: string; stopped?: boolean }) {
  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    if (stopped) {
      return undefined;
    }
    const id = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(id);
  }, [stopped]);

  const remaining = useMemo(() => {
    if (!endsAt) {
      return 0;
    }
    return Math.max(0, new Date(endsAt).getTime() - now);
  }, [endsAt, now]);

  const minutes = Math.floor(remaining / 60000);
  const seconds = Math.floor((remaining % 60000) / 1000);

  return <span className={`timer ${remaining < 60_000 ? 'timer--danger' : ''}`}>{`${minutes}:${seconds.toString().padStart(2, '0')}`}</span>;
}
