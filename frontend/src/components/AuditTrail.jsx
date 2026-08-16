import React, { useState } from 'react';
import { ShieldCheck, ChevronDown, ChevronRight, FileText, AlertTriangle } from 'lucide-react';

export default function AuditTrail({ auditLogs }) {
  const [expandedLogId, setExpandedLogId] = useState(null);

  const toggleExpand = (id) => {
    setExpandedLogId(expandedLogId === id ? null : id);
  };

  return (
    <div style={{ padding: '0 24px 24px 24px' }}>
      <div style={{ marginBottom: '16px' }}>
        <h2 style={{ fontSize: '1.1rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}>
          <ShieldCheck color="var(--cyan)" size={20} /> System Audit Trail & Reconciliation Log
        </h2>
        <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
          Complete, deterministic audit log for every attendance event ingestion, conflict resolution, and state transition.
        </p>
      </div>

      {auditLogs.length === 0 ? (
        <div className="glass-panel" style={{ padding: '40px', textAlign: 'center', color: 'var(--text-muted)' }}>
          No audit logs recorded yet. Ingest an event or run a fixture to inspect the audit log.
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {auditLogs.map((log) => {
            const isExpanded = expandedLogId === log.audit_id;
            const conflictsDetectedList = Array.isArray(log.conflicts_detected) ? log.conflicts_detected : [];
            const conflictsResolvedList = Array.isArray(log.conflicts_resolved) ? log.conflicts_resolved : [];
            const hasConflicts = conflictsDetectedList.length > 0;

            return (
              <div key={log.audit_id} className="glass-panel" style={{ padding: '16px', borderRadius: '12px' }}>
                
                {/* Audit Header Row */}
                <div
                  style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', cursor: 'pointer' }}
                  onClick={() => toggleExpand(log.audit_id)}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                    {isExpanded ? <ChevronDown size={18} color="var(--primary)" /> : <ChevronRight size={18} color="var(--text-muted)" />}
                    <div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <span style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-main)' }}>
                          Event: {log.event_id}
                        </span>
                        <span style={{ fontSize: '0.75rem', fontFamily: 'var(--font-mono)', color: 'var(--cyan)' }}>
                          User: {log.user_id}
                        </span>
                        <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)' }}>
                          ({log.session_id})
                        </span>
                      </div>
                      <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                        {log.decision}
                      </p>
                    </div>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                    {hasConflicts && (
                      <span className="badge badge-resolved" style={{ fontSize: '0.65rem' }}>
                        <AlertTriangle size={10} /> {conflictsDetectedList.length} Conflict(s)
                      </span>
                    )}
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)', fontFamily: 'var(--font-mono)' }}>
                      {log.timestamp ? new Date(log.timestamp).toLocaleTimeString() : ''}
                    </span>
                  </div>
                </div>

                {/* Expanded Details */}
                {isExpanded && (
                  <div style={{ marginTop: '16px', paddingTop: '16px', borderTop: '1px solid var(--border-glass)', display: 'flex', flexDirection: 'column', gap: '12px' }}>
                    
                    {/* Detected Conflicts */}
                    {conflictsDetectedList.length > 0 && (
                      <div style={{ background: 'rgba(245, 158, 11, 0.08)', border: '1px solid rgba(245, 158, 11, 0.2)', padding: '12px', borderRadius: '8px' }}>
                        <h4 style={{ fontSize: '0.75rem', color: 'var(--amber)', fontWeight: 700, marginBottom: '6px' }}>
                          Conflicts Flagged:
                        </h4>
                        <ul style={{ paddingLeft: '18px', fontSize: '0.75rem', color: 'var(--text-main)' }}>
                          {conflictsDetectedList.map((item, idx) => (
                            <li key={idx}>{item}</li>
                          ))}
                        </ul>
                      </div>
                    )}

                    {/* Resolved Actions */}
                    {conflictsResolvedList.length > 0 && (
                      <div style={{ background: 'rgba(16, 185, 129, 0.08)', border: '1px solid rgba(16, 185, 129, 0.2)', padding: '12px', borderRadius: '8px' }}>
                        <h4 style={{ fontSize: '0.75rem', color: 'var(--emerald)', fontWeight: 700, marginBottom: '6px' }}>
                          Resolutions Applied:
                        </h4>
                        <ul style={{ paddingLeft: '18px', fontSize: '0.75rem', color: 'var(--text-main)' }}>
                          {conflictsResolvedList.map((item, idx) => (
                            <li key={idx}>{item}</li>
                          ))}
                        </ul>
                      </div>
                    )}

                    {/* State Diff */}
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                      <div>
                        <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>
                          PREVIOUS STATE
                        </span>
                        <pre style={{
                          background: 'rgba(0,0,0,0.4)', padding: '10px', borderRadius: '8px',
                          fontSize: '0.7rem', fontFamily: 'var(--font-mono)', overflowX: 'auto',
                          color: '#9ca3af'
                        }}>
                          {typeof log.previous_state === 'object' ? JSON.stringify(log.previous_state, null, 2) : log.previous_state}
                        </pre>
                      </div>

                      <div>
                        <span style={{ fontSize: '0.7rem', color: 'var(--emerald)', display: 'block', marginBottom: '4px' }}>
                          NEW STATE
                        </span>
                        <pre style={{
                          background: 'rgba(0,0,0,0.4)', padding: '10px', borderRadius: '8px',
                          fontSize: '0.7rem', fontFamily: 'var(--font-mono)', overflowX: 'auto',
                          color: '#34d399'
                        }}>
                          {typeof log.new_state === 'object' ? JSON.stringify(log.new_state, null, 2) : log.new_state}
                        </pre>
                      </div>
                    </div>

                  </div>
                )}

              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
