/**
 * VendorAnalytics — analytics dashboard tab for vendor.
 * Phase 4.2: Revenue, order volume, top items, CSV export.
 */

import { useState, useEffect } from 'react';
import { analyticsService } from '../services/analytics.service';
import type { VendorAnalytics as VendorAnalyticsType } from '../types/phase4.types';

interface VendorAnalyticsProps {
  vendorId: string;
}

export default function VendorAnalytics({ vendorId }: VendorAnalyticsProps) {
  const [analytics, setAnalytics] = useState<VendorAnalyticsType | null>(null);
  const [period, setPeriod] = useState<'daily' | 'weekly' | 'monthly'>('weekly');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    analyticsService
      .getVendorAnalytics(vendorId, period)
      .then(setAnalytics)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [vendorId, period]);

  const handleExport = async () => {
    try {
      const blob = await analyticsService.exportVendorCsv(vendorId, period);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `vendor-analytics-${period}.csv`;
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      // silent
    }
  };

  if (loading) {
    return <div className="flex justify-center py-12"><div className="animate-spin h-8 w-8 border-b-2 border-primary-600 rounded-full" /></div>;
  }

  if (!analytics) {
    return <p className="text-gray-500 text-center py-8">No analytics data available yet.</p>;
  }

  return (
    <div className="space-y-6">
      {/* Period Selector + Export */}
      <div className="flex items-center justify-between">
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
        <button
          onClick={handleExport}
          className="px-4 py-2 text-sm font-medium text-primary-600 border border-primary-600 rounded-lg hover:bg-primary-50"
        >
          Export CSV
        </button>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-white rounded-lg shadow p-4">
          <p className="text-sm text-gray-500">Total Orders</p>
          <p className="text-2xl font-bold text-gray-900">{analytics.totalOrders}</p>
        </div>
        <div className="bg-white rounded-lg shadow p-4">
          <p className="text-sm text-gray-500">Revenue</p>
          <p className="text-2xl font-bold text-gray-900">
            ${(analytics.totalRevenueCents / 100).toFixed(2)}
          </p>
        </div>
        <div className="bg-white rounded-lg shadow p-4">
          <p className="text-sm text-gray-500">Avg Prep Time</p>
          <p className="text-2xl font-bold text-gray-900">{analytics.averagePrepTime} min</p>
        </div>
        <div className="bg-white rounded-lg shadow p-4">
          <p className="text-sm text-gray-500">Cancellation Rate</p>
          <p className="text-2xl font-bold text-gray-900">{(analytics.cancellationRate * 100).toFixed(1)}%</p>
        </div>
      </div>

      {/* Top Items */}
      {analytics.topItems && analytics.topItems.length > 0 && (
        <div className="bg-white rounded-lg shadow p-4">
          <h3 className="font-semibold text-gray-900 mb-3">Top Items</h3>
          <div className="space-y-2">
            {analytics.topItems.slice(0, 5).map((item, i) => (
              <div key={i} className="flex items-center justify-between py-2 border-b last:border-0">
                <div className="flex items-center gap-3">
                  <span className="text-sm font-medium text-gray-500 w-6">{i + 1}.</span>
                  <span className="text-sm text-gray-900">{item.name}</span>
                </div>
                <div className="flex items-center gap-4">
                  <span className="text-sm text-gray-500">{item.count} orders</span>
                  <span className="text-sm font-medium text-gray-900">${(item.revenue / 100).toFixed(2)}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Orders by Day chart (simple bar representation) */}
      {analytics.ordersByDay && analytics.ordersByDay.length > 0 && (
        <div className="bg-white rounded-lg shadow p-4">
          <h3 className="font-semibold text-gray-900 mb-3">Orders Over Time</h3>
          <div className="flex items-end gap-1 h-40">
            {analytics.ordersByDay.map((day, i) => {
              const maxCount = Math.max(...analytics.ordersByDay.map((d) => d.count), 1);
              const height = (day.count / maxCount) * 100;
              return (
                <div key={i} className="flex-1 flex flex-col items-center">
                  <div
                    className="w-full bg-primary-500 rounded-t"
                    style={{ height: `${height}%`, minHeight: day.count > 0 ? '4px' : '0' }}
                    title={`${day.date}: ${day.count} orders`}
                  />
                  <span className="text-xs text-gray-400 mt-1 truncate w-full text-center">
                    {day.date.slice(5)}
                  </span>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
