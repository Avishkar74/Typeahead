import { type ChangeEvent, type FC, type FormEvent, useState } from 'react';
import { submitSearch } from '../services/searchService';

type FeedbackState = 'idle' | 'loading' | 'success' | 'error';

const SearchSubmission: FC = () => {
  const [query, setQuery] = useState('');
  const [feedback, setFeedback] = useState<FeedbackState>('idle');
  const [message, setMessage] = useState('');

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const trimmed = query.trim();
    if (!trimmed) return;

    setFeedback('loading');
    setMessage('');
    try {
      const res = await submitSearch(trimmed);
      setFeedback('success');
      setMessage(res.message ?? 'Search recorded');
      setQuery('');
    } catch (err: unknown) {
      setFeedback('error');
      setMessage(err instanceof Error ? err.message : 'Failed to submit search');
    }
  };

  return (
    <div className="panel">
      <div className="panel-header">
        <span className="panel-icon">📨</span>
        <h2 className="panel-title">Submit Search</h2>
      </div>
      <p className="panel-description">
        Record a search event. The query is persisted to <code>search_logs</code> and enqueued for batch aggregation.
      </p>
      <form className="submission-form" onSubmit={(e) => void handleSubmit(e)}>
        <input
          id="submit-search-input"
          type="text"
          className="text-input"
          placeholder="e.g. iphone"
          value={query}
          onChange={(e: ChangeEvent<HTMLInputElement>) => setQuery(e.target.value)}
          maxLength={255}
          aria-label="Search term to submit"
        />
        <button
          id="submit-search-btn"
          type="submit"
          className="btn btn-primary"
          disabled={feedback === 'loading' || !query.trim()}
        >
          {feedback === 'loading' ? 'Submitting…' : 'Submit'}
        </button>
      </form>

      {feedback === 'success' && (
        <div className="feedback success-feedback">
          <span>✅</span> {message}
        </div>
      )}
      {feedback === 'error' && (
        <div className="feedback error-feedback">
          <span>❌</span> {message}
        </div>
      )}
    </div>
  );
};

export default SearchSubmission;
