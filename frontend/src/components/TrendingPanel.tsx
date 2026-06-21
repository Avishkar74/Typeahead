import type { FC } from 'react';
import { useTrending } from '../hooks/useTrending';

const TrendingPanel: FC = () => {
  const { data, loading, error, load } = useTrending();

  return (
    <div className="panel">
      <div className="panel-header">
        <span className="panel-icon">🔥</span>
        <h2 className="panel-title">Trending Searches</h2>
        <button
          id="load-trending-btn"
          type="button"
          className="btn btn-secondary btn-sm"
          onClick={() => void load()}
          disabled={loading}
        >
          {loading ? 'Loading…' : data ? 'Refresh' : 'Load'}
        </button>
      </div>
      <p className="panel-description">Top 10 trending queries from <code>GET /trending</code>.</p>

      {error && (
        <div className="error-state">
          <span className="error-icon">⚠️</span> {error}
        </div>
      )}

      {loading && (
        <div className="loading-state">
          <div className="spinner" />
          <span>Loading trending…</span>
        </div>
      )}

      {!loading && data && data.results.length === 0 && (
        <div className="empty-state"><p>No trending searches yet.</p></div>
      )}

      {!loading && data && data.results.length > 0 && (
        <ol className="trending-list">
          {data.results.map((item, i) => (
            <li key={item.query} className="trending-item">
              <span className="trending-rank">{i + 1}</span>
              <div className="trending-body">
                <span className="trending-query">{item.query}</span>
                <div className="trending-meta">
                  <span className="count-chip count-global">G {item.global_count}</span>
                  <span className="count-chip count-weekly">W {item.weekly_count}</span>
                  <span className="score-label">score {Number(item.trending_score).toFixed(1)}</span>
                </div>
              </div>
            </li>
          ))}
        </ol>
      )}

      {!loading && !data && !error && (
        <div className="empty-state"><p>Click <strong>Load</strong> to fetch trending data.</p></div>
      )}
    </div>
  );
};

export default TrendingPanel;
