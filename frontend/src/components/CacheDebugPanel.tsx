import { type ChangeEvent, type FC, type FormEvent, useState } from 'react';
import { useCacheDebug } from '../hooks/useCacheDebug';

const CacheDebugPanel: FC = () => {
  const [prefix, setPrefix] = useState('');
  const { data, loading, error, lookup } = useCacheDebug();

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    void lookup(prefix);
  };

  return (
    <div className="panel">
      <div className="panel-header">
        <span className="panel-icon">🔍</span>
        <h2 className="panel-title">Cache Debug</h2>
        <span className="badge badge-blue">GET /api/debug/cache</span>
      </div>
      <p className="panel-description">
        Resolve which Redis node and slot handles a given prefix, and check whether it is currently cached.
      </p>

      <form className="submission-form" onSubmit={handleSubmit}>
        <input
          id="cache-debug-input"
          type="text"
          className="text-input"
          placeholder="e.g. iph"
          value={prefix}
          onChange={(e: ChangeEvent<HTMLInputElement>) => setPrefix(e.target.value)}
          maxLength={255}
          aria-label="Prefix to debug"
        />
        <button
          id="cache-debug-btn"
          type="submit"
          className="btn btn-primary"
          disabled={loading || !prefix.trim()}
        >
          {loading ? 'Looking up…' : 'Lookup'}
        </button>
      </form>

      {error && (
        <div className="error-state">
          <span className="error-icon">⚠️</span> {error}
        </div>
      )}

      {data && !loading && (
        <div className="debug-result">
          <div className="debug-grid">
            <div className="debug-cell">
              <span className="debug-label">Prefix</span>
              <code className="debug-value">{data.prefix}</code>
            </div>
            <div className="debug-cell">
              <span className="debug-label">Slot</span>
              <code className="debug-value">{data.slot}</code>
            </div>
            <div className="debug-cell">
              <span className="debug-label">Responsible Node</span>
              <code className="debug-value node-value">{data.responsible_node}</code>
            </div>
            <div className="debug-cell">
              <span className="debug-label">Cache Status</span>
              <span className={`cache-badge large ${data.cache_hit ? 'hit' : 'miss'}`}>
                {data.cache_hit ? '✅ HIT' : '❌ MISS'}
              </span>
            </div>
          </div>

          <div className="node-diagram">
            {['localhost:6379', 'localhost:6380', 'localhost:6381'].map((node) => (
              <div
                key={node}
                className={`node-pill ${node === data.responsible_node ? 'active-node' : ''}`}
              >
                <span className="node-dot" />
                {node}
                {node === data.responsible_node && <span className="node-arrow">← responsible</span>}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default CacheDebugPanel;
