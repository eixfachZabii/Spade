import React, { useState, useEffect } from "react";
import { Card, useMediaQuery, IconButton } from "@mui/material";
import PokerGameUI from "./components/PokerGameUI/PokerGameUI";
import { FullscreenExitRounded, FullscreenRounded } from "@mui/icons-material";
import "./PokerGameBox.css";

function PokerGameBox() {
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [isTransitioning, setIsTransitioning] = useState(false);
  const isSmallScreen = useMediaQuery("(max-width: 960px)");
  const isMobileScreen = useMediaQuery("(max-width: 600px)");

  // Handle fullscreen transition
  const toggleFullscreen = () => {
    setIsTransitioning(true);
    setIsFullscreen(!isFullscreen);

    // Reset transitioning state after animation completes
    setTimeout(() => {
      setIsTransitioning(false);
    }, 400); // Match transition duration
  };

  // Handle escape key to exit fullscreen
  useEffect(() => {
    const handleEscKey = (e) => {
      if (e.key === "Escape" && isFullscreen) {
        toggleFullscreen();
      }
    };

    window.addEventListener("keydown", handleEscKey);
    return () => window.removeEventListener("keydown", handleEscKey);
  }, [isFullscreen]);

  return (
    <div
      className={`poker-game-container ${isFullscreen ? 'fullscreen' : ''} ${isTransitioning ? 'transitioning' : ''}`}
      style={{
        // Increased height by 10% when not in fullscreen
        height: isFullscreen
          ? "100vh"
          : isSmallScreen
          ? "calc(100vw * 0.95)"
          : "calc(100vw * 0.825)", // Increased from 0.75 to 0.825 (10% more)
        minHeight: isSmallScreen ? "650px" : "750px", // Adjusted min-height
        maxHeight: isFullscreen ? "100vh" : "90vh",
      }}
    >
      <Card
        className="poker-game-card"
        sx={{
          backgroundColor: "rgba(16, 20, 24, 0.9)",
          backgroundImage: "linear-gradient(to bottom, rgba(39, 49, 56, 0.8), rgba(16, 20, 24, 0.9))",
          borderRadius: "20px",
          boxShadow: isFullscreen
            ? "0 0 40px rgba(0, 0, 0, 0.7), inset 0 0 60px rgba(26, 138, 211, 0.1)"
            : "0 8px 24px rgba(0, 0, 0, 0.4), inset 0 0 20px rgba(26, 138, 211, 0.1)",
          border: "1px solid rgba(82, 172, 250, 0.2)",
          overflow: "hidden",
        }}
      >
        {/* Fullscreen Button */}
        <IconButton
          onClick={toggleFullscreen}
          className="fullscreen-button"
          aria-label={isFullscreen ? "Exit fullscreen" : "Enter fullscreen"}
        >
          {isFullscreen ? (
            <FullscreenExitRounded />
          ) : (
            <FullscreenRounded />
          )}
        </IconButton>

        {/* Poker-UI Rendering */}
        <PokerGameUI isFullscreen={isFullscreen} isMobile={isMobileScreen} />
      </Card>
    </div>
  );
}

export default PokerGameBox;