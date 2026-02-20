/**
 * Payment service — aligned with backend PaymentController
 */

import api from './api';
import {
  PaymentIntentRequest,
  PaymentIntentResponse,
  PaymentResponse,
} from '../types';

export const paymentService = {
  /**
   * Create payment intent
   * POST /payments/intent
   */
  createPaymentIntent: async (
    paymentData: PaymentIntentRequest
  ): Promise<PaymentIntentResponse> => {
    const response = await api.post<any, PaymentIntentResponse>(
      '/payments/intent',
      paymentData
    );
    return response;
  },

  /**
   * Get payment details by payment ID
   * GET /payments/:paymentId
   */
  getPayment: async (paymentId: string): Promise<PaymentResponse> => {
    const response = await api.get<any, PaymentResponse>(`/payments/${paymentId}`);
    return response;
  },
};

export default paymentService;
