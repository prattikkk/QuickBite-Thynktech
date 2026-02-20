# QuickBite Frontend

Modern, responsive React frontend for the QuickBite food delivery platform.

## 🚀 Tech Stack

- **React 18** - UI library
- **TypeScript** - Type safety and better DX
- **Vite** - Fast build tool and dev server
- **Tailwind CSS** - Utility-first CSS framework
- **React Router v6** - Client-side routing
- **Zustand** - Lightweight state management
- **Axios** - HTTP client with interceptors
- **STOMP + SockJS** - WebSocket for real-time updates
- **Cypress** - E2E testing framework

## 📋 Prerequisites

- **Node.js** 18.x or higher
- **npm** 9.x or higher
- **Backend API** running on `http://localhost:8080`

## 🛠️ Installation

### 1. Clone and Navigate

```bash
cd frontend
```

### 2. Install Dependencies

```bash
npm install
```

### 3. Environment Configuration

Create a `.env` file in the `frontend` directory:

```bash
cp .env.example .env
```

Update the `.env` file with your configuration:

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

## 🏃 Running the Application

### Development Mode

Start the Vite dev server with hot module replacement:

```bash
npm run dev
```

The app will be available at `http://localhost:5173`

### Production Build

Build the optimized production bundle:

```bash
npm run build
```

### Preview Production Build

Preview the production build locally:

```bash
npm run preview
```

## 🧪 Testing

### Run E2E Tests

```bash
npm run test:e2e
```

### Open Cypress Test Runner

```bash
npm run cypress:open
```

## 📁 Project Structure

```
frontend/
├── src/
│   ├── components/        # Reusable UI components
│   │   ├── CartWidget.tsx
│   │   ├── Footer.tsx
│   │   ├── Header.tsx
│   │   ├── LoadingSpinner.tsx
│   │   ├── MenuItemCard.tsx
│   │   ├── ProtectedRoute.tsx
│   │   └── Toast.tsx
│   ├── hooks/            # Custom React hooks
│   │   ├── useAuth.ts
│   │   └── useOrderUpdates.ts
│   ├── pages/            # Page components
│   │   ├── Cart.tsx
│   │   ├── Checkout.tsx
│   │   ├── DriverDashboard.tsx
│   │   ├── Login.tsx
│   │   ├── OrderTrack.tsx
│   │   ├── Register.tsx
│   │   ├── VendorDashboard.tsx
│   │   ├── VendorDetail.tsx
│   │   └── VendorList.tsx
│   ├── services/         # API service layer
│   │   ├── address.service.ts
│   │   ├── api.ts
│   │   ├── auth.service.ts
│   │   ├── driver.service.ts
│   │   ├── order.service.ts
│   │   ├── payment.service.ts
│   │   └── vendor.service.ts
│   ├── store/            # Zustand stores
│   │   ├── authStore.ts
│   │   ├── cartStore.ts
│   │   └── toastStore.ts
│   ├── types/            # TypeScript type definitions
│   │   ├── auth.types.ts
│   │   ├── common.types.ts
│   │   ├── order.types.ts
│   │   ├── payment.types.ts
│   │   └── vendor.types.ts
│   ├── utils/            # Utility functions
│   │   ├── dateHelpers.ts
│   │   ├── formatCurrency.ts
│   │   └── validation.ts
│   ├── App.tsx           # Main app component with routing
│   ├── main.tsx          # Application entry point
│   └── index.css         # Global styles and Tailwind directives
├── cypress/              # E2E test files
├── public/               # Static assets
├── .env.example          # Environment variables template
├── index.html            # HTML template
├── package.json          # Dependencies and scripts
├── tailwind.config.js    # Tailwind CSS configuration
├── tsconfig.json         # TypeScript configuration
└── vite.config.ts        # Vite configuration
```

## 🔑 Key Features

### Authentication
- Login/Register with role selection (Customer, Vendor, Driver)
- JWT-based authentication with refresh tokens
- Protected routes based on user roles
- Persistent login state with localStorage

### Customer Features
- Browse restaurants with search and filtering
- View restaurant menus with item details
- Add items to cart with quantity management
- Checkout with address and payment selection
- Track orders in real-time with status updates
- View order history

### Vendor Features
- Dashboard to view incoming orders
- Accept/Reject orders
- Mark orders as ready for pickup
- Real-time order notifications

### Driver Features
- View assigned orders
- Mark orders as picked up
- Mark orders as delivered
- Navigate to delivery addresses

### Real-time Updates
- WebSocket integration using STOMP
- Live order status updates
- Toast notifications for important events
- Fallback to polling if WebSocket unavailable

## 🎨 UI/UX Features

- **Responsive Design** - Mobile-first approach with Tailwind CSS
- **Loading States** - Smooth loading spinners for better UX
- **Error Handling** - Toast notifications for user feedback
- **Form Validation** - Client-side validation with helpful error messages
- **Accessible** - Semantic HTML and ARIA labels
- **Dark Mode Ready** - Utility classes for future dark mode support

## 🔐 Security

- JWT tokens stored in localStorage (consider httpOnly cookies for production)
- Authorization header automatically added to API requests
- Input validation on forms
- Protected routes prevent unauthorized access
- CORS handled by backend proxy in development

## 🌐 API Integration

The frontend communicates with the backend API running on port 8080. All API calls go through the Axios instance configured in `src/services/api.ts` with:

- Automatic bearer token injection
- Error response handling
- Base URL configuration from environment variables
- Request/response interceptors

### API Endpoints Used

- **Auth**: `/auth/register`, `/auth/login`, `/auth/refresh`
- **Vendors**: `/vendors`, `/vendors/{id}`, `/vendors/{id}/menu`
- **Orders**: `/orders`, `/orders/{id}`, `/orders/{id}/status`
- **Addresses**: `/addresses`, `/addresses/{id}`
- **Payments**: `/payment/initiate`, `/payment/verify`
- **Driver**: `/driver/orders`, `/driver/orders/{id}/pickup`, `/driver/orders/{id}/deliver`

## 📝 Configuration Files

### `vite.config.ts`

- Path aliases (`@` for `src/`)
- Proxy configuration for backend API
- Environment variable prefix: `VITE_`

### `tailwind.config.js`

- Custom color palette (primary red theme)
- Font family configuration
- Custom utility classes

### `tsconfig.json`

- Strict mode enabled
- Path mapping for clean imports
- ES2020 target with modern features

## 🧩 State Management

### Auth Store (`authStore.ts`)
- User authentication state
- Login/logout actions
- Token management
- localStorage persistence

### Cart Store (`cartStore.ts`)
- Shopping cart items
- Add/remove/update quantities
- Vendor validation (single vendor per order)
- localStorage sync

### Toast Store (`toastStore.ts`)
- Notification system
- Success/error/warning/info toasts
- Auto-dismiss after 5 seconds

## 🎯 Development Guidelines

### Adding a New Page

1. Create component in `src/pages/`
2. Define route in `App.tsx`
3. Add navigation link in `Header.tsx` if needed
4. Use `ProtectedRoute` wrapper for authenticated pages

### Adding a New API Service

1. Create service file in `src/services/`
2. Define TypeScript types in `src/types/`
3. Import and use in components
4. Handle errors with try-catch and toast notifications

### Styling Guidelines

- Use Tailwind utility classes
- Follow mobile-first responsive design
- Use `primary-*` colors for brand consistency
- Keep component styles co-located

## 🐛 Troubleshooting

### Port 5173 Already in Use

```bash
# Kill the process or use a different port
npm run dev -- --port 3000
```

### API Connection Issues

- Verify backend is running on port 8080
- Check `.env` file has correct `VITE_API_BASE_URL`
- Check browser console for CORS errors

### Build Errors

```bash
# Clear node_modules and reinstall
rm -rf node_modules package-lock.json
npm install
```

### Type Errors

```bash
# Regenerate TypeScript declarations
npm run build
```

## 📦 Production Deployment

### Build Optimization

The production build is optimized with:
- Code splitting for faster initial load
- Tree shaking to remove unused code
- Minification and compression
- Asset optimization (images, fonts)

### Deployment Checklist

- [ ] Update `VITE_API_BASE_URL` to production API
- [ ] Run `npm run build`
- [ ] Test production build with `npm run preview`
- [ ] Deploy `dist/` folder to hosting service
- [ ] Configure nginx/apache for SPA routing
- [ ] Set up SSL certificate
- [ ] Enable gzip compression on server

### Docker Deployment

```bash
# Build Docker image
docker build -t quickbite-frontend .

# Run container
docker run -p 80:80 quickbite-frontend
```

## 🤝 Contributing

1. Create a feature branch
2. Make your changes
3. Run tests and linting
4. Submit a pull request

## 📄 License

See LICENSE file in the root directory.

## 📞 Support

For issues or questions, contact the development team or create an issue in the repository.

---

**Built with ❤️ by the QuickBite Team**

