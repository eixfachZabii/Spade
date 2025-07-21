// client/src/App.js
import { useState, useEffect } from "react";
import { Routes, Navigate, Route, useLocation } from "react-router-dom";
import { ThemeProvider } from "@mui/material/styles";
import CssBaseline from "@mui/material/CssBaseline";
import Sidenav from "./examples/Sidenav";
import Configurator from "./examples/Configurator";
import theme from "./assets/theme";
import { CacheProvider } from "@emotion/react";
import routes from "./routes";
import { useVisionUIController, setMiniSidenav, setOpenConfigurator } from "./context";

// Import SpotifyProvider and the mini player
import { SpotifyProvider } from "./context/SpotifyContext";
import SpotifyMiniPlayer from "./layouts/spotify/SpotifyMiniPlayer";

// Import AuthProvider and ProtectedRoute for authentication
import { AuthProvider, useAuth } from "./context/AuthContext";
import ProtectedRoute from "./components/ProtectedRoute";

// App content component that has access to auth context
const AppContent = () => {
  const [controller, dispatch] = useVisionUIController();
  const { miniSidenav, direction, layout, openConfigurator, sidenavColor } = controller;
  const [onMouseEnter, setOnMouseEnter] = useState(false);
  const [rtlCache] = useState(null);
  const { pathname } = useLocation();
  const { isAuthenticated, loading } = useAuth();

  // Open sidenav when mouse enter on mini sidenav
  const handleOnMouseEnter = () => {
    if (miniSidenav && !onMouseEnter) {
      setMiniSidenav(dispatch, false);
      setOnMouseEnter(true);
    }
  };

  // Close sidenav when mouse leave mini sidenav
  const handleOnMouseLeave = () => {
    if (onMouseEnter) {
      setMiniSidenav(dispatch, true);
      setOnMouseEnter(false);
    }
  };

  // Change the openConfigurator state
  const handleConfiguratorOpen = () => setOpenConfigurator(dispatch, !openConfigurator);

  // Setting the dir attribute for the body element
  useEffect(() => {
    document.body.setAttribute("dir", direction);
  }, [direction]);

  // Setting page scroll to 0 when changing the route
  useEffect(() => {
    document.documentElement.scrollTop = 0;
    document.scrollingElement.scrollTop = 0;
  }, [pathname]);

  // Check if current path is authentication page
  const isAuthPage = pathname.startsWith('/authentication/');

  // Generate routes with protection
  const getRoutes = (allRoutes) =>
      allRoutes.map((route) => {
        if (route.collapse) {
          return getRoutes(route.collapse);
        }

        if (route.route) {
          // Determine if route needs protection
          const needsAuth = route.requiresAuth !== false; // Default to true unless explicitly false

          if (needsAuth) {
            // Wrap protected routes with ProtectedRoute
            return (
                <Route
                    exact
                    path={route.route}
                    element={
                      <ProtectedRoute>
                        <route.component />
                      </ProtectedRoute>
                    }
                    key={route.key}
                />
            );
          } else {
            // Public routes (sign-in, sign-up)
            return (
                <Route
                    exact
                    path={route.route}
                    element={<route.component />}
                    key={route.key}
                />
            );
          }
        }

        return null;
      });

  return (
      <SpotifyProvider>
        {direction === "rtl" ? (
            <CacheProvider value={rtlCache}>
              <ThemeProvider theme={theme}>
                <CssBaseline />
                {layout === "dashboard" && !isAuthPage && isAuthenticated && (
                    <>
                      <Sidenav
                          color={sidenavColor}
                          brand=""
                          brandName="SPADE"
                          routes={routes}
                          onMouseEnter={handleOnMouseEnter}
                          onMouseLeave={handleOnMouseLeave}
                      />
                      <Configurator />
                    </>
                )}
                {layout === "vr" && <Configurator />}
                <Routes>
                  {getRoutes(routes)}
                  {/* Default route - redirect to dashboard if authenticated, otherwise to sign-in */}
                  <Route
                      path="/"
                      element={
                        <Navigate
                            to={isAuthenticated ? "/dashboard" : "/authentication/sign-in"}
                            replace
                        />
                      }
                  />
                  {/* Catch all route - redirect to dashboard if authenticated, otherwise to sign-in */}
                  <Route
                      path="*"
                      element={
                        <Navigate
                            to={isAuthenticated ? "/dashboard" : "/authentication/sign-in"}
                            replace
                        />
                      }
                  />
                </Routes>
                {/* Show Spotify mini player only on authenticated pages */}
                {!isAuthPage && isAuthenticated && <SpotifyMiniPlayer />}
              </ThemeProvider>
            </CacheProvider>
        ) : (
            <ThemeProvider theme={theme}>
              <CssBaseline />
              {layout === "dashboard" && !isAuthPage && isAuthenticated && (
                  <>
                    <Sidenav
                        color={sidenavColor}
                        brand=""
                        brandName="SPADE"
                        routes={routes}
                        onMouseEnter={handleOnMouseEnter}
                        onMouseLeave={handleOnMouseLeave}
                    />
                    <Configurator />
                  </>
              )}
              {layout === "vr" && <Configurator />}
              <Routes>
                {getRoutes(routes)}
                {/* Default route - redirect to dashboard if authenticated, otherwise to sign-in */}
                <Route
                    path="/"
                    element={
                      <Navigate
                          to={isAuthenticated ? "/dashboard" : "/authentication/sign-in"}
                          replace
                      />
                    }
                />
                {/* Catch all route - redirect to dashboard if authenticated, otherwise to sign-in */}
                <Route
                    path="*"
                    element={
                      <Navigate
                          to={isAuthenticated ? "/dashboard" : "/authentication/sign-in"}
                          replace
                      />
                    }
                />
              </Routes>
              {/* Show Spotify mini player only on authenticated pages */}
              {!isAuthPage && isAuthenticated && <SpotifyMiniPlayer />}
            </ThemeProvider>
        )}
      </SpotifyProvider>
  );
};

// Main App component
export default function App() {
  return (
      <AuthProvider>
        <AppContent />
      </AuthProvider>
  );
}