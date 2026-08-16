import React, { useState } from 'react';
import { Play, RotateCcw, CheckCircle, AlertTriangle, ArrowRight, Code } from 'lucide-react';

const FIXTURES = [
  {
    id: '01_idempotent_duplicate_events.json',
    title: 'Fixture 1: Idempotent Event Replay',
    description: 'Submits duplicate events with identical event_id to verify system returns HTTP 200 without changing state.',
    badge: 'Idempotency'
  },
  {
    id: '02_out_of_order_checkout_before_checkin.json',
    title: 'Fixture 2: Out-of-Order Events (Checkout First)',
    description: 'Submits checkout event before checkin. Verifies state transitions from invalid to valid upon checkin arrival.',
    badge: 'Temporal Replay'
  },
  {
    id: '03_conflicting_sources_camera_vs_app.json',
    title: 'Fixture 3: Source Precedence Conflict (Camera vs App)',
    description: 'Submits checkin via App first, then Camera checkin. Verifies Camera overrides App deterministically.',
    badge: 'Conflict Resolution'
  },
  {
    id: '04_identity_ambiguity_and_pending_drift.json',
    title: 'Fixture 4: Low Face Confidence & Identity Ambiguity',
    description: 'Submits event with confidence < 0.8. Verifies identity resolution flags user as PENDING.',
    badge: 'Identity Resolution'
  },
  {
    id: '05_late_event_reconciliation.json',
    title: 'Fixture 5: Late Arriving Event Reconciliation',
    description: 'Submits checkout event, then a late checkin event arriving 2 hours later. Verifies state reconstruction.',
    badge: 'Reconciliation'
  }
];

export default function FixtureSimulator({ onRefreshAll }) {
  const [selectedFixture, setSelectedFixture] = useState(FIXTURES[0]);
  const [simulating, setSimulating] = useState(false);
  const [simulationLogs, setSimulationLogs] = useState([]);
  const [stepResults, setStepResults] = useState([]);

  const runFixture = async (fixture) => {
    setSimulating(true);
    setSimulationLogs([`[INIT] Loading fixture payload: ${fixture.id}...`]);
    setStepResults([]);

    try {
      // Fetch fixture json content cleanly
      let events = [];
      let res = await fetch(`/fixtures/${fixture.id}`);
      
      const contentType = res.headers.get('content-type');
      if (res.ok && contentType && contentType.includes('application/json')) {
        events = await res.json();
      } else {
        // Try backend endpoint fallback /api/fixtures/
        const apiRes = await fetch(`/api/fixtures/${fixture.id}`);
        const apiContentType = apiRes.headers.get('content-type');
        if (apiRes.ok && apiContentType && apiContentType.includes('application/json')) {
          events = await apiRes.json();
        } else {
          // Use structured default fixture definition
          events = getFallbackFixtureData(fixture.id);
        }
      }

      setSimulationLogs(prev => [...prev, `[LOADED] Found ${events.length} event(s) in fixture.`]);

      const results = [];
      for (let i = 0; i < events.length; i++) {
        const event = events[i];
        setSimulationLogs(prev => [...prev, `[STEP ${i + 1}] POST /events -> event_id: ${event.event_id} (${event.source} ${event.action})`]);

        const res = await fetch('/events', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(event)
        });

        const data = await res.json();
        results.push({ step: i + 1, event, responseStatus: res.status, responseData: data });

        setSimulationLogs(prev => [...prev, `[RESPONSE ${res.status}] Session State: status=${data.status || 'N/A'}`]);
        // Slight pause for visual feedback
        await new Promise(r => setTimeout(r, 400));
      }

      setStepResults(results);
      setSimulationLogs(prev => [...prev, `[SUCCESS] Fixture simulation complete!`]);
      if (onRefreshAll) onRefreshAll();
    } catch (err) {
      setSimulationLogs(prev => [...prev, `[ERROR] Simulation failed: ${err.message}`]);
    } finally {
      setSimulating(false);
    }
  };

  const getFallbackFixtureData = (id) => {
    if (id.includes('01')) {
      return [
        { event_id: 'EVT_DUP_001', user_id: 'USR_1001', source: 'camera', timestamp: '2026-08-16T08:00:00Z', action: 'checkin', face_confidence: 0.95, session_id: 'SESS_101', face_template: 'FT_JOHNDOE_V1' },
        { event_id: 'EVT_DUP_001', user_id: 'USR_1001', source: 'camera', timestamp: '2026-08-16T08:00:00Z', action: 'checkin', face_confidence: 0.95, session_id: 'SESS_101', face_template: 'FT_JOHNDOE_V1' }
      ];
    }
    if (id.includes('02')) {
      return [
        { event_id: 'EVT_OOO_OUT', user_id: 'USR_1002', source: 'camera', timestamp: '2026-08-16T17:00:00Z', action: 'checkout', face_confidence: 0.92, session_id: 'SESS_102', face_template: 'FT_JANESMITH_V1' },
        { event_id: 'EVT_OOO_IN', user_id: 'USR_1002', source: 'camera', timestamp: '2026-08-16T09:00:00Z', action: 'checkin', face_confidence: 0.96, session_id: 'SESS_102', face_template: 'FT_JANESMITH_V1' }
      ];
    }
    if (id.includes('03')) {
      return [
        { event_id: 'EVT_SRC_APP', user_id: 'USR_1001', source: 'app', timestamp: '2026-08-16T08:05:00Z', action: 'checkin', face_confidence: 0.85, session_id: 'SESS_103', face_template: 'FT_JOHNDOE_V1' },
        { event_id: 'EVT_SRC_CAM', user_id: 'USR_1001', source: 'camera', timestamp: '2026-08-16T08:00:00Z', action: 'checkin', face_confidence: 0.98, session_id: 'SESS_103', face_template: 'FT_JOHNDOE_V1' }
      ];
    }
    if (id.includes('04')) {
      return [
        { event_id: 'EVT_LOW_CONF', user_id: 'USR_9999_NEW', source: 'camera', timestamp: '2026-08-16T10:00:00Z', action: 'checkin', face_confidence: 0.65, session_id: 'SESS_104', face_template: 'FT_UNKNOWN' }
      ];
    }
    return [
      { event_id: 'EVT_LATE_OUT', user_id: 'USR_1004', source: 'app', timestamp: '2026-08-16T17:30:00Z', action: 'checkout', face_confidence: 0.88, session_id: 'SESS_105', face_template: 'FT_SARAHC_V1' },
      { event_id: 'EVT_LATE_IN', user_id: 'USR_1004', source: 'camera', timestamp: '2026-08-16T08:30:00Z', action: 'checkin', face_confidence: 0.96, session_id: 'SESS_105', face_template: 'FT_SARAHC_V1' }
    ];
  };

  return (
    <div style={{ padding: '0 24px 24px 24px' }}>
      <div style={{ marginBottom: '16px' }}>
        <h2 style={{ fontSize: '1.1rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Play color="var(--emerald)" size={20} /> Edge-Case Fixture & Temporal Replay Simulator
        </h2>
        <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
          Run interactive sample fixtures covering out-of-order events, duplicate event replay, source precedence, low confidence identity drift, and late arrival reconciliation.
        </p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '320px 1fr', gap: '20px' }}>
        
        {/* Fixture Selector Sidebar */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
          {FIXTURES.map(f => (
            <div
              key={f.id}
              className="glass-panel"
              onClick={() => setSelectedFixture(f)}
              style={{
                padding: '14px', cursor: 'pointer',
                borderColor: selectedFixture.id === f.id ? 'var(--primary)' : 'var(--border-glass)',
                background: selectedFixture.id === f.id ? 'rgba(99, 102, 241, 0.12)' : 'var(--bg-card)'
              }}
            >
              <span className="badge badge-pending" style={{ fontSize: '0.65rem', marginBottom: '6px' }}>
                {f.badge}
              </span>
              <h4 style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-main)' }}>{f.title}</h4>
              <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '4px' }}>{f.description}</p>
            </div>
          ))}
        </div>

        {/* Simulator Execution Panel */}
        <div className="glass-panel" style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '20px' }}>
          
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <h3 style={{ fontSize: '1.1rem', fontWeight: 700 }}>{selectedFixture.title}</h3>
              <span style={{ fontSize: '0.75rem', fontFamily: 'var(--font-mono)', color: 'var(--cyan)' }}>
                {selectedFixture.id}
              </span>
            </div>
            <button
              className="btn btn-primary"
              disabled={simulating}
              onClick={() => runFixture(selectedFixture)}
            >
              {simulating ? 'Simulating Replay...' : 'Execute Fixture Replay'} <ArrowRight size={16} />
            </button>
          </div>

          {/* Console Execution Output */}
          <div style={{ background: 'rgba(0,0,0,0.5)', padding: '16px', borderRadius: '12px', border: '1px solid var(--border-glass)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '10px', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
              <Code size={14} /> Execution Console & Event Replay Terminal
            </div>
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: '0.75rem', height: '160px', overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '4px' }}>
              {simulationLogs.length === 0 ? (
                <span style={{ color: 'var(--text-dim)' }}>Click "Execute Fixture Replay" to run simulation steps...</span>
              ) : (
                simulationLogs.map((log, i) => (
                  <div key={i} style={{
                    color: log.includes('[SUCCESS]') ? 'var(--emerald)' : log.includes('[ERROR]') ? 'var(--rose)' : log.includes('[STEP') ? 'var(--cyan)' : 'var(--text-main)'
                  }}>
                    {log}
                  </div>
                ))
              )}
            </div>
          </div>

          {/* Step Results Display */}
          {stepResults.length > 0 && (
            <div>
              <h4 style={{ fontSize: '0.85rem', fontWeight: 700, marginBottom: '10px' }}>Reconciliation State Timeline</h4>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                {stepResults.map(res => (
                  <div key={res.step} style={{ background: 'rgba(255,255,255,0.03)', padding: '12px', borderRadius: '8px', border: '1px solid var(--border-glass)' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', fontWeight: 600 }}>
                      <span>Step {res.step}: {res.event.source.toUpperCase()} {res.event.action.toUpperCase()} ({res.event.event_id})</span>
                      <span className={`badge ${res.responseData.status === 'valid' ? 'badge-valid' : res.responseData.status === 'conflict_resolved' ? 'badge-resolved' : res.responseData.status === 'pending' ? 'badge-pending' : 'badge-invalid'}`}>
                        {res.responseData.status}
                      </span>

                    </div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '6px' }}>
                      Session ID: <strong>{res.responseData.session_id}</strong> | Check-in: <strong>{res.responseData.checkin_time || 'N/A'}</strong> | Checkout: <strong>{res.responseData.checkout_time || 'N/A'}</strong>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

        </div>

      </div>
    </div>
  );
}
