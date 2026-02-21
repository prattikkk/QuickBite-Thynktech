/**
 * Admin Health Dashboard — Phase 2 Observability
 * Displays operational metrics, DB pool stats, webhook queue depths,
 * feature flags, and business counters.
 */

import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { adminService, HealthSummary } from '../services/admin.service';

export default function AdminHealth() {
  const [data, setData] = useState<HealthSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [toggling, setToggling] = useState<string | null>(null);
  const [orderLookupId, setOrderLookupId] = useState('');
  const navigate = useNavigate();

  const fetchData = async () => {
    try {
      setLoading(true);
      const summary = await adminService.getHealthSummary();
      setData(summary);
      setError(null);
    } catch (err: any) {
      setError(err.message || 'Failed to load health summary');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, 15_000); // Auto-refresh every 15s
    return () => clearInterval(interval);
  }, []);

  const handleToggle = async (key: string, currentValue: boolean) => {
    setToggling(key);
    try {
      await adminService.toggleFeatureFlag(key, !currentValue);
      await fetchData();
    } catch (err: any) {
      setError(err.message || 'Failed to toggle flag');
    } finally {
      setToggling(null);
    }
  };

  if (loading && !data) {
    return (
      <div className="max-w-5xl mx-auto p-6">
        <h1 className="text-2xl font-bold mb-4">Admin Health Dashboard</h1>
        <p className="text-gray-500">Loading...</p>
      </div>
    );
  }

  if (error && !data) {
    return (
      <div className="max-w-5xl mx-auto p-6">
        <h1 className="text-2xl font-bold mb-4">Admin Health Dashboard</h1>
        <p className="text-red-600">{error}</p>
        <button onClick={fetchData} className="mt-2 text-blue-600 underline">Retry</button>
      </div>
    );
  }

  if (!data) return null;

  return (
    <div className="max-w-5xl mx-auto p-6 space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold">Admin Health Dashboard</h1>
        <button
          onClick={fetchData}
          className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 text-sm"
        >
          Refresh
        </button>
      </div>

      {error && <p className="text-red-500 text-sm">{error}</p>}

      {/* DB Pool Stats */}
      <section className="bg-white rounded-lg shadow p-4">
        <h2 className="text-lg font-semibold mb-3">Database Connection Pool</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <MetricCard label="Active" value={data.dbPool?.activeConnections ?? '-'} color="blue" />
          <MetricCard label="Idle" value={data.dbPool?.idleConnections ?? '-'} color="green" />
          <MetricCard label="Total" value={data.dbPool?.totalConnections ?? '-'} color="gray" />
          <MetricCard label="Waiting" value={data.dbPool?.threadsAwaitingConnection ?? '-'} color="yellow" />
        </div>
      </section>

      {/* Webhook Queue */}
      <section className="bg-white rounded-lg shadow p-4">
        <h2 className="text-lg font-semibold mb-3">Webhook Queue</h2>
        <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
          <MetricCard label="Pending" value={data.webhooks?.pendingCount ?? 0} color="yellow" />
          <MetricCard label="Dead Letter Queue" value={data.webhooks?.dlqCount ?? 0} color="red" />
          <MetricCard label="Timeline Events" value={data.totalTimelineEvents ?? 0} color="gray" />
        </div>
      </section>

      {/* Business Metrics */}
      <section className="bg-white rounded-lg shadow p-4">
        <h2 className="text-lg font-semibold mb-3">Business Metrics (since boot)</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <MetricCard label="Orders Created" value={data.metrics?.ordersCreated ?? 0} color="green" />
          <MetricCard label="Transitions" value={data.metrics?.orderTransitions ?? 0} color="blue" />
          <MetricCard label="Payment Intents" value={data.metrics?.paymentIntentsCreated ?? 0} color="blue" />
          <MetricCard label="Payment Success" value={data.metrics?.paymentSuccess ?? 0} color="green" />
          <MetricCard label="Payment Failed" value={data.metrics?.paymentFailed ?? 0} color="red" />
          <MetricCard label="Webhooks OK" value={data.metrics?.webhooksProcessed ?? 0} color="green" />
          <MetricCard label="Webhooks Failed" value={data.metrics?.webhooksFailed ?? 0} color="red" />
          <MetricCard label="Webhooks DLQ" value={data.metrics?.webhooksDlq ?? 0} color="red" />
        </div>
      </section>

      {/* Feature Flags */}
      <section className="bg-white rounded-lg shadow p-4">
        <h2 className="text-lg font-semibold mb-3">Feature Flags</h2>
        <div className="space-y-2">
          {data.featureFlags && Object.entries(data.featureFlags).map(([key, enabled]) => (
            <div key={key} className="flex items-center justify-between border-b pb-2">
              <span className="font-mono text-sm">{key}</span>
              <button
                onClick={() => handleToggle(key, enabled)}
                disabled={toggling === key}
                className={`px-3 py-1 rounded text-sm font-medium ${
                  enabled
                    ? 'bg-green-100 text-green-800 hover:bg-green-200'
                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                } ${toggling === key ? 'opacity-50 cursor-wait' : ''}`}
              >
                {toggling === key ? '...' : enabled ? 'ON' : 'OFF'}
              </button>
            </div>
          ))}
        </div>
      </section>

      {/* Order Lookup */}
      <section className="bg-white rounded-lg shadow p-4">
        <h2 className="text-lg font-semibold mb-3">Order Timeline Lookup</h2>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            const trimmed = orderLookupId.trim();
            if (trimmed) navigate(`/admin/orders/${trimmed}/timeline`);
          }}
          className="flex gap-2"
        >
          <input
            type="text"
            placeholder="Enter Order ID (UUID)"
            value={orderLookupId}
            onChange={(e) => setOrderLookupId(e.target.value)}
            className="flex-1 border border-gray-300 rounded px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          />
          <button
            type="submit"
            className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 text-sm whitespace-nowrap"
          >
            View Timeline
          </button>
        </form>
      </section>
    </div>
  );
}

/* ─── Small helper component ─── */
function MetricCard({ label, value, color }: { label: string; value: number | string; color: string }) {
  const colorMap: Record<string, string> = {
    blue: 'bg-blue-50 text-blue-700',
    green: 'bg-green-50 text-green-700',
    red: 'bg-red-50 text-red-700',
    yellow: 'bg-yellow-50 text-yellow-700',
    gray: 'bg-gray-50 text-gray-700',
  };

  return (
    <div className={`rounded-lg p-3 ${colorMap[color] || colorMap.gray}`}>
      <div className="text-2xl font-bold">{value}</div>
      <div className="text-xs mt-1">{label}</div>
    </div>
  );
}
