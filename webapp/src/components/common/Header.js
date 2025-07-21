// webapp/src/components/common/Header.js
import React from "react";
import { FaMoon, FaSun, FaHome, FaUser, FaCog, FaBug } from "react-icons/fa";
import { GiPokerHand } from "react-icons/gi";
import { BsCircleFill } from "react-icons/bs";

/**
 * Header component containing the app logo, navigation, and theme toggle
 *
 * @param {Object} props - Component props
 * @param {boolean} props.darkMode - Current theme state
 * @param {function} props.setDarkMode - Function to toggle theme
 * @param {string} props.currentPage - Current active page
 * @param {function} props.navigateTo - Function to navigate between pages
 * @param {Object} props.user - Current user data
 * @param {boolean} props.socketConnected - Socket connection status
 * @param {boolean} props.atTable - Whether user is at a table
 * @param {boolean} props.isTableOwner - Whether user is the table owner
 * @param {number} props.windowWidth - Current window width for responsive layout
 * @returns {JSX.Element} Header component
 */
const Header = ({
                    darkMode,
                    setDarkMode,
                    currentPage,
                    navigateTo,
                    user,
                    socketConnected,
                    atTable,
                    isTableOwner,
                    windowWidth
                }) => {
    // Determine if we should show the title based on window width
    const isCompactMode = windowWidth < 600;

    return (
        <header className="app-header">
            <div className={`logo ${isCompactMode ? 'compact' : ''}`} onClick={() => navigateTo("home")}>
                <div className="logo-icon-container">
                    <GiPokerHand className="logo-icon" />
                </div>
                <h1 className="logo-title">SPADE</h1>
            </div>

            <div className="header-controls">
                <div className="connection-indicator">
                    <BsCircleFill
                        className={`status-circle ${socketConnected ? "connected" : "disconnected"}`}
                    />
                </div>
                {user && (
                    <div className="nav-buttons">
                        <button
                            className={`nav-button ${currentPage === "home" ? "active" : ""}`}
                            onClick={() => navigateTo("home")}
                            aria-label="Home"
                        >
                            <FaHome className="nav-icon"/>
                        </button>

                        {/* Only show Calibration tab if user is at a table AND is the table owner */}
                        {atTable && isTableOwner && (
                            <button
                                className={`nav-button ${currentPage === "calibration" ? "active" : ""}`}
                                onClick={() => navigateTo("calibration")}
                                aria-label="Calibration"
                            >
                                <FaCog className="nav-icon"/>
                            </button>
                        )}

                        {/* Debug page - accessible to all authenticated users */}
                        <button
                            className={`nav-button ${currentPage === "debug" ? "active" : ""}`}
                            onClick={() => navigateTo("debug")}
                            aria-label="Debug"
                            title="Game Debug"
                        >
                            <FaBug className="nav-icon"/>
                        </button>
                    </div>
                )}

                <button
                    className={`nav-button ${
                        currentPage === "profile" ? "active" : ""
                    }`}
                    onClick={() => navigateTo("profile")}
                    aria-label="Profile"
                >
                    {user ? (
                        <div
                            className={`mini-avatar ${
                                user.customAvatar ? "custom" : `avatar-${user.avatar}`
                            }`}
                            style={
                                user.customAvatar
                                    ? {backgroundImage: `url(${user.customAvatar})`}
                                    : {}
                            }
                        >
                            {!user.customAvatar && user.username.charAt(0).toUpperCase()}
                        </div>
                    ) : (
                        <FaUser className="nav-icon"/>
                    )}
                </button>

                <button
                    className="theme-toggle"
                    onClick={() => setDarkMode(!darkMode)}
                    aria-label="Toggle theme"
                >
                    {darkMode ? <FaSun/> : <FaMoon/>}
                </button>
            </div>
        </header>
    );
};

export default Header;