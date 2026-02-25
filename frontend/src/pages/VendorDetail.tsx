/**
 * VendorDetail page - shows vendor info and menu
 */

import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { vendorService } from '../services';
import { useCartStore, useToastStore } from '../store';
import { VendorDTO, MenuItemDTO } from '../types';
import { LoadingSpinner, MenuItemCard, FavoriteButton, VendorReviews } from '../components';
import ModifierSelector from '../components/ModifierSelector';
import type { SelectedModifier } from '../types/phase4.types';
import { modifierService } from '../services/modifier.service';

export default function VendorDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [vendor, setVendor] = useState<VendorDTO | null>(null);
  const [menuItems, setMenuItems] = useState<MenuItemDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const { addItem } = useCartStore();
  const { success } = useToastStore();

  // Modifier modal state
  const [modifierItem, setModifierItem] = useState<MenuItemDTO | null>(null);

  useEffect(() => {
    loadVendorAndMenu();
  }, [id]);

  const loadVendorAndMenu = async () => {
    if (!id) return;

    try {
      setLoading(true);
      const [vendorData, menuData] = await Promise.all([
        vendorService.getVendorById(id),
        vendorService.getVendorMenu(id, { page: 0, size: 100 }),
      ]);

      setVendor(vendorData);
      // getVendorMenu returns array directly
      setMenuItems(Array.isArray(menuData) ? menuData : []);
    } catch (err: any) {
      setError(err.message || 'Failed to load vendor details');
    } finally {
      setLoading(false);
    }
  };

  const handleAddToCart = async (menuItem: MenuItemDTO) => {
    if (!vendor) return;

    // Check for modifiers — if item has modifier groups, show modal
    try {
      const groups = await modifierService.getModifiers(menuItem.id);
      if (groups.length > 0) {
        setModifierItem(menuItem);
        return; // show modifier modal instead of adding directly
      }
    } catch {
      // No modifiers or API error — add directly
    }

    addItem({
      menuItemId: menuItem.id,
      menuItemName: menuItem.name,
      vendorId: vendor.id,
      vendorName: vendor.name,
      priceCents: menuItem.priceCents,
      imageUrl: menuItem.imageUrl,
    });

    success(`${menuItem.name} added to cart!`);
  };

  const handleModifiersConfirmed = (_modifiers: SelectedModifier[], totalExtraCents: number) => {
    if (!vendor || !modifierItem) return;
    addItem({
      menuItemId: modifierItem.id,
      menuItemName: modifierItem.name,
      vendorId: vendor.id,
      vendorName: vendor.name,
      priceCents: modifierItem.priceCents + totalExtraCents,
      imageUrl: modifierItem.imageUrl,
    });
    success(`${modifierItem.name} added to cart with customizations!`);
    setModifierItem(null);
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  if (error || !vendor) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <h2 className="text-2xl font-bold text-gray-900 mb-2">Error</h2>
          <p className="text-gray-600 mb-4">{error || 'Vendor not found'}</p>
          <button
            onClick={() => navigate('/vendors')}
            className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700"
          >
            Back to Vendors
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Vendor Header */}
      <div className="bg-white shadow-md mb-6">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <button
            onClick={() => navigate('/vendors')}
            className="text-primary-600 hover:text-primary-700 font-medium mb-4 inline-flex items-center"
          >
            <svg className="w-5 h-5 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
            Back to vendors
          </button>

          <div className="flex flex-col md:flex-row gap-6">
            <div className="flex-1">
              <div className="flex items-start justify-between">
                <h1 className="text-3xl font-bold text-gray-900 mb-2">{vendor.name}</h1>
                <FavoriteButton vendorId={vendor.id} className="ml-4" />
              </div>
              <p className="text-gray-600 mb-4">{vendor.description}</p>

              <div className="flex flex-wrap gap-4 text-sm text-gray-600">
                <div className="flex items-center">
                  <svg className="w-5 h-5 mr-1 text-yellow-500" fill="currentColor" viewBox="0 0 20 20">
                    <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                  </svg>
                  {vendor.rating ? vendor.rating.toFixed(1) : 'N/A'}
                </div>

                <div className="flex items-center">
                  <svg className="w-5 h-5 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                  {vendor.address}
                </div>

                <div className="flex items-center">
                  <svg className="w-5 h-5 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                  </svg>
                  {vendor.menuItemCount} menu items
                </div>
              </div>

              {!vendor.active && (
                <div className="mt-4 bg-yellow-50 border border-yellow-200 rounded-lg p-3">
                  <p className="text-yellow-800 text-sm font-medium">Currently Closed</p>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Menu Items */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <h2 className="text-2xl font-bold text-gray-900 mb-6">Menu</h2>

        {menuItems.length === 0 ? (
          <div className="text-center py-12">
            <p className="text-gray-600">No menu items available</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {menuItems.map((item) => (
              <MenuItemCard
                key={item.id}
                menuItem={item}
                onAddToCart={handleAddToCart}
                disabled={!vendor.active}
              />
            ))}
          </div>
        )}

        {/* Reviews Section */}
        {id && (
          <div className="mt-10">
            <VendorReviews vendorId={id} />
          </div>
        )}
      </div>

      {/* Modifier Selection Modal */}
      {modifierItem && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg shadow-xl max-w-md w-full max-h-[80vh] overflow-y-auto p-6">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-lg font-bold text-gray-900">Customize: {modifierItem.name}</h3>
              <button
                onClick={() => setModifierItem(null)}
                className="text-gray-400 hover:text-gray-600"
              >
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            <ModifierSelector
              menuItemId={modifierItem.id}
              onSelectModifiers={handleModifiersConfirmed}
            />

            <div className="mt-6">
              <button
                onClick={() => setModifierItem(null)}
                className="w-full py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 font-medium"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
