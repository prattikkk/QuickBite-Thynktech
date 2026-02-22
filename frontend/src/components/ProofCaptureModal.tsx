/**
 * ProofCaptureModal — Phase 3 proof-of-delivery capture.
 * Shows camera/file upload for photo proof, with optional notes and GPS.
 * Also supports OTP verification flow when OTP feature flag is enabled.
 */

import { useState, useRef } from 'react';
import { driverService } from '../services/driver.service';
import type { DeliveryProofDTO } from '../types';

interface Props {
  orderId: string;
  onProofSubmitted: (proof: DeliveryProofDTO) => void;
  onCancel: () => void;
  onDeliverWithoutProof: () => void;
}

type ProofMode = 'photo' | 'otp';

export default function ProofCaptureModal({
  orderId,
  onProofSubmitted,
  onCancel,
  onDeliverWithoutProof,
}: Props) {
  const [mode, setMode] = useState<ProofMode>('photo');
  const [photo, setPhoto] = useState<File | null>(null);
  const [photoPreview, setPhotoPreview] = useState<string | null>(null);
  const [notes, setNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [proofRequired, setProofRequired] = useState<boolean | null>(null);
  const [checkingRequired, setCheckingRequired] = useState(false);

  // OTP state
  const [otpCode, setOtpCode] = useState('');
  const [otpGenerated, setOtpGenerated] = useState(false);
  const [otpGenerating, setOtpGenerating] = useState(false);

  const fileInputRef = useRef<HTMLInputElement>(null);

  // Check if proof is required on mount
  useState(() => {
    (async () => {
      try {
        setCheckingRequired(true);
        const required = await driverService.isProofRequired(orderId);
        setProofRequired(required);
      } catch {
        setProofRequired(false);
      } finally {
        setCheckingRequired(false);
      }
    })();
  });

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Validate file type
    const allowed = ['image/jpeg', 'image/png', 'image/webp', 'image/heic'];
    if (!allowed.includes(file.type)) {
      setError('Please select a JPEG, PNG, or WebP image');
      return;
    }

    setPhoto(file);
    setError('');

    // Create preview
    const reader = new FileReader();
    reader.onload = () => setPhotoPreview(reader.result as string);
    reader.readAsDataURL(file);
  };

  const handlePhotoSubmit = async () => {
    if (!photo) {
      setError('Please capture or select a photo');
      return;
    }

    try {
      setSubmitting(true);
      setError('');

      // Get current GPS position
      let lat: number | undefined;
      let lng: number | undefined;
      try {
        const pos = await new Promise<GeolocationPosition>((resolve, reject) =>
          navigator.geolocation.getCurrentPosition(resolve, reject, { timeout: 5000 })
        );
        lat = pos.coords.latitude;
        lng = pos.coords.longitude;
      } catch {
        // GPS not available — proceed without
      }

      const proof = await driverService.submitPhotoProof(orderId, photo, notes || undefined, lat, lng);
      onProofSubmitted(proof);
    } catch (err: any) {
      setError(err?.message || 'Failed to submit proof');
    } finally {
      setSubmitting(false);
    }
  };

  const handleGenerateOtp = async () => {
    try {
      setOtpGenerating(true);
      setError('');
      await driverService.generateOtp(orderId);
      setOtpGenerated(true);
    } catch (err: any) {
      setError(err?.message || 'Failed to generate OTP');
    } finally {
      setOtpGenerating(false);
    }
  };

  const handleVerifyOtp = async () => {
    if (otpCode.length !== 6) {
      setError('Please enter the 6-digit OTP');
      return;
    }

    try {
      setSubmitting(true);
      setError('');
      const proof = await driverService.verifyOtp(orderId, otpCode);
      onProofSubmitted(proof);
    } catch (err: any) {
      setError(err?.message || 'Invalid OTP code');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl shadow-2xl max-w-lg w-full max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="px-6 py-4 border-b flex items-center justify-between">
          <h3 className="text-lg font-semibold text-gray-900">Delivery Proof</h3>
          <button
            onClick={onCancel}
            className="text-gray-400 hover:text-gray-600 text-xl"
          >
            &times;
          </button>
        </div>

        {/* Loading state */}
        {checkingRequired && (
          <div className="p-6 text-center text-gray-500">Checking proof requirements...</div>
        )}

        {!checkingRequired && (
          <div className="p-6 space-y-4">
            {/* Mode toggle */}
            <div className="flex gap-2 border-b pb-3">
              <button
                onClick={() => setMode('photo')}
                className={`px-4 py-2 rounded-lg text-sm font-medium transition ${
                  mode === 'photo'
                    ? 'bg-primary-600 text-white'
                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                }`}
              >
                Photo Proof
              </button>
              <button
                onClick={() => setMode('otp')}
                className={`px-4 py-2 rounded-lg text-sm font-medium transition ${
                  mode === 'otp'
                    ? 'bg-primary-600 text-white'
                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                }`}
              >
                OTP Verification
              </button>
            </div>

            {/* Error display */}
            {error && (
              <div className="p-3 bg-red-50 border border-red-200 text-red-700 rounded-lg text-sm">
                {error}
              </div>
            )}

            {/* Photo mode */}
            {mode === 'photo' && (
              <div className="space-y-4">
                {/* Photo preview */}
                {photoPreview ? (
                  <div className="relative">
                    <img
                      src={photoPreview}
                      alt="Delivery proof"
                      className="w-full h-48 object-cover rounded-lg"
                    />
                    <button
                      onClick={() => { setPhoto(null); setPhotoPreview(null); }}
                      className="absolute top-2 right-2 bg-red-500 text-white rounded-full w-8 h-8 flex items-center justify-center text-sm hover:bg-red-600"
                    >
                      &times;
                    </button>
                  </div>
                ) : (
                  <div
                    onClick={() => fileInputRef.current?.click()}
                    className="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center cursor-pointer hover:border-primary-400 hover:bg-primary-50 transition"
                  >
                    <svg
                      className="mx-auto h-12 w-12 text-gray-400"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={1.5}
                        d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z"
                      />
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={1.5}
                        d="M15 13a3 3 0 11-6 0 3 3 0 016 0z"
                      />
                    </svg>
                    <p className="mt-2 text-sm text-gray-600">
                      Tap to capture or select a photo
                    </p>
                    <p className="text-xs text-gray-400 mt-1">
                      JPEG, PNG, or WebP up to 5MB
                    </p>
                  </div>
                )}

                {/* Hidden file input — accepts camera on mobile */}
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/jpeg,image/png,image/webp,image/heic"
                  capture="environment"
                  className="hidden"
                  onChange={handleFileChange}
                />

                {/* Notes */}
                <textarea
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  placeholder="Delivery notes (optional — e.g. left at door)"
                  className="w-full border border-gray-300 rounded-lg px-4 py-2 text-sm focus:ring-2 focus:ring-primary-500 focus:border-transparent resize-none"
                  rows={2}
                />

                <button
                  onClick={handlePhotoSubmit}
                  disabled={!photo || submitting}
                  className="w-full py-3 bg-green-600 text-white rounded-lg font-medium hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed transition"
                >
                  {submitting ? 'Submitting...' : 'Submit Photo Proof'}
                </button>
              </div>
            )}

            {/* OTP mode */}
            {mode === 'otp' && (
              <div className="space-y-4">
                {!otpGenerated ? (
                  <div className="text-center space-y-4">
                    <p className="text-sm text-gray-600">
                      Generate a 6-digit OTP that the customer will receive. Ask the customer for the code to confirm delivery.
                    </p>
                    <button
                      onClick={handleGenerateOtp}
                      disabled={otpGenerating}
                      className="w-full py-3 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 disabled:opacity-50 transition"
                    >
                      {otpGenerating ? 'Generating...' : 'Generate OTP'}
                    </button>
                  </div>
                ) : (
                  <div className="space-y-4">
                    <p className="text-sm text-gray-600 text-center">
                      OTP sent to customer. Enter the code the customer provides:
                    </p>
                    <input
                      type="text"
                      inputMode="numeric"
                      pattern="[0-9]*"
                      maxLength={6}
                      value={otpCode}
                      onChange={(e) => setOtpCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                      placeholder="Enter 6-digit OTP"
                      className="w-full text-center text-2xl tracking-[0.5em] border border-gray-300 rounded-lg px-4 py-3 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    />
                    <button
                      onClick={handleVerifyOtp}
                      disabled={otpCode.length !== 6 || submitting}
                      className="w-full py-3 bg-green-600 text-white rounded-lg font-medium hover:bg-green-700 disabled:opacity-50 transition"
                    >
                      {submitting ? 'Verifying...' : 'Verify OTP & Confirm Delivery'}
                    </button>
                  </div>
                )}
              </div>
            )}

            {/* Skip if not required */}
            {proofRequired === false && (
              <button
                onClick={onDeliverWithoutProof}
                className="w-full py-2 text-gray-600 text-sm hover:text-gray-800 underline"
              >
                Skip — Mark delivered without proof
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
