import { type ChangeEvent, type FC, type KeyboardEvent } from 'react';
import type { RankingMode } from '../types/api';

interface Props {
  query: string;
  ranking: RankingMode;
  onQueryChange: (q: string) => void;
  onRankingChange: (r: RankingMode) => void;
  onSubmit: () => void;
}

const SearchBox: FC<Props> = ({ query, ranking, onQueryChange, onRankingChange, onSubmit }) => {
  const handleKey = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') onSubmit();
  };

  const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
    onQueryChange(e.target.value);
  };

  return (
    <div className="search-box-wrapper">
      <div className="search-input-row">
        <div className="search-icon-wrap">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="11" cy="11" r="8" />
            <line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
        </div>
        <input
          id="search-input"
          type="text"
          className="search-input"
          placeholder="Start typing to see suggestions…"
          value={query}
          onChange={handleChange}
          onKeyDown={handleKey}
          autoComplete="off"
          maxLength={255}
          aria-label="Search query"
        />
        <button
          type="button"
          className="search-submit-btn"
          onClick={onSubmit}
          aria-label="Submit search"
        >
          Search
        </button>
        {query && (
          <button
            type="button"
            className="search-clear-btn"
            onClick={() => onQueryChange('')}
            aria-label="Clear search"
          >
            ✕
          </button>
        )}
      </div>
      <div className="ranking-selector">
        <span className="ranking-label">Ranking:</span>
        {(['trending', 'global'] as RankingMode[]).map((mode) => (
          <button
            key={mode}
            id={`ranking-${mode}`}
            type="button"
            className={`ranking-btn ${ranking === mode ? 'active' : ''}`}
            onClick={() => onRankingChange(mode)}
          >
            {mode === 'trending' ? '🔥 Trending' : '🌐 Global'}
          </button>
        ))}
      </div>
    </div>
  );
};

export default SearchBox;
