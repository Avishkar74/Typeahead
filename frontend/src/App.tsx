import { useState } from 'react';
import type { RankingMode } from './types/api';
import { useSuggestions } from './hooks/useSuggestions';
import { submitSearch } from './services/searchService';
import Header from './components/Header';
import SearchBox from './components/SearchBox';
import SuggestionsPanel from './components/SuggestionsPanel';
import VirtualTimePanel from './components/VirtualTimePanel';
import CacheDebugPanel from './components/CacheDebugPanel';
import './index.css';

function App() {
  const [query, setQuery] = useState('');
  const [ranking, setRanking] = useState<RankingMode>('trending');
  const { data, loading, error } = useSuggestions(query, ranking, 300);
  const [searchMessage, setSearchMessage] = useState<string | null>(null);
  const [searchError, setSearchError] = useState<string | null>(null);
  const [searchLoading, setSearchLoading] = useState(false);

  const handleSearch = async (term: string) => {
    const trimmed = term.trim();
    if (!trimmed) {
      return;
    }

    setSearchLoading(true);
    setSearchError(null);
    setSearchMessage(null);

    try {
      const response = await submitSearch(trimmed);
      setSearchMessage(response.message);
    } catch (err: unknown) {
      setSearchError(err instanceof Error ? err.message : 'Failed to submit search');
    } finally {
      setSearchLoading(false);
    }
  };

  const handleSuggestionClick = (selected: string) => {
    setQuery(selected);
    void handleSearch(selected);
  };

  return (
    <div className="app">
      <Header />

      <main className="app-main">
        <section className="demo-section">
          <div className="section-label">
            <span className="section-number">01</span>
            <span>Search Typeahead</span>
          </div>
          <div className="demo-card">
            <SearchBox
              query={query}
              ranking={ranking}
              onQueryChange={setQuery}
              onRankingChange={setRanking}
              onSubmit={() => void handleSearch(query)}
            />
            {(searchMessage || searchError || searchLoading) && (
              <div className={`feedback ${searchError ? 'error-state' : searchLoading ? 'loading-state' : 'success-feedback'}`}>
                {searchLoading && <div className="spinner" />}
                <span>{searchError ?? searchMessage ?? 'Submitting search…'}</span>
              </div>
            )}
            <SuggestionsPanel
              data={data}
              loading={loading}
              error={error}
              query={query}
              onSuggestionClick={handleSuggestionClick}
            />
          </div>
        </section>

        <div className="bottom-grid">
          <section className="grid-cell">
            <VirtualTimePanel />
          </section>

          <section className="grid-cell">
            <div className="section-label">
              <span className="section-number">02</span>
              <span>Cache Debug</span>
            </div>
            <CacheDebugPanel />
          </section>
        </div>
      </main>

      <footer className="app-footer">
        <span>Search Typeahead System</span>
        <span className="footer-sep">·</span>
        <span>Premium SaaS interface</span>
      </footer>
    </div>
  );
}

export default App;
