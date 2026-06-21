import type { FC } from 'react';

const Header: FC = () => (
  <header className="app-header">
    <div className="header-inner">
      <div className="header-brand">
        <span className="header-icon">⚡</span>
        <div>
          <h1 className="header-title">Search Typeahead System</h1>
          <p className="header-subtitle">High-performance distributed search with Redis consistent hashing</p>
        </div>
      </div>
      <div className="header-badges">
        <span className="badge badge-blue">React + Vite</span>
        <span className="badge badge-green">Spring Boot</span>
        <span className="badge badge-red">Redis × 3</span>
        <span className="badge badge-purple">PostgreSQL</span>
      </div>
    </div>
  </header>
);

export default Header;
