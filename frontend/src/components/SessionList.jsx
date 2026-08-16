import React, { useState } from 'react';
import { Clock, CheckCircle2, AlertTriangle, XCircle, Search } from 'lucide-react';

export default function SessionList({ sessions, loading }) {
  const [filterStatus, setFilterStatus] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  const filteredSessions = sessions.filter(session => {
    const matchesStatus = filterStatus === 'ALL' || session.status.toUpperCase() === filterStatus;
    const matchesSearch = session.userId.toLowerCase().includes(searchQuery.toLowerCase()) ||
                          session.sessionId.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesStatus && matchesSearch;
  });

  const getStatusBadge = (status) => {
    const s = status ? status.toLowerCase() : '';
    if (s === 'valid') {
      return <span className="badge badge-valid"><CheckCircle2 size={12} /> Valid</span>;
    }
    if (s === 'conflict_resolved') {
      return <span className="badge badge-resolved"><AlertTriangle size={12} /> Conflict Resolved</span>;
    }
    if (s === 'pending') {
      return <span className="badge badge-pending"><Clock size={12} /> Pending</span>;
    }
    return <span className="badge badge-invalid"><XCircle size={12} /> Invalid</span>;
  };


  const formatDate = (isoString) => {
    if (!isoString) return <span style={{ color: 'var(--text-dim)', fontStyle: 'italic' }}>Pending</span>;
    try {
      const date = new Date(isoString);
      return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }) + ' (' + date.toLocaleDateString() + ')';
    } catch (e) {
      return isoString;
    }
  };

  return (
    <div style={{ padding: '0 24px 24px 24px' }}>
      
      {/* Controls Bar */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', flexWrap: 'wrap', gap: '12px' }}>
        <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
          <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Status Filter:</span>
          {['ALL', 'VALID', 'CONFLICT_RESOLVED', 'PENDING', 'INVALID'].map(status => (
            <button
              key={status}
              className={`btn ${filterStatus === status ? 'btn-primary' : 'btn-secondary'}`}
              onClick={() => setFilterStatus(status)}
              style={{ fontSize: '0.75rem', padding: '4px 10px' }}
            >
              {status}
            </button>
          ))}
        </div>

        <div style={{ position: 'relative', width: '260px' }}>
          <Search size={16} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
          <input
            type="text"
            placeholder="Search User or Session ID..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{
              width: '100%', padding: '8px 12px 8px 36px',
              borderRadius: '10px', background: 'rgba(0,0,0,0.3)',
              border: '1px solid var(--border-glass)', color: 'var(--text-main)',
              fontSize: '0.85rem'
            }}
          />
        </div>
      </div>

      {/* Sessions Grid */}
      {loading ? (
        <div className="glass-panel" style={{ padding: '40px', textAlign: 'center', color: 'var(--text-muted)' }}>
          Loading attendance sessions...
        </div>
      ) : filteredSessions.length === 0 ? (
        <div className="glass-panel" style={{ padding: '40px', textAlign: 'center', color: 'var(--text-muted)' }}>
          No attendance sessions recorded yet. Use the <strong>Ingest Event</strong> button or <strong>Edge Case Simulator</strong> to generate sessions.
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(340px, 1fr))', gap: '16px' }}>
          {filteredSessions.map((session, idx) => (
            <div key={idx} className="glass-panel" style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '14px' }}>
              
              {/* Card Header */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div>
                  <h3 style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--text-main)' }}>
                    {session.userId}
                  </h3>
                  <span style={{ fontSize: '0.75rem', fontFamily: 'var(--font-mono)', color: 'var(--cyan)' }}>
                    {session.sessionId}
                  </span>
                </div>
                {getStatusBadge(session.status)}
              </div>

              {/* Timeline Info */}
              <div style={{ background: 'rgba(0,0,0,0.2)', borderRadius: '10px', padding: '12px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.8rem' }}>
                  <Clock size={14} color="var(--emerald)" />
                  <span style={{ color: 'var(--text-muted)' }}>Check-in:</span>
                  <span style={{ fontWeight: 600 }}>{formatDate(session.checkinTime)}</span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.8rem' }}>
                  <Clock size={14} color="var(--rose)" />
                  <span style={{ color: 'var(--text-muted)' }}>Checkout:</span>
                  <span style={{ fontWeight: 600 }}>{formatDate(session.checkoutTime)}</span>
                </div>
              </div>

            </div>
          ))}
        </div>
      )}
    </div>
  );
}
