// client/src/components/ProtectedRoute.js
import { useAuth } from '../context/AuthContext';
import { Navigate, useLocation } from 'react-router-dom';
import VuiBox from './VuiBox';
import VuiTypography from './VuiTypography';
import { CircularProgress } from '@mui/material';

/**
 * A component wrapper that protects routes requiring authentication
 * Redirects unauthenticated users to login page
 */
const ProtectedRoute = ({ children }) => {
  const { user, loading, isAuthenticated } = useAuth();
  const location = useLocation();

  // Show loading spinner while checking authentication
  if (loading) {
    return (
        <VuiBox
            display="flex"
            alignItems="center"
            justifyContent="center"
            height="100vh"
            flexDirection="column"
        >
          <CircularProgress color="info" size={40} />
          <VuiTypography color="text" variant="button" fontWeight="regular" sx={{ mt: 2 }}>
            Checking authentication...
          </VuiTypography>
        </VuiBox>
    );
  }

  // If not authenticated, redirect to login with return path
  if (!isAuthenticated || !user) {
    return (
        <Navigate
            to="/authentication/sign-in"
            state={{ from: location.pathname }}
            replace
        />
    );
  }

  // If authenticated, render the protected component
  return children;
};

export default ProtectedRoute;