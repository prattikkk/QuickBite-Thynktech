/**
 * AdminReporting — platform KPI dashboard for admin.
 * Phase 4.3 — upgraded with revenue chart + delivery time breakdown
 */

import { useState, useEffect } from 'react';
import { analyticsService } from '../services/analytics.service';
import type { PlatformKpis } from '../types/phase4.types';
import Breadcrumbs from '../components/Breadcrumbs';
import { SkeletonKpiCards } from '../components/Skeleton';

export default function AdminReporting() {
  const [kpis, setKpis] = useState<PlatformKpis | null>(null);
  const [period, setPeriod] = useState<'daily' | 'weekly' | 'monthly'>('monthly');
  const [loading, setLoading] = useState(true);
  const [revenueData, setRevenueData] = useState<Record<string, unknown>[]>([]);
  const [deliveryData, setDeliveryData] = useState<Record<string, unknown> | null>(null);
  const [revenueLoading, setRevenueLoading] = useState(false);
  const [deliveryLoading, setDeliveryLoading] = useState(false);

  useEffect(() => {
    setLoading(true);
    analyticsService
      .getPlatformKpis(period)
      .then(setKpis)
      .catch(() => {})
      .finally(() => setLoading(false));

    // Load revenue report
    setRevenueLoading(true);
    analyticsService
      .getRevenueReport(period)
      .then((data) => setRevenueData(Array.isArray(data) ? data : []))
      .catch(() => setRevenueData([]))
      .finally(() => setRevenueLoading(false));

    // Load delivery times
    setDeliveryLoading(true);
    analyticsService
      .getDeliveryTimeReport()
      .then(setDeliveryData)
      .catch(() => setDeliveryData(null))
      .finally(() => setDeliveryLoading(false));
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

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <Breadcrumbs items={[{ label: 'Admin', to: '/admin/health' }, { label: 'Reports' }]} />

      <div className="flex items-center justify-between mb-8">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Platform Reports</h1>
        <div className="flex gap-2">
          {(['daily', 'weekly', 'monthly'] as const).map((p) => (
            <button
              key={p}
              onClick={() => setPeriod(p)}
              className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                period === p ? 'bg-primary-600 text-white' : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-200 hover:bg-gray-200 dark:hover:bg-gray-600'
              }`}
            >
              {p.charAt(0).toUpperCase() + p.slice(1)}
            </button>
          ))}
        </div>
      </div>

      {loading ? (
        <SkeletonKpiCards count={7} />
      ) : kpis ? (
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

          {/* Revenue Chart Section */}
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 mb-8">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Revenue Trend</h2>
            {revenueLoading ? (
              <div className="flex justify-center py-8">
                <div className="animate-spin h-6 w-6 border-b-2 border-primary-600 rounded-full" />
              </div>
            ) : revenueData.length > 0 ? (
              <div className="flex items-end gap-1 h-48">
                {revenueData.map((entry, i) => {
                  const revenue = Number((entry as any).revenueCents ?? (entry as any).totalRevenueCents ?? (entry as any).revenue ?? 0);
                  const maxRevenue = Math.max(...revenueData.map((d) => Number((d as any).revenueCents ?? (d as any).totalRevenueCents ?? (d as any).revenue ?? 0)), 1);
                  const height = (revenue / maxRevenue) * 100;
                  const label = String((entry as any).date ?? (entry as any).period ?? `P${i + 1}`);
                  return (
                    <div key={i} className="flex-1 flex flex-col items-center">
                      <div
                        className="w-full bg-primary-500 rounded-t transition-all duration-300"
                        style={{ height: `${height}%`, minHeight: revenue > 0 ? '4px' : '0' }}
                        title={`${label}: $${(revenue / 100).toFixed(2)}`}
                      />
                      <span className="text-xs text-gray-400 dark:text-gray-500 mt-1 truncate w-full text-center">
                        {label.length > 5 ? label.slice(5) : label}
                      </span>
                    </div>
                  );
                })}
              </div>
            ) : (
              <p className="text-gray-500 dark:text-gray-400 text-sm text-center py-4">No revenue data available for this period.</p>
            )}
          </div>

          {/* Delivery Time Section */}
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 mb-8">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Delivery Times</h2>
            {deliveryLoading ? (
              <div className="flex justify-center py-8">
                <div className="animate-spin h-6 w-6 border-b-2 border-primary-600 rounded-full" />
              </div>
            ) : deliveryData ? (
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <DeliveryMetric
                  label="Average"
                  value={`${(deliveryData as any).averageMinutes ?? (deliveryData as any).average ?? '—'} min`}
                />
                <DeliveryMetric
                  label="Median"
                  value={`${(deliveryData as any).medianMinutes ?? (deliveryData as any).median ?? '—'} min`}
                />
                <DeliveryMetric
                  label="P95"
                  value={`${(deliveryData as any).p95Minutes ?? (deliveryData as any).p95 ?? '—'} min`}
                />
                <DeliveryMetric
                  label="Total Deliveries"
                  value={String((deliveryData as any).totalDeliveries ?? (deliveryData as any).count ?? '—')}
                />
              </div>
            ) : (
              <p className="text-gray-500 dark:text-gray-400 text-sm text-center py-4">No delivery time data available.</p>
            )}
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
      ) : null}
    </div>
  );
}

function KpiCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4">
      <p className="text-sm text-gray-500 dark:text-gray-400">{label}</p>
      <p className="text-2xl font-bold text-gray-900 dark:text-white">{value}</p>
    </div>
  );
}

function DeliveryMetric({ label, value }: { label: string; value: string }) {
  return (
    <div className="bg-gray-50 dark:bg-gray-900 rounded-lg p-4 text-center">
      <p className="text-sm text-gray-500 dark:text-gray-400 mb-1">{label}</p>
      <p className="text-xl font-bold text-gray-900 dark:text-white">{value}</p>
    </div>
  );
}
