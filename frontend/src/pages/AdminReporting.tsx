/**
 * AdminReporting — platform KPI dashboard for admin.
 * Phase 4.3
 */

import { useState, useEffect } from 'react';
import { analyticsService } from '../services/analytics.service';
import type { PlatformKpis } from '../types/phase4.types';

export default function AdminReporting() {
  const [kpis, setKpis] = useState<PlatformKpis | null>(null);
  const [period, setPeriod] = useState<'daily' | 'weekly' | 'monthly'>('monthly');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    analyticsService
      .getPlatformKpis(period)
      .then(setKpis)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [period]);

  const handleExport = async (type: string) => {
    try {
      const blob = await analyticsService.exportAdminCsv(type, period);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${type}-report-${period}.csv`;
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      // silent
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center py-12">
        <div className="animate-spin h-8 w-8 border-b-2 border-primary-600 rounded-full" />
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-2xl font-bold text-gray-900">Platform Reports</h1>
        <div className="flex gap-2">
          {(['daily', 'weekly', 'monthly'] as const).map((p) => (
            <button
              key={p}
              onClick={() => setPeriod(p)}
              className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                period === p ? 'bg-primary-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              {p.charAt(0).toUpperCase() + p.slice(1)}
            </button>
          ))}
        </div>
      </div>

      {kpis && (
        <>
          {/* KPI Cards */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
            <KpiCard label="Total Orders" value={kpis.totalOrders.toString()} />
            <KpiCard label="Revenue" value={`$${(kpis.totalRevenueCents / 100).toFixed(2)}`} />
            <KpiCard label="Customers" value={kpis.totalCustomers.toString()} />
            <KpiCard label="Vendors" value={kpis.totalVendors.toString()} />
            <KpiCard label="Drivers" value={kpis.totalDrivers.toString()} />
            <KpiCard label="Avg Delivery" value={`${kpis.averageDeliveryTime} min`} />
            <KpiCard label="Repeat Rate" value={`${(kpis.repeatOrderRate * 100).toFixed(1)}%`} />
          </div>

          {/* Export buttons */}
          <div className="flex gap-3">
            <button
              onClick={() => handleExport('revenue')}
              className="px-4 py-2 text-sm font-medium text-white bg-primary-600 rounded-lg hover:bg-primary-700"
            >
              Export Revenue CSV
            </button>
            <button
              onClick={() => handleExport('orders')}
              className="px-4 py-2 text-sm font-medium text-primary-600 border border-primary-600 rounded-lg hover:bg-primary-50"
            >
              Export Orders CSV
            </button>
          </div>
        </>
      )}
    </div>
  );
}

function KpiCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="bg-white rounded-lg shadow p-4">
      <p className="text-sm text-gray-500">{label}</p>
      <p className="text-2xl font-bold text-gray-900">{value}</p>
    </div>
  );
}
