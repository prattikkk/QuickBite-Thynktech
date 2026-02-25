/**
 * Skeleton — loading placeholder component
 * Variants: text, rect, circle for different content layouts
 */

interface SkeletonProps {
  variant?: 'text' | 'rect' | 'circle';
  width?: string;
  height?: string;
  className?: string;
  count?: number;
}

export default function Skeleton({
  variant = 'text',
  width,
  height,
  className = '',
  count = 1,
}: SkeletonProps) {
  const baseClasses = 'animate-pulse bg-gray-200 rounded';

  const variantClasses = {
    text: 'h-4 rounded',
    rect: 'rounded-lg',
    circle: 'rounded-full',
  };

  const style: React.CSSProperties = {};
  if (width) style.width = width;
  if (height) style.height = height;
  if (variant === 'circle' && !height) style.height = width;

  return (
    <>
      {Array.from({ length: count }, (_, i) => (
        <div
          key={i}
          className={`${baseClasses} ${variantClasses[variant]} ${className}`}
          style={style}
          role="status"
          aria-label="Loading"
        />
      ))}
    </>
  );
}

/** Pre-built skeleton patterns for common layouts */
export function SkeletonCard() {
  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-4 space-y-3">
      <Skeleton variant="rect" height="160px" className="w-full" />
      <Skeleton width="60%" />
      <Skeleton width="40%" />
      <Skeleton width="30%" />
    </div>
  );
}

export function SkeletonTable({ rows = 5, cols = 4 }: { rows?: number; cols?: number }) {
  return (
    <div className="space-y-3">
      <div className="flex gap-4">
        {Array.from({ length: cols }, (_, i) => (
          <Skeleton key={i} width={`${100 / cols}%`} height="16px" />
        ))}
      </div>
      {Array.from({ length: rows }, (_, r) => (
        <div key={r} className="flex gap-4">
          {Array.from({ length: cols }, (_, c) => (
            <Skeleton key={c} width={`${100 / cols}%`} height="14px" />
          ))}
        </div>
      ))}
    </div>
  );
}

export function SkeletonKpiCards({ count = 4 }: { count?: number }) {
  return (
    <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
      {Array.from({ length: count }, (_, i) => (
        <div key={i} className="bg-white rounded-lg shadow p-4 space-y-2">
          <Skeleton width="60%" height="12px" />
          <Skeleton width="40%" height="28px" />
        </div>
      ))}
    </div>
  );
}
