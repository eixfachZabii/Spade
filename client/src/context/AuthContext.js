// client/src/context/AuthContext.js
import React, { createContext, useContext, useState, useEffect } from 'react';
import ApiService from '../services/ApiService';

// Create context
const AuthContext = createContext();

// Hook to use the auth context
export const useAuth = () => useContext(AuthContext);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Migrate old token to new format
  const migrateToken = () => {
    const oldToken = localStorage.getItem("spade_token");
    const newToken = localStorage.getItem("token");

    if (oldToken && !newToken) {
      // Migrate old token to new format
      localStorage.setItem("token", oldToken);
      localStorage.removeItem("spade_token");
      ApiService.token = oldToken;
      console.log("Migrated token from spade_token to token");
    }
  };

  useEffect(() => {
    // Check if user is already authenticated
    const checkAuth = async () => {
      try {
        // First, migrate any old tokens
        migrateToken();

        if (ApiService.isAuthenticated()) {
          const userData = await ApiService.getCurrentUser();
          setUser(userData);
        }
      } catch (err) {
        console.error("Failed to fetch user data:", err);
        ApiService.clearToken();
        setUser(null);
      } finally {
        setLoading(false);
      }
    };

    checkAuth();
  }, []);

  // Login function
  const login = async (credentials) => {
    setError(null);
    setLoading(true);

    try {
      const data = await ApiService.login(credentials);
      // The backend returns { token, user } - set user from the response
      setUser(data.user);
      return data;
    } catch (err) {
      const errorMessage = err.message || "Login failed";
      setError(errorMessage);
      throw new Error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  // Register function
  const register = async (userData) => {
    setError(null);
    setLoading(true);

    try {
      const data = await ApiService.register(userData);
      return data;
    } catch (err) {
      const errorMessage = err.message || "Registration failed";
      setError(errorMessage);
      throw new Error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  // Logout function
  const logout = () => {
    ApiService.clearToken();
    setUser(null);
    setError(null);
    // Redirect to login page
    window.location.href = '/authentication/sign-in';
  };

  // Refresh user data
  const refreshUser = async () => {
    if (!ApiService.isAuthenticated()) {
      setUser(null);
      return;
    }

    try {
      const userData = await ApiService.getCurrentUser();
      setUser(userData);
      return userData;
    } catch (err) {
      console.error("Failed to refresh user data:", err);
      ApiService.clearToken();
      setUser(null);
      throw err;
    }
  };

  const contextValue = {
    user,
    loading,
    error,
    login,
    register,
    logout,
    refreshUser,
    isAuthenticated: !!user && ApiService.isAuthenticated(),
  };

  return (
      <AuthContext.Provider value={contextValue}>
        {children}
      </AuthContext.Provider>
  );
};

export default AuthContext;