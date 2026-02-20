/**
 * Format currency from cents to rupees
 */
export const formatCurrency = (cents: number): string => {
  const rupees = cents / 100;
  return `₹${rupees.toFixed(2)}`;
};

/**
 * Format currency without decimals for whole numbers
 */
export const formatCurrencyCompact = (cents: number): string => {
  const rupees = cents / 100;
  if (rupees % 1 === 0) {
    return `₹${Math.floor(rupees)}`;
  }
  return `₹${rupees.toFixed(2)}`;
};

/**
 * Parse currency string to cents
 */
export const parseCurrencyToCents = (value: string): number => {
  const cleaned = value.replace(/[₹,\s]/g, '');
  const rupees = parseFloat(cleaned);
  return Math.round(rupees * 100);
};
