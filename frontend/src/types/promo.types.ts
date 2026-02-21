/**
 * Promo code types
 */

export type DiscountType = 'PERCENT' | 'FIXED';

export interface PromoCodeDTO {
  id: string;
  code: string;
  description: string;
  discountType: DiscountType;
  discountValue: number;
  minOrderCents: number;
  maxDiscountCents?: number;
  maxUses?: number;
  currentUses: number;
  validFrom: string;
  validUntil?: string;
  active: boolean;
  createdAt: string;
}

export interface PromoValidateResponse {
  valid: boolean;
  code: string;
  description?: string;
  discountType?: string;
  discountCents?: number;
  message: string;
}
