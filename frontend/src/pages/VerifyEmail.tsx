import { useState, useEffect } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { authService } from '../services/auth.service';
import { LoadingSpinner } from '../components/LoadingSpinner';

function ResendForm() {
  const [email, setEmail] = useState('');
  const [sending, setSending] = useState(false);
  const [sent, setSent] = useState(false);

  const handleResend = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim()) return;
    setSending(true);
    try {
      await authService.resendVerification(email.trim());
      setSent(true);
    } catch {
      // silent — server doesn't reveal if email exists
      setSent(true);
    } finally {
      setSending(false);
    }
  };

  if (sent) {
    return (
      <p className="text-sm text-green-600 mt-3">
        If that email is registered, a new verification link has been sent.
      </p>
    );
  }

  return (
    <form onSubmit={handleResend} className="mt-4 space-y-2">
      <p className="text-sm text-gray-500">Need a new verification link?</p>
      <input
        type="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        placeholder="Enter your email"
        required
        className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-primary-500 focus:border-primary-500"
      />
      <button
        type="submit"
        disabled={sending}
        className="w-full bg-primary-600 text-white py-2 px-4 rounded-md text-sm hover:bg-primary-700 disabled:opacity-50"
      >
        {sending ? 'Sending…' : 'Resend Verification Email'}
      </button>
    </form>
  );
}

export default function VerifyEmail() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') || '';

  const [status, setStatus] = useState<'loading' | 'success' | 'error' | 'no-token'>('loading');
  const [errorMsg, setErrorMsg] = useState('');

  useEffect(() => {
    if (!token) {
      setStatus('no-token');
      return;
    }

    const verify = async () => {
      try {
        await authService.verifyEmail(token);
        setStatus('success');
      } catch (err: any) {
        setStatus('error');
        setErrorMsg(err.message || 'Verification failed. Token may be expired.');
      }
    };

    verify();
  }, [token]);

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center px-4 py-12">
      <div className="max-w-md w-full">
        <div className="text-center mb-8">
          <h1 className="text-4xl font-bold text-primary-600 mb-2">QuickBite</h1>
        </div>

        <div className="bg-white py-8 px-6 shadow rounded-lg text-center">
          {status === 'loading' && (
            <div>
              <LoadingSpinner size="lg" />
              <p className="mt-4 text-gray-600">Verifying your email...</p>
            </div>
          )}

          {status === 'success' && (
            <div>
              <svg className="mx-auto h-12 w-12 text-green-500 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
              </svg>
              <h2 className="text-xl font-semibold text-gray-900 mb-2">Email Verified!</h2>
              <p className="text-gray-600 mb-4">
                Your email address has been verified. You can now use all features.
              </p>
              <Link to="/login" className="text-primary-600 hover:text-primary-700 font-medium">
                Go to login →
              </Link>
            </div>
          )}

          {status === 'error' && (
            <div>
              <svg className="mx-auto h-12 w-12 text-red-500 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
              <h2 className="text-xl font-semibold text-gray-900 mb-2">Verification Failed</h2>
              <p className="text-gray-600 mb-4">{errorMsg}</p>
              <ResendForm />
              <Link to="/login" className="text-primary-600 hover:text-primary-700 font-medium mt-3 inline-block">
                ← Back to login
              </Link>
            </div>
          )}

          {status === 'no-token' && (
            <div>
              <svg className="mx-auto h-12 w-12 text-yellow-500 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
              <h2 className="text-xl font-semibold text-gray-900 mb-2">No Token Provided</h2>
              <p className="text-gray-600 mb-4">
                Please use the verification link from your email.
              </p>
              <ResendForm />
              <Link to="/login" className="text-primary-600 hover:text-primary-700 font-medium mt-3 inline-block">
                ← Back to login
              </Link>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
