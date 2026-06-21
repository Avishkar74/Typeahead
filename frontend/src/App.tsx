import { useState } from 'react';
import type { RankingMode } from './types/api';
import { useSuggestions } from './hooks/useSuggestions';
import Header from './components/Header';
import SearchBox from './components/SearchBox';
import SuggestionsPanel from './components/SuggestionsPanel';
import SearchSubmission from './components/SearchSubmission';
import TrendingPanel from './components/TrendingPanel';
import VirtualTimePanel from './components/VirtualTimePanel';
import CacheDebugPanel from './components/CacheDebugPanel';
import './index.css';

function App() {
  const [query, setQuery] = useState('');
  const [ranking, setRanking] = useState<RankingMode>('trending');
  const { data, loading, error } = useSuggestions(query, ranking, 300);

  const handleSuggestionClick = (selected: string) => {
    setQuery(selected);
  };

  return (
    <div className="app">
      <Header />

      <main className="app-main">
        {/* ─── Search Typeahead Demo ─── */}
        <section className="demo-section">
          <div className="section-label">
            <span className="section-number">01</span>
            <span>Search Typeahead Demo</span>
          </div>
          <div className="demo-card">
            <SearchBox
              query={query}
              ranking={ranking}
              onQueryChange={setQuery}
              onRankingChange={setRanking}
              onSubmit={() => {/* suggestions auto-show */}}
            />
            <SuggestionsPanel
              data={data}
              loading={loading}
              error={error}
              query={query}
              onSuggestionClick={handleSuggestionClick}
            />
          </div>
        </section>

        {/* ─── Bottom Grid ─── */}
        <div className="bottom-grid">
          <section className="grid-cell">
            <div className="section-label">
              <span className="section-number">02</span>
              <span>Submit Search</span>
            </div>
            <SearchSubmission />
          </section>

          <section className="grid-cell">
            <div className="section-label">
              <span className="section-number">03</span>
              <span>Trending</span>
            </div>
            <TrendingPanel />
          </section>

          <section className="grid-cell">
            <div className="section-label">
              <span className="section-number">04</span>
              <span>Virtual Time</span>
            </div>
            <VirtualTimePanel />
          </section>

          <section className="grid-cell">
            <div className="section-label">
              <span className="section-number">05</span>
              <span>Cache Debug</span>
            </div>
            <CacheDebugPanel />
          </section>
        </div>
      </main>

      <footer className="app-footer">
        <span>Search Typeahead System — HLD Assignment</span>
        <span className="footer-sep">·</span>
        <span>React + Spring Boot + Redis × 3 + PostgreSQL</span>
      </footer>
    </div>
  );
}

export default App;
