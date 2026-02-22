/**
 * OfflineBanner — a slim, fixed banner that appears when the browser
 * goes offline and disappears when connectivity returns.
 */

import { usePWA } from '../hooks/usePWA';

export default function OfflineBanner() {
  const { isOnline } = usePWA();

  if (isOnline) return null;

  return (
    <div className="fixed top-0 inset-x-0 z-[60] bg-red-600 text-white text-center text-sm py-1.5 shadow-md">
      <span className="inline-flex items-center gap-1.5">
        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M18.364 5.636a9 9 0 010 12.728M5.636 5.636a9 9 0 000 12.728m2.828-9.9a5 5 0 017.072 0m-9.9 2.828a5 5 0 010-7.072"
          />
        </svg>
        You are offline — some features may be limited
      </span>
    </div>
  );
}
