export function formatDate(value?: string): string {
  if (!value) {
    return '-';
  }
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'short',
    timeStyle: 'short'
  }).format(new Date(value));
}

export function ratio(accepted?: number, attempts?: number): string {
  const solved = accepted ?? 0;
  const total = attempts ?? 0;
  if (total === 0) {
    return `${solved}/0`;
  }
  return `${solved}/${total} (${Math.round((solved / total) * 100)}%)`;
}

export function displayName(user?: { displayName?: string; username?: string; userId?: string } | null): string {
  return user?.displayName || user?.username || user?.userId || 'user';
}
