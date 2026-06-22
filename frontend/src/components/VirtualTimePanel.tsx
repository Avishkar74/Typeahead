import { type FC, useEffect, useState } from 'react';
import { fetchVirtualTime } from '../services/virtualTimeService';
import type { VirtualTimeResponse } from '../types/api';

function formatDuration(totalSeconds: number): string {
  const seconds = Math.max(0, Math.floor(totalSeconds));
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const remainingSeconds = seconds % 60;

  const parts = [];
  if (hours > 0) parts.push(`${hours}h`);
  if (minutes > 0 || hours > 0) parts.push(`${minutes}m`);
  parts.push(`${remainingSeconds}s`);
  return parts.join(' ');
}

function formatDisplayTime(value: string): string {
  return value.replace('T', ' ');
}

const VirtualTimePanel: FC = () => {
  const [data, setData] = useState<VirtualTimeResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;

    const load = async () => {
      try {
        const response = await fetchVirtualTime();
        if (!active) {
          return;
        }
        setData(response);
        setError(null);
      } catch (err: unknown) {
        if (!active) {
          return;
        }
        setError(err instanceof Error ? err.message : 'Failed to load virtual time');
        setData(null);
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    };

    void load();
    const intervalId = window.setInterval(() => {
      void load();
    }, 5000);

    return () => {
      active = false;
      window.clearInterval(intervalId);
    };
  }, []);

  return (
    <div className="panel panel-soft">
      <div className="panel-header">
        <span className="panel-icon">⏱</span>
        <div>
          <h2 className="panel-title">Virtual Time</h2>
          <p className="panel-description">Live snapshot from <code>GET /api/virtual-time</code>.</p>
        </div>
        <span className="status-pill">
          <span className="status-dot" />
          Live
        </span>
      </div>

      {loading && !data && (
        <div className="loading-state">
          <div className="spinner" />
          <span>Loading virtual time…</span>
        </div>
      )}

      {error && !data && (
        <div className="error-state">
          <span className="error-icon">⚠️</span>
          <span>{error}</span>
        </div>
      )}

      {data && (
        <div className="vt-grid">
          <div className="metric-card metric-strong">
            <span className="metric-label">Current Virtual Time</span>
            <strong className="metric-value">{formatDisplayTime(data.virtual_time)}</strong>
          </div>
          <div className="metric-card">
            <span className="metric-label">Reference Time</span>
            <strong className="metric-value">{formatDisplayTime(data.reference_time)}</strong>
          </div>
          <div className="metric-card">
            <span className="metric-label">Elapsed Real Time</span>
            <strong className="metric-value">{formatDuration(data.real_time_elapsed_seconds)}</strong>
          </div>
          <div className="metric-card">
            <span className="metric-label">Batch Increment</span>
            <strong className="metric-value">+{data.batch_flush_increment_seconds} seconds</strong>
          </div>
        </div>
      )}
    </div>
  );
};

export default VirtualTimePanel;
