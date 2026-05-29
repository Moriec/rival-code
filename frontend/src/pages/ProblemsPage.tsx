import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight, Search } from 'lucide-react';
import { pageContent, problemApi } from '../api/endpoints';
import { DataTable } from '../components/DataTable';
import { DifficultyBadge } from '../components/DifficultyBadge';
import { Panel } from '../components/Panel';
import { ErrorState, LoadingState } from '../components/StateBlocks';
import { StatusBadge } from '../components/StatusBadge';
import { TagList } from '../components/TagList';
import type { ProblemDifficulty, ProblemSummaryDto } from '../types/contracts';
import { ratio } from '../utils/format';

const PAGE_SIZES = [20, 50, 100] as const;

export function ProblemsPage() {
  const [search, setSearch] = useState('');
  const [difficulty, setDifficulty] = useState<ProblemDifficulty | ''>('');
  const [tagId, setTagId] = useState('');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<(typeof PAGE_SIZES)[number]>(50);

  const filter = useMemo(
    () => ({
      search: search || undefined,
      difficulties: difficulty ? [difficulty] : undefined,
      tagIds: tagId ? [tagId] : undefined,
      page,
      size: pageSize,
      sortBy: 'publishedAt',
      sortDirection: 'DESC'
    }),
    [difficulty, page, pageSize, search, tagId]
  );

  const problemsQuery = useQuery({ queryKey: ['problems', filter], queryFn: () => problemApi.list(filter) });
  const tagsQuery = useQuery({ queryKey: ['problem-tags'], queryFn: problemApi.tags });
  const problems = pageContent(problemsQuery.data);
  const pageData = problemsQuery.data && !Array.isArray(problemsQuery.data) ? problemsQuery.data : undefined;
  const currentPage = pageData?.number ?? page;
  const currentPageSize = pageData?.size ?? pageSize;
  const totalElements = pageData?.totalElements ?? problems.length;
  const totalPages = Math.max(pageData?.totalPages ?? 1, 1);
  const shownFrom = totalElements === 0 ? 0 : currentPage * currentPageSize + 1;
  const shownTo = totalElements === 0 ? 0 : Math.min(totalElements, currentPage * currentPageSize + problems.length);
  const hasPreviousPage = currentPage > 0;
  const hasNextPage = currentPage + 1 < totalPages;
  const archiveSummary = totalElements === 0 ? '0 problems' : `${shownFrom}-${shownTo} of ${totalElements}`;

  const resetPage = () => setPage(0);
  const goToPage = (nextPage: number) => setPage(Math.max(0, Math.min(nextPage, totalPages - 1)));

  return (
    <div className="page-grid">
      <Panel title="Problems" actions={<span className="muted">{archiveSummary}</span>}>
        <div className="filters">
          <label>
            Search
            <input
              value={search}
              onChange={(event) => {
                setSearch(event.target.value);
                resetPage();
              }}
              placeholder="title or slug"
            />
          </label>
          <label>
            Difficulty
            <select
              value={difficulty}
              onChange={(event) => {
                setDifficulty(event.target.value as ProblemDifficulty | '');
                resetPage();
              }}
            >
              <option value="">All</option>
              <option value="EASY">Easy</option>
              <option value="MEDIUM">Medium</option>
              <option value="HARD">Hard</option>
            </select>
          </label>
          <label>
            Tag
            <select
              value={tagId}
              onChange={(event) => {
                setTagId(event.target.value);
                resetPage();
              }}
            >
              <option value="">All tags</option>
              {(tagsQuery.data ?? []).map((tag) => (
                <option key={tag.tagId} value={tag.tagId}>
                  {tag.name}
                </option>
              ))}
            </select>
          </label>
          <button
            type="button"
            onClick={() => {
              resetPage();
              problemsQuery.refetch();
            }}
            title="Refresh"
          >
            <Search size={14} /> Apply
          </button>
        </div>
      </Panel>

      <Panel
        title="Archive"
        actions={<span className="muted">{problemsQuery.isFetching && !problemsQuery.isLoading ? 'Updating...' : archiveSummary}</span>}
      >
        {problemsQuery.isLoading ? (
          <LoadingState />
        ) : problemsQuery.isError ? (
          <ErrorState error={problemsQuery.error} />
        ) : (
          <>
            <DataTable<ProblemSummaryDto>
              columns={[
                {
                  key: 'title',
                  header: 'Problem',
                  render: (problem) => (
                    <Link to={`/problems/${problem.problemId}`}>
                      {problem.slug}. {problem.title}
                    </Link>
                  )
                },
                { key: 'difficulty', header: 'Difficulty', render: (problem) => <DifficultyBadge difficulty={problem.difficulty} /> },
                { key: 'tags', header: 'Tags', render: (problem) => <TagList tags={problem.tags} /> },
                { key: 'accepted', header: 'Accepted', render: (problem) => ratio(problem.acceptedCount, problem.attemptsCount), align: 'right' },
                { key: 'status', header: 'Status', render: (problem) => <StatusBadge status={problem.status} /> }
              ]}
              data={problems}
              getRowKey={(problem) => problem.problemId}
              emptyText="No problems found"
            />
            <div className="pagination-bar" aria-label="Problem archive pages">
              <span className="muted">
                Page {currentPage + 1} of {totalPages}
              </span>
              <div className="pagination-controls">
                <button type="button" onClick={() => goToPage(0)} disabled={!hasPreviousPage} title="First page" aria-label="First page">
                  <ChevronsLeft size={15} />
                </button>
                <button
                  type="button"
                  onClick={() => goToPage(currentPage - 1)}
                  disabled={!hasPreviousPage}
                  title="Previous page"
                  aria-label="Previous page"
                >
                  <ChevronLeft size={15} />
                </button>
                <button
                  type="button"
                  onClick={() => goToPage(currentPage + 1)}
                  disabled={!hasNextPage}
                  title="Next page"
                  aria-label="Next page"
                >
                  <ChevronRight size={15} />
                </button>
                <button
                  type="button"
                  onClick={() => goToPage(totalPages - 1)}
                  disabled={!hasNextPage}
                  title="Last page"
                  aria-label="Last page"
                >
                  <ChevronsRight size={15} />
                </button>
              </div>
              <label className="page-size-control">
                Per page
                <select
                  value={pageSize}
                  onChange={(event) => {
                    setPageSize(Number(event.target.value) as (typeof PAGE_SIZES)[number]);
                    resetPage();
                  }}
                >
                  {PAGE_SIZES.map((size) => (
                    <option key={size} value={size}>
                      {size}
                    </option>
                  ))}
                </select>
              </label>
            </div>
          </>
        )}
      </Panel>
    </div>
  );
}
