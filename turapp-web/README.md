# turapp-web

React web app for planning outdoor trips and viewing trip history on a map.

## Features

- Register and log in (JWT-based auth)
- Browse saved routes and trips with map view
- Interactive route planner — click waypoints, see estimated distance and time
- Pace estimation based on your own trip history
- National parks shown as green polygons on the map
- Nature reserves shown as orange polygons on the map
- Kartverket topographic tile layer

## Tech stack

- **React 19** with TypeScript
- **Vite** for bundling and dev server
- **React Router v7** for client-side routing
- **Leaflet** + **react-leaflet** for interactive maps
- **Axios** for API communication (with JWT interceptor)
- **Oxlint** for linting

## Getting started

### Prerequisites
- Node.js 18+
- The backend running on `http://localhost:8080`

### Install and run

```bash
npm install
npm run dev
```

### Build for production

```bash
npm run build
```

## Project structure

```
src/
├── api/          # Axios client, endpoint functions, TypeScript types
├── auth/         # Auth context and protected route wrapper
├── components/   # Shared UI components (NavBar, KartverketTileLayer, etc.)
├── pages/        # Page-level components (Trips, Routes, Planner, etc.)
└── utils/        # Geo calculations, pace estimation
```
