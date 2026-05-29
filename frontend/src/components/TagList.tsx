import type { TagDto } from '../types/contracts';

export function TagList({ tags }: { tags?: TagDto[] }) {
  if (!tags?.length) {
    return <span className="muted">none</span>;
  }
  return (
    <span className="tag-list">
      {tags.map((tag) => (
        <span key={tag.tagId} className="tag" style={tag.color ? { borderColor: tag.color } : undefined}>
          {tag.name}
        </span>
      ))}
    </span>
  );
}
