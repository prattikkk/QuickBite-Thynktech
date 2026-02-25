/**
 * Chat types — Phase 4.4
 */

export interface ChatRoomDTO {
  id: string;
  orderId: string;
  otherUserId: string;
  otherUserName: string;
  roomType: 'CUSTOMER_DRIVER' | 'CUSTOMER_VENDOR';
  closed: boolean;
  lastMessage?: string;
  lastMessageAt?: string;
  unreadCount: number;
  createdAt: string;
}

export interface ChatMessageDTO {
  id: string;
  roomId: string;
  senderId: string;
  senderName: string;
  content: string;
  read: boolean;
  createdAt: string;
}

export interface ChatRoomCreateRequest {
  orderId: string;
  otherUserId: string;
  roomType: 'CUSTOMER_DRIVER' | 'CUSTOMER_VENDOR';
}

/**
 * Analytics types — Phase 4.2 / 4.3
 */

export interface VendorAnalytics {
  totalOrders: number;
  totalRevenueCents: number;
  averagePrepTime: number;
  cancellationRate: number;
  topItems: { name: string; count: number; revenue: number }[];
  ordersByDay: { date: string; count: number; revenue: number }[];
}

export interface PlatformKpis {
  totalOrders: number;
  totalRevenueCents: number;
  totalCustomers: number;
  totalVendors: number;
  totalDrivers: number;
  averageDeliveryTime: number;
  repeatOrderRate: number;
}

/**
 * Driver review types — Phase 4.7
 */

export interface DriverReviewDTO {
  id: string;
  orderId: string;
  orderNumber: string;
  customerId: string;
  customerName: string;
  driverId: string;
  driverName: string;
  rating: number;
  comment?: string;
  disputed: boolean;
  disputeReason?: string;
  hidden: boolean;
  createdAt: string;
}

/**
 * Modifiers — Phase 4.12
 */

export interface ModifierGroupDTO {
  id: string;
  menuItemId: string;
  name: string;
  required: boolean;
  minSelections: number;
  maxSelections: number;
  sortOrder: number;
  modifiers: ModifierDTO[];
}

export interface ModifierDTO {
  id: string;
  groupId: string;
  name: string;
  priceCents: number;
  available: boolean;
  sortOrder: number;
}

export interface SelectedModifier {
  modifierId: string;
  name: string;
  priceCents: number;
}

/**
 * Inventory — Phase 4.6
 */

export interface InventoryItem {
  itemId: string;
  name: string;
  stockCount: number;
  lowStockThreshold: number;
  autoDisableOnZero: boolean;
  available: boolean;
  lowStock: boolean;
}
