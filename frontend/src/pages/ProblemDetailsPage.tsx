import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery } from '@tanstack/react-query';
import { Send } from 'lucide-react';
import { problemApi, submissionApi } from '../api/endpoints';
import { CodeEditor } from '../components/CodeEditor';
import { DifficultyBadge } from '../components/DifficultyBadge';
import { FormattedStatement } from '../components/FormattedStatement';
import { Panel } from '../components/Panel';
import { ErrorState, FieldError, LoadingState } from '../components/StateBlocks';
import { TagList } from '../components/TagList';
import { useAuth } from '../features/auth/AuthProvider';
import type { SupportedUiLanguage } from '../types/contracts';
import { UI_LANGUAGES } from '../types/contracts';
import { defaultCode } from '../utils/languageTemplates';

export function ProblemDetailsPage() {
  const { problemId = '' } = useParams();
  const navigate = useNavigate();
  const { user, isAuthenticated } = useAuth();
  const [language, setLanguage] = useState<SupportedUiLanguage>('JAVA');
  const draftKey = useMemo(() => `rivalcode.draft.practice.${problemId}.${language}`, [language, problemId]);
  const [sourceCode, setSourceCode] = useState(() => window.localStorage.getItem(draftKey) ?? defaultCode(language));

  const problemQuery = useQuery({ queryKey: ['problem', problemId], queryFn: () => problemApi.get(problemId), enabled: Boolean(problemId) });

  useEffect(() => {
    setSourceCode(window.localStorage.getItem(draftKey) ?? defaultCode(language));
  }, [draftKey, language]);

  useEffect(() => {
    window.localStorage.setItem(draftKey, sourceCode);
  }, [draftKey, sourceCode]);

  const submitMutation = useMutation({
    mutationFn: () =>
      submissionApi.create({
        problemId,
        problemVersionId: problemQuery.data?.problemVersionId,
        mode: 'PRACTICE',
        userCode: { sourceCode, language }
      }),
    onSuccess: (response) => navigate(`/submissions/${response.submissionId}`)
  });

  if (problemQuery.isLoading) {
    return <LoadingState />;
  }
  if (problemQuery.isError || !problemQuery.data) {
    return <ErrorState error={problemQuery.error} />;
  }

  const problem = problemQuery.data;

  return (
    <div className="page-grid">
      <div className="page-grid">
        <Panel
          title={`${problem.slug}. ${problem.title}`}
          actions={
            <>
              <DifficultyBadge difficulty={problem.difficulty} />
              <TagList tags={problem.tags} />
            </>
          }
        >
          <div className="meta-grid">
            <div>
              <span className="meta-label">Time</span>
              <strong>{problem.limits?.timeLimitMs ?? '-'} ms</strong>
            </div>
            <div>
              <span className="meta-label">Memory</span>
              <strong>{problem.limits?.memoryLimitKb ?? '-'} KB</strong>
            </div>
            <div>
              <span className="meta-label">Checker</span>
              <strong>{problem.checkerType ?? 'STANDARD'}</strong>
            </div>
            <div>
              <span className="meta-label">Version</span>
              <strong>{problem.problemVersionId}</strong>
            </div>
          </div>
          <FormattedStatement text={problem.statement} />
        </Panel>

        <Panel title="Input">
          <FormattedStatement text={problem.inputSpec} compact />
        </Panel>

        <Panel title="Output">
          <FormattedStatement text={problem.outputSpec} compact />
        </Panel>

        <Panel title="Examples">
          <div className="page-grid">
            {(problem.examples ?? []).map((example) => (
              <div className="example-pair" key={example.orderNo}>
                <strong className="example-label">Input</strong>
                <pre className="example-block">{example.input}</pre>
                <strong className="example-label">Output</strong>
                <pre className="example-block">{example.expectedOutput}</pre>
                {example.explanation ? <FormattedStatement text={example.explanation} compact /> : null}
              </div>
            ))}
            {!problem.examples?.length && <span className="muted">No examples</span>}
          </div>
        </Panel>
      </div>

      <Panel title="Submit">
        {isAuthenticated ? (
          <form
            className="form-grid"
            onSubmit={(event) => {
              event.preventDefault();
              submitMutation.mutate();
            }}
          >
            <div className="editor-toolbar">
              <label>
                Language
                <select value={language} onChange={(event) => setLanguage(event.target.value as SupportedUiLanguage)}>
                  {UI_LANGUAGES.map((item) => (
                    <option key={item} value={item}>
                      {item}
                    </option>
                  ))}
                </select>
              </label>
              <button className="button-primary" type="submit" disabled={submitMutation.isPending}>
                <Send size={14} /> {submitMutation.isPending ? 'Sending...' : 'Submit'}
              </button>
            </div>
            <FieldError>{submitMutation.error instanceof Error ? submitMutation.error.message : undefined}</FieldError>
            <CodeEditor value={sourceCode} language={language} height="clamp(420px, 62vh, 720px)" onChange={setSourceCode} />
          </form>
        ) : (
          <p className="muted">
            <Link to="/login">Login</Link> to submit a solution.
          </p>
        )}
      </Panel>
    </div>
  );
}
