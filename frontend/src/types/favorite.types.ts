/**
 * Favorite types
 */

export interface FavoriteDTO {
  id: string;
  vendorId: string;
  vendorName: string;
  vendorDescription: string;
  vendorAddress: string;
  rating: number | null;
  vendorActive: boolean;
  createdAt: string;
}
