import React from 'react';
import { Activity, ShieldAlert, Users, Play, PlusCircle, RefreshCw } from 'lucide-react';

export default function Navbar({ activeTab, setActiveTab, stats, onReset, onOpenIngestModal }) {
  return (
    <header className="glass-panel" style={{ margin: '16px 24px', padding: '16px 24px' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
        
        {/* Brand Title */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <div style={{
            width: '42px', height: '42px', borderRadius: '12px',
            background: 'linear-gradient(135deg, #6366f1, #06b6d4)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            boxShadow: '0 0 16px rgba(99, 102, 241, 0.4)'
          }}>
            <Activity size={24} color="#ffffff" />
          </div>
          <div>
            <h1 style={{ fontSize: '1.25rem', fontWeight: 800, letterSpacing: '-0.02em', background: 'linear-gradient(to right, #ffffff, #9ca3af)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
              Attendance Engine
            </h1>
            <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '6px' }}>
              <span className="pulse-dot pulse-green"></span> Temporal Replay & Identity Resolution
            </p>
          </div>
        </div>

        {/* Live System Stats */}
        <div style={{ display: 'flex', gap: '12px', background: 'rgba(0,0,0,0.2)', padding: '6px 12px', borderRadius: '12px', border: '1px solid var(--border-glass)' }}>
          <div style={{ textAlign: 'center', padding: '0 8px' }}>
            <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)', display: 'block' }}>SESSIONS</span>
            <span style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--text-main)' }}>{stats.sessionsCount}</span>
          </div>
          <div style={{ width: '1px', background: 'var(--border-glass)' }}></div>
          <div style={{ textAlign: 'center', padding: '0 8px' }}>
            <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)', display: 'block' }}>RESOLVED</span>
            <span style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--amber)' }}>{stats.resolvedCount}</span>
          </div>
          <div style={{ width: '1px', background: 'var(--border-glass)' }}></div>
          <div style={{ textAlign: 'center', padding: '0 8px' }}>
            <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)', display: 'block' }}>PENDING USERS</span>
            <span style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--primary)' }}>{stats.pendingCount}</span>
          </div>
          <div style={{ width: '1px', background: 'var(--border-glass)' }}></div>
          <div style={{ textAlign: 'center', padding: '0 8px' }}>
            <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)', display: 'block' }}>AUDIT LOGS</span>
            <span style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--cyan)' }}>{stats.auditCount}</span>
          </div>
        </div>

        {/* Action Controls */}
        <div style={{ display: 'flex', gap: '10px' }}>
          <button className="btn btn-primary" onClick={onOpenIngestModal}>
            <PlusCircle size={16} /> Ingest Event
          </button>
          <button className="btn btn-secondary" onClick={onReset} title="Reset system state">
            <RefreshCw size={16} /> Clear State
          </button>
        </div>

      </div>

      {/* Navigation Tabs */}
      <nav style={{ display: 'flex', gap: '8px', marginTop: '20px', borderTop: '1px solid var(--border-glass)', paddingTop: '16px' }}>
        <button
          className={`btn ${activeTab === 'sessions' ? 'btn-primary' : 'btn-secondary'}`}
          onClick={() => setActiveTab('sessions')}
        >
          <Activity size={16} /> Live Sessions
        </button>
        <button
          className={`btn ${activeTab === 'audit' ? 'btn-primary' : 'btn-secondary'}`}
          onClick={() => setActiveTab('audit')}
        >
          <ShieldAlert size={16} /> Audit Trail ({stats.auditCount})
        </button>
        <button
          className={`btn ${activeTab === 'users' ? 'btn-primary' : 'btn-secondary'}`}
          onClick={() => setActiveTab('users')}
        >
          <Users size={16} /> Identity Registry ({stats.usersCount})
        </button>
        <button
          className={`btn ${activeTab === 'simulator' ? 'btn-primary' : 'btn-secondary'}`}
          onClick={() => setActiveTab('simulator')}
        >
          <Play size={16} /> Edge Case Simulator
        </button>
      </nav>
    </header>
  );
}
