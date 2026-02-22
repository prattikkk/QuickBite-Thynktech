/**
 * DeliveryProofDisplay — Phase 3: Shows proof-of-delivery info on order timeline.
 * Renders photo, OTP verification badge, notes, and GPS coordinates.
 */

import { useState, useEffect } from 'react';
import { orderService } from '../services/order.service';
import type { DeliveryProofDTO } from '../types';

interface Props {
  orderId: string;
}

export default function DeliveryProofDisplay({ orderId }: Props) {
  const [proof, setProof] = useState<DeliveryProofDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [imageOpen, setImageOpen] = useState(false);

  useEffect(() => {
    (async () => {
      try {
        const data = await orderService.getDeliveryProof(orderId);
        setProof(data);
      } catch {
        // No proof available — that's fine
      } finally {
        setLoading(false);
      }
    })();
  }, [orderId]);

  if (loading) return null;
  if (!proof) return null;

  const apiBase = import.meta.env.VITE_API_BASE_URL?.replace('/api', '') || '';
  const photoSrc = proof.photoUrl ? `${apiBase}${proof.photoUrl}` : null;

  return (
    <div className="bg-green-50 border border-green-200 rounded-lg p-4 mt-4">
      <h4 className="text-sm font-semibold text-green-800 mb-3 flex items-center gap-2">
        <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        Delivery Proof
      </h4>

      <div className="space-y-3">
        {/* Proof type badge */}
        <div className="flex items-center gap-2">
          <span className="text-xs font-medium bg-green-100 text-green-700 px-2 py-0.5 rounded">
            {proof.proofType}
          </span>
          {proof.otpVerified && (
            <span className="text-xs font-medium bg-blue-100 text-blue-700 px-2 py-0.5 rounded">
              OTP Verified
            </span>
          )}
        </div>

        {/* Photo */}
        {photoSrc && (
          <div>
            <img
              src={photoSrc}
              alt="Delivery proof photo"
              className="w-full max-w-xs h-32 object-cover rounded-lg cursor-pointer border hover:opacity-90 transition"
              onClick={() => setImageOpen(true)}
            />
          </div>
        )}

        {/* Notes */}
        {proof.notes && (
          <p className="text-sm text-gray-700">
            <span className="font-medium">Notes:</span> {proof.notes}
          </p>
        )}

        {/* GPS location */}
        {proof.lat && proof.lng && (
          <p className="text-xs text-gray-500">
            Delivered at: {Number(proof.lat).toFixed(6)}, {Number(proof.lng).toFixed(6)}
          </p>
        )}

        {/* Timestamp */}
        <p className="text-xs text-gray-400">
          Submitted: {new Date(proof.submittedAt).toLocaleString()}
        </p>
      </div>

      {/* Full-screen image overlay */}
      {imageOpen && photoSrc && (
        <div
          className="fixed inset-0 bg-black bg-opacity-80 flex items-center justify-center z-50 p-4"
          onClick={() => setImageOpen(false)}
        >
          <img
            src={photoSrc}
            alt="Delivery proof (full size)"
            className="max-w-full max-h-full object-contain rounded-lg"
          />
        </div>
      )}
    </div>
  );
}
