import React, { useState, useEffect } from 'react';
import Navbar from './components/Navbar';
import SessionList from './components/SessionList';
import AuditTrail from './components/AuditTrail';
import IdentityManager from './components/IdentityManager';
import FixtureSimulator from './components/FixtureSimulator';
import EventModal from './components/EventModal';

export default function App() {
  const [activeTab, setActiveTab] = useState('sessions');
  const [sessions, setSessions] = useState([]);
  const [auditLogs, setAuditLogs] = useState([]);
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [resSessions, resAudit, resUsers] = await Promise.all([
        fetch('/api/sessions').catch(() => ({ ok: false })),
        fetch('/api/audit').catch(() => ({ ok: false })),
        fetch('/api/users').catch(() => ({ ok: false }))
      ]);

      if (resSessions.ok) setSessions(await resSessions.json());
      if (resAudit.ok) setAuditLogs(await resAudit.json());
      if (resUsers.ok) setUsers(await resUsers.json());
    } catch (e) {
      console.error('Failed to fetch data from backend API:', e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleReset = async () => {
    if (window.confirm('Are you sure you want to reset all attendance sessions, logged events, and audit logs?')) {
      try {
        await fetch('/api/reset', { method: 'POST' });
        fetchData();
      } catch (e) {
        alert('Failed to reset system state.');
      }
    }
  };

  const resolvedCount = (sessions || []).filter(s => s && s.status === 'conflict_resolved').length;
  const pendingUsersCount = (users || []).filter(u => u && (u.status === 'PENDING' || u.status === 'pending')).length;


  const stats = {
    sessionsCount: sessions.length,
    resolvedCount: resolvedCount,
    pendingCount: pendingUsersCount,
    auditCount: auditLogs.length,
    usersCount: users.length
  };

  return (
    <div style={{ minHeight: '100vh', paddingBottom: '40px' }}>
      <Navbar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        stats={stats}
        onReset={handleReset}
        onOpenIngestModal={() => setIsModalOpen(true)}
      />

      <main>
        {activeTab === 'sessions' && <SessionList sessions={sessions} loading={loading} />}
        {activeTab === 'audit' && <AuditTrail auditLogs={auditLogs} />}
        {activeTab === 'users' && <IdentityManager users={users} />}
        {activeTab === 'simulator' && <FixtureSimulator onRefreshAll={fetchData} />}
      </main>

      <EventModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onEventIngested={fetchData}
      />
    </div>
  );
}
