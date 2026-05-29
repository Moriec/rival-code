import type { ProblemDifficulty } from '../types/contracts';

export function DifficultyBadge({ difficulty }: { difficulty?: ProblemDifficulty }) {
  if (!difficulty) {
    return <span className="badge badge--muted">UNKNOWN</span>;
  }
  return <span className={`difficulty difficulty--${difficulty.toLowerCase()}`}>{difficulty}</span>;
}
