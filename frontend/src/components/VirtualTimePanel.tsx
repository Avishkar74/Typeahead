import type { FC } from 'react';

/**
 * VirtualTimePanel — informational placeholder.
 *
 * The backend does not expose a REST endpoint for reading the virtual time
 * (no GET /api/virtual-time or equivalent controller exists).
 * Virtual time is an internal backend concept:
 *   - It starts from a reference date (2006-05-31) on application startup.
 *   - It advances in real elapsed time and by +60s on each batch flush.
 *   - It is persisted to PostgreSQL (system_config table) on shutdown.
 *
 * To expose virtual time to the UI a new backend endpoint would be required.
 * This component documents that dependency gap clearly.
 */
const VirtualTimePanel: FC = () => (
  <div className="panel panel-muted">
    <div className="panel-header">
      <span className="panel-icon">⏰</span>
      <h2 className="panel-title">Virtual Time</h2>
      <span className="badge badge-yellow">No backend endpoint</span>
    </div>
    <div className="vt-info">
      <p>
        The system uses a <strong>Virtual Time</strong> subsystem to simulate historical search data.
        Virtual time starts near <code>2006-05-31</code> and advances in real elapsed time,
        plus <strong>+60 seconds</strong> on every batch flush.
      </p>
      <div className="vt-facts">
        <div className="vt-fact">
          <span className="vt-fact-label">Reference date</span>
          <code className="vt-fact-value">2006-05-31T23:59:56</code>
        </div>
        <div className="vt-fact">
          <span className="vt-fact-label">Time advance per flush</span>
          <code className="vt-fact-value">+60 seconds</code>
        </div>
        <div className="vt-fact">
          <span className="vt-fact-label">Persistence</span>
          <code className="vt-fact-value">system_config table (PostgreSQL)</code>
        </div>
        <div className="vt-fact">
          <span className="vt-fact-label">Affects</span>
          <code className="vt-fact-value">search_logs.virtual_searched_at, daily/weekly counts</code>
        </div>
      </div>
      <p className="vt-note">
        ⚠️ <strong>Missing backend dependency:</strong> No <code>GET /api/virtual-time</code> endpoint
        is implemented on the backend. To display live virtual time here, a new controller
        exposing <code>VirtualTimeManager.getVirtualTime()</code> would need to be added.
      </p>
    </div>
  </div>
);

export default VirtualTimePanel;
