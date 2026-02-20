/**
 * Validation utilities
 */

export const validateEmail = (email: string): boolean => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
};

export const validatePhone = (phone: string): boolean => {
  // Indian phone number validation (10 digits)
  const phoneRegex = /^[6-9]\d{9}$/;
  return phoneRegex.test(phone.replace(/[\s-]/g, ''));
};

export const validatePassword = (password: string): { valid: boolean; message?: string } => {
  if (password.length < 8) {
    return { valid: false, message: 'Password must be at least 8 characters' };
  }
  if (!/[A-Z]/.test(password)) {
    return { valid: false, message: 'Password must contain at least one uppercase letter' };
  }
  if (!/[a-z]/.test(password)) {
    return { valid: false, message: 'Password must contain at least one lowercase letter' };
  }
  if (!/[0-9]/.test(password)) {
    return { valid: false, message: 'Password must contain at least one number' };
  }
  if (!/[@$!%*?&]/.test(password)) {
    return { valid: false, message: 'Password must contain at least one special character (@$!%*?&)' };
  }
  return { valid: true };
};

export const validateRequired = (value: string): boolean => {
  return value.trim().length > 0;
};
