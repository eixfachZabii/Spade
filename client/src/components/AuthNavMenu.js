// client/src/components/AuthNavMenu.js

import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Menu,
  MenuItem,
  IconButton,
  ListItemIcon,
  ListItemText,
  Avatar,
  Tooltip,
  Divider
} from '@mui/material';
import {
  Person,
  Logout,
  AccountCircle,
  Settings,
  Dashboard,
  Analytics
} from '@mui/icons-material';
import { useAuth } from '../context/AuthContext';
import VuiBox from './VuiBox';
import VuiTypography from './VuiTypography';

function AuthNavMenu() {
  const [anchorEl, setAnchorEl] = useState(null);
  const open = Boolean(anchorEl);
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  const handleClick = (event) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleLogout = () => {
    logout();
    handleClose();
    navigate('/authentication/sign-in');
  };

  const handleLogin = () => {
    handleClose();
    navigate('/authentication/sign-in');
  };

  const handleSignUp = () => {
    handleClose();
    navigate('/authentication/sign-up');
  };

  const handleProfile = () => {
    handleClose();
    navigate('/profile');
  };

  const handleDashboard = () => {
    handleClose();
    navigate('/dashboard');
  };

  const handleAnalytics = () => {
    handleClose();
    navigate('/analytics');
  };

  // Get user's initials for avatar
  const getUserInitials = () => {
    if (!user || !user.username) return 'U';
    const names = user.username.split(' ');
    if (names.length === 1) return names[0].charAt(0).toUpperCase();
    return (names[0].charAt(0) + names[names.length - 1].charAt(0)).toUpperCase();
  };

  return (
    <VuiBox>
      <Tooltip title={isAuthenticated ? user?.username || 'Account' : 'Login'}>
        <IconButton
          onClick={handleClick}
          size="small"
          aria-controls={open ? 'account-menu' : undefined}
          aria-haspopup="true"
          aria-expanded={open ? 'true' : undefined}
          sx={{
            color: 'white',
            ml: 0.5,
            transition: 'all 0.2s ease-in-out',
            '&:hover': {
              transform: 'translateY(-2px)',
              backgroundColor: 'rgba(255, 255, 255, 0.1)'
            }
          }}
        >
          {isAuthenticated ? (
            <Avatar
              sx={{
                width: 36,
                height: 36,
                bgcolor: 'primary.main',
                fontSize: '1rem',
                fontWeight: 'bold',
                border: '2px solid rgba(255, 255, 255, 0.2)'
              }}
            >
              {getUserInitials()}
            </Avatar>
          ) : (
            <Person />
          )}
        </IconButton>
      </Tooltip>
      <Menu
        anchorEl={anchorEl}
        id="account-menu"
        open={open}
        onClose={handleClose}
        PaperProps={{
          sx: {
            bgcolor: 'rgba(22, 25, 34, 0.9)',
            backdropFilter: 'blur(10px)',
            color: 'white',
            overflow: 'visible',
            filter: 'drop-shadow(0px 2px 8px rgba(0,0,0,0.32))',
            mt: 1.5,
            border: '1px solid rgba(255, 255, 255, 0.1)',
            borderRadius: '12px',
            '& .MuiMenuItem-root': {
              padding: 1.5,
              my: 0.5,
              borderRadius: '8px',
              mx: 0.5,
              transition: 'all 0.2s ease',
              '&:hover': {
                bgcolor: 'rgba(255, 255, 255, 0.1)'
              }
            }
          },
        }}
        transformOrigin={{ horizontal: 'right', vertical: 'top' }}
        anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
      >
        {isAuthenticated ? (
          <>
            <VuiBox px={2} pt={1.5} pb={0.5}>
              <VuiTypography variant="button" color="white" fontWeight="bold">
                Hello, {user?.username || 'User'}
              </VuiTypography>
            </VuiBox>
            <Divider sx={{ my: 1, bgcolor: 'rgba(255, 255, 255, 0.1)' }} />
            <MenuItem onClick={handleDashboard}>
              <ListItemIcon>
                <Dashboard fontSize="small" sx={{ color: 'white' }} />
              </ListItemIcon>
              <ListItemText>
                <VuiTypography variant="button" color="white">
                  Dashboard
                </VuiTypography>
              </ListItemText>
            </MenuItem>
            <MenuItem onClick={handleProfile}>
              <ListItemIcon>
                <AccountCircle fontSize="small" sx={{ color: 'white' }} />
              </ListItemIcon>
              <ListItemText>
                <VuiTypography variant="button" color="white">
                  Profile
                </VuiTypography>
              </ListItemText>
            </MenuItem>
            <MenuItem onClick={handleAnalytics}>
              <ListItemIcon>
                <Analytics fontSize="small" sx={{ color: 'white' }} />
              </ListItemIcon>
              <ListItemText>
                <VuiTypography variant="button" color="white">
                  Analytics
                </VuiTypography>
              </ListItemText>
            </MenuItem>
            <MenuItem onClick={handleLogout}>
              <ListItemIcon>
                <Logout fontSize="small" sx={{ color: 'white' }} />
              </ListItemIcon>
              <ListItemText>
                <VuiTypography variant="button" color="white">
                  Logout
                </VuiTypography>
              </ListItemText>
            </MenuItem>
          </>
        ) : (
          <>
            <MenuItem onClick={handleLogin}>
              <ListItemIcon>
                <Person fontSize="small" sx={{ color: 'white' }} />
              </ListItemIcon>
              <ListItemText>
                <VuiTypography variant="button" color="white">
                  Login
                </VuiTypography>
              </ListItemText>
            </MenuItem>
            <MenuItem onClick={handleSignUp}>
              <ListItemIcon>
                <AccountCircle fontSize="small" sx={{ color: 'white' }} />
              </ListItemIcon>
              <ListItemText>
                <VuiTypography variant="button" color="white">
                  Sign Up
                </VuiTypography>
              </ListItemText>
            </MenuItem>
          </>
        )}
      </Menu>
    </VuiBox>
  );
}

export default AuthNavMenu;