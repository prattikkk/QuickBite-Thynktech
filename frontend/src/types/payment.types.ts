/**
 * Payment DTOs
 */

export interface PaymentIntentRequest {
  orderId: string;
  currency?: string;
  paymentMethod?: string;
  description?: string;
}

export interface PaymentIntentResponse {
  id: string;
  providerPaymentId: string;
  clientSecret: string;
  amountCents: number;
  currency: string;
  status: string;
}

export interface PaymentConfirmRequest {
  paymentIntentId: string;
  orderId: string;
}

export interface PaymentResponse {
  id: string;
  providerPaymentId: string;
  clientSecret: string;
  amountCents: number;
  currency: string;
  status: string;
}
