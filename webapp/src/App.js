// webapp/src/App.js
import React, { useState, useEffect } from "react";
import { io } from "socket.io-client";
import ApiService from "./services/ApiService";
import Header from "./components/common/Header";
import HomePage from "./pages/HomePage";
import ProfilePage from "./pages/ProfilePage";
import CalibrationPage from "./pages/CalibrationPage";
import GameDebug from "./pages/GameDebug";
import { GiPokerHand } from "react-icons/gi";

// Socket connection setup
const socket = io(
    window.location.hostname === 'localhost'
        ? 'http://localhost:8080'
        : window.location.origin,
    {
  rejectUnauthorized: false,
  reconnection: false,
  reconnectionAttempts: 1,
  reconnectionDelay: 10000,
});

/**
 * Main App component
 * Handles global state, navigation, and authentication
 *
 * @returns {JSX.Element} App component
 */
function App() {
  // Theme state
  const [darkMode, setDarkMode] = useState(true);

  // Navigation state
  const [currentPage, setCurrentPage] = useState("home");

  // User state
  const [user, setUser] = useState(null);
  const [isLoadingUser, setIsLoadingUser] = useState(true);

  // Socket state
  const [socketConnected, setSocketConnected] = useState(false);

  // Table state
  const [atTable, setAtTable] = useState(false);
  const [currentTable, setCurrentTable] = useState(null);
  const [isTableOwner, setIsTableOwner] = useState(false);
  const [checkingTableStatus, setCheckingTableStatus] = useState(false);

  // Window width state for responsive header
  const [windowWidth, setWindowWidth] = useState(window.innerWidth);

  // Check theme preference and load user data on mount
  useEffect(() => {
    // Apply theme class to body
    document.body.className = darkMode ? "dark-theme" : "light-theme";

    // Load user data if token exists
    const token = localStorage.getItem("token");
    if (token) {
      // First check if we have cached user data in localStorage
      const cachedUserData = localStorage.getItem("pokerUser");
      if (cachedUserData) {
        // Set initial user state from localStorage
        const parsedUser = JSON.parse(cachedUserData);
        setUser(parsedUser);
      }

      // Always load fresh data from API
      loadUserData();
    } else {
      setIsLoadingUser(false);
    }

    // Add window resize listener for responsive header
    const handleResize = () => {
      setWindowWidth(window.innerWidth);
    };

    window.addEventListener('resize', handleResize);

    // Clean up the event listener on unmount
    return () => {
      window.removeEventListener('resize', handleResize);
    };
  }, [darkMode]);

  // Load table status when user is loaded
  useEffect(() => {
    if (user) {
      checkTableStatus();
    }
  }, [user]);

  // Refresh user data when changing pages
  useEffect(() => {
    if (user && currentPage === "profile") {
      loadUserData();
    }
  }, [currentPage]);

  // Setup socket connection listeners
  useEffect(() => {
    const onConnect = () => {
      setSocketConnected(true);
      console.log("Socket connected");
    };

    const onDisconnect = () => {
      setSocketConnected(false);
      console.log("Socket disconnected");
    };

    socket.on("connect", onConnect);
    socket.on("disconnect", onDisconnect);

    // Set initial connection state
    setSocketConnected(socket.connected);

    // Clean up event listeners
    return () => {
      socket.off("connect", onConnect);
      socket.off("disconnect", onDisconnect);
    };
  }, []);

  /**
   * Load user data from API
   */
  const loadUserData = async () => {
    setIsLoadingUser(true);
    try {
      const userData = await ApiService.getCurrentUser();

      // Process avatar data from backend
      let processedAvatarData = null;

      // Check for the base64 encoded avatar
      if (userData.avatarBase64) {
        processedAvatarData = `data:image/jpeg;base64,${userData.avatarBase64}`;
      }

      // Create user object with processed avatar
      const userWithAvatar = {
        ...userData,
        avatar: processedAvatarData ? null : "default",
        customAvatar: processedAvatarData,
      };

      setUser(userWithAvatar);

      // Store complete user data in localStorage for persistence
      localStorage.setItem("pokerUser", JSON.stringify(userWithAvatar));
    } catch (error) {
      console.error("Failed to load user data:", error);
      // Clear token if authentication failed
      ApiService.clearToken();
      localStorage.removeItem("pokerUser");
    } finally {
      setIsLoadingUser(false);
    }
  };

  /**
   * Check if the user is at a table
   */
  const checkTableStatus = async () => {
    if (!user) {
      setAtTable(false);
      setCurrentTable(null);
      setIsTableOwner(false);
      setCheckingTableStatus(false);
      return;
    }

    setCheckingTableStatus(true);

    try {
      // Call the dedicated endpoint to get table status
      const tableStatus = await ApiService.getCurrentTable();

      if (tableStatus.isAtTable && tableStatus.tableId) {
        setAtTable(true);

        // Get table details if not included in the response
        const tableDetails = tableStatus.table || await ApiService.getTableById(tableStatus.tableId);
        setCurrentTable(tableDetails);

        // Check if the current user is the owner of the table
        setIsTableOwner(tableDetails.ownerId === user.id);
      } else {
        setAtTable(false);
        setCurrentTable(null);
        setIsTableOwner(false);
      }
    } catch (error) {
      console.error("Error checking table status:", error);
      setAtTable(false);
      setCurrentTable(null);
      setIsTableOwner(false);
    } finally {
      setCheckingTableStatus(false);
    }
  };

  /**
   * Handle login from ProfilePage
   * @param {Object} userData User data from login/register
   */
  const handleLogin = (userData) => {
    // Process avatar data if it exists
    let processedAvatarData = null;
    if (userData.avatarBase64) {
      processedAvatarData = `data:image/jpeg;base64,${userData.avatarBase64}`;
    }

    const userWithProcessedAvatar = {
      ...userData,
      avatar: processedAvatarData ? null : "default",
      customAvatar: processedAvatarData,
    };

    setUser(userWithProcessedAvatar);
    localStorage.setItem("pokerUser", JSON.stringify(userWithProcessedAvatar));

    // Navigate to home page after login
    setCurrentPage("home");

    // Check table status after login
    setTimeout(checkTableStatus, 500);
  };

  /**
   * Handle logout
   */
  const handleLogout = () => {
    ApiService.clearToken();
    setUser(null);
    localStorage.removeItem("pokerUser");
    setCurrentPage("home");
    setAtTable(false);
    setCurrentTable(null);
    setIsTableOwner(false);
  };

  /**
   * Handle navigation
   * @param {string} page Page to navigate to
   */
  const navigateTo = (page) => {
    setCurrentPage(page);
  };

  /**
   * Update user balance after table operations
   * @param {number} newBalance Updated balance
   */
  const updateUserBalance = (newBalance) => {
    if (user) {
      const updatedUser = {
        ...user,
        balance: newBalance
      };
      setUser(updatedUser);
      localStorage.setItem("pokerUser", JSON.stringify(updatedUser));
    }
  };

  // Render the appropriate content based on authentication state and loading state
  const renderContent = () => {
    // If still loading user data, show loading spinner
    if (isLoadingUser) {
      return (
          <div className="loading-container">
            <div className="loading-spinner"></div>
            <p>Loading...</p>
          </div>
      );
    }

    // If on profile page, show it regardless of authentication
    if (currentPage === "profile") {
      return (
          <ProfilePage
              user={user}
              onLogin={handleLogin}
              onLogout={handleLogout}
              navigateToHome={() => navigateTo("home")}
          />
      );
    }

    // Debug page - accessible to authenticated users
    if (currentPage === "debug" && user) {
      return <GameDebug user={user} />;
    }

    // If user is not logged in, show auth required message
    if (!user) {
      return (
          <div className="auth-required">
            <div className="auth-required-card">
              <GiPokerHand className="auth-icon" />
              <h2>Welcome to SPADE Poker</h2>
              <p>Please log in or create an account to continue</p>
              <button
                  className="primary-button"
                  onClick={() => navigateTo("profile")}
              >
                Login / Register
              </button>
            </div>
          </div>
      );
    }

    // User is logged in, show the appropriate page
    if (currentPage === "calibration" && atTable && isTableOwner) {
      return (
          <CalibrationPage
              socket={socket}
              socketConnected={socketConnected}
              tableId={currentTable?.id}
          />
      );
    }

    // Default to home page for logged in users
    return (
        <HomePage
            socket={socket}
            socketConnected={socketConnected}
            darkMode={darkMode}
            user={user}
            onTableStatusChange={checkTableStatus}
            onBalanceUpdate={updateUserBalance}
        />
    );
  };

  return (
      <div className={`app ${darkMode ? "dark-theme" : "light-theme"}`}>
        <Header
            darkMode={darkMode}
            setDarkMode={setDarkMode}
            currentPage={currentPage}
            navigateTo={navigateTo}
            user={user}
            socketConnected={socketConnected}
            atTable={atTable}
            isTableOwner={isTableOwner}
            windowWidth={windowWidth}
        />

        <main>
          {renderContent()}
        </main>
      </div>
  );
}

export default App;