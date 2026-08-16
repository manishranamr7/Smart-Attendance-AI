import React from 'react';
import { Users, UserCheck, AlertCircle, ShieldAlert, Key } from 'lucide-react';

export default function IdentityManager({ users }) {
  return (
    <div style={{ padding: '0 24px 24px 24px' }}>
      <div style={{ marginBottom: '16px' }}>
        <h2 style={{ fontSize: '1.1rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Users color="var(--primary)" size={20} /> Biometric Identity & Enrollment Registry
        </h2>
        <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
          Identity resolution registry verifying face confidence scores (threshold ≥ 0.8), face templates, and 30-day enrollment windows.
        </p>
      </div>

      {users.length === 0 ? (
        <div className="glass-panel" style={{ padding: '40px', textAlign: 'center', color: 'var(--text-muted)' }}>
          No user enrollments found.
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '16px' }}>
          {users.map((user) => {
            const isPending = user.status && user.status.toUpperCase() === 'PENDING';
            const isExpired = user.status && user.status.toUpperCase() === 'EXPIRED_WINDOW';

            return (
              <div key={user.userId} className="glass-panel" style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <div>
                    <h3 style={{ fontSize: '1rem', fontWeight: 700 }}>{user.name}</h3>
                    <span style={{ fontSize: '0.75rem', fontFamily: 'var(--font-mono)', color: 'var(--cyan)' }}>
                      ID: {user.userId}
                    </span>
                  </div>
                  {isPending ? (
                    <span className="badge badge-pending">
                      <AlertCircle size={12} /> Pending Review
                    </span>
                  ) : isExpired ? (
                    <span className="badge badge-invalid">
                      <ShieldAlert size={12} /> Expired (&gt;30 Days)
                    </span>
                  ) : (
                    <span className="badge badge-valid">
                      <UserCheck size={12} /> Enrolled
                    </span>
                  )}
                </div>

                <div style={{ background: 'rgba(0,0,0,0.25)', padding: '10px 14px', borderRadius: '10px', display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                    <Key size={12} color="var(--amber)" /> Face Template Hash:
                  </div>
                  <span style={{ fontSize: '0.75rem', fontFamily: 'var(--font-mono)', color: 'var(--amber)', wordBreak: 'break-all' }}>
                    {user.faceTemplate || 'N/A'}
                  </span>
                </div>

                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                  Enrollment Date: <strong style={{ color: 'var(--text-main)' }}>{user.enrollmentDate ? new Date(user.enrollmentDate).toLocaleDateString() : 'N/A'}</strong>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
