import type { FC } from 'react';
import type { SuggestionDto, SuggestionResponse } from '../types/api';

interface Props {
  data: SuggestionResponse | null;
  loading: boolean;
  error: string | null;
  query: string;
  onSuggestionClick: (query: string) => void;
}

function ScoreBar({ value, max }: { value: number; max: number }) {
  const pct = max > 0 ? Math.round((value / max) * 100) : 0;
  return (
    <div className="score-bar-track" title={`Score: ${value.toFixed(2)}`}>
      <div className="score-bar-fill" style={{ width: `${pct}%` }} />
    </div>
  );
}

function SuggestionRow({ item, rank, maxScore, onClick }: {
  item: SuggestionDto;
  rank: number;
  maxScore: number;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      className="suggestion-row"
      onClick={onClick}
      id={`suggestion-${rank}`}
    >
      <span className="suggestion-rank">#{rank}</span>
      <div className="suggestion-body">
        <span className="suggestion-query">{item.query}</span>
        <ScoreBar value={Number(item.trending_score)} max={maxScore} />
      </div>
      <div className="suggestion-counts">
        <span className="count-chip count-global" title="Global">G {item.global_count}</span>
        <span className="count-chip count-weekly" title="Weekly">W {item.weekly_count}</span>
        <span className="count-chip count-daily" title="Daily">D {item.daily_count}</span>
      </div>
    </button>
  );
}

const SuggestionsPanel: FC<Props> = ({ data, loading, error, query, onSuggestionClick }) => {
  const trimmed = query.trim();

  if (trimmed.length < 3) {
    return (
      <div className="panel suggestions-panel">
        <div className="panel-header">
          <span className="panel-icon">💡</span>
          <h2 className="panel-title">Suggestions</h2>
        </div>
        <div className="empty-state">
          <p>Type at least 3 characters to see suggestions.</p>
        </div>
      </div>
    );
  }

  const maxScore = data
    ? Math.max(...data.results.map((r) => Number(r.trending_score)), 1)
    : 1;

  return (
    <div className="panel suggestions-panel">
      <div className="panel-header">
        <span className="panel-icon">💡</span>
        <h2 className="panel-title">Suggestions</h2>
        {data && (
          <div className="panel-meta">
            <span className={`cache-badge ${data.cache_hit ? 'hit' : 'miss'}`}>
              {data.cache_hit ? '✅ Cache HIT' : '⚡ Cache MISS'}
            </span>
            <span className="ranking-chip">{data.ranking_type}</span>
          </div>
        )}
      </div>

      {loading && (
        <div className="loading-state">
          <div className="spinner" />
          <span>Fetching suggestions…</span>
        </div>
      )}

      {error && !loading && (
        <div className="error-state">
          <span className="error-icon">⚠️</span>
          <span>{error}</span>
        </div>
      )}

      {!loading && !error && !data && (
        <div className="empty-state">
          <p>Suggestions will appear here once the backend responds.</p>
        </div>
      )}

      {!loading && !error && data && data.results.length === 0 && (
        <div className="empty-state">
          <p>No suggestions found for "<strong>{data.prefix}</strong>"</p>
        </div>
      )}

      {!loading && !error && data && data.results.length > 0 && (
        <div className="suggestion-list">
          {data.results.map((item, i) => (
            <SuggestionRow
              key={item.query}
              item={item}
              rank={i + 1}
              maxScore={maxScore}
              onClick={() => onSuggestionClick(item.query)}
            />
          ))}
        </div>
      )}
    </div>
  );
};

export default SuggestionsPanel;
