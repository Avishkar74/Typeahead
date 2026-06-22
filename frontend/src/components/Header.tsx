import type { FC } from 'react';

const Header: FC = () => (
  <header className="app-header">
    <div className="header-inner">
      <div className="header-brand">
        <div>
          <h1 className="header-title">Search Typeahead System</h1>
          <p className="header-subtitle">High-performance distributed search with Redis consistent hashing</p>
        </div>
      </div>
    </div>
  </header>
);

export default Header;
