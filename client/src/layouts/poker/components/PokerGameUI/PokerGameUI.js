// client/src/layouts/poker/components/PokerGameUI/PokerGameUI.js

import React, { useState, useEffect, useRef } from "react";
import Player from "../player/Player";
import PokerTable from "../table/PokerTable";
import { getPlayerPositions } from "../../utils/positionUtils";
import { loadCardImage } from "../../utils/cardUtils";
import LoadingSpinner from "../../utils/loadingSpinner/LoadingSpinner";
import PokerGameService from "../../../../services/PokerGameService";
import "./PokerGameUI.css";

// Import for dashboard data (preserve existing functionality)
import { lineChartDataDashboard } from "../../../dashboard/data/lineChartData";

const PokerGameUI = ({ isFullscreen, isMobile }) => {
  const [players, setPlayers] = useState([]);
  const [playerPositions, setPlayerPositions] = useState([]);
  const [pot, setPot] = useState(null);
  const [communityCards, setCommunityCards] = useState([]);
  const [prevCommunityCards, setPrevCommunityCards] = useState([]);
  const [dealerIndex, setDealerIndex] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [connectionError, setConnectionError] = useState(false);
  const [turningCards, setTurningCards] = useState([]);
  const [currentTableId, setCurrentTableId] = useState(null);
  const [gameActive, setGameActive] = useState(false);
  const containerRef = useRef(null);

  // Counter for chart data sync (preserve existing functionality)
  let syncCallCount = 0;

  // Function to sync players with lineChartDataDashboard and update balance
  const syncChartDataWithPlayers = (players) => {
    players.forEach((player) => {
      const playerData = lineChartDataDashboard.find(
          (data) => data.name === player.name
      );

      let serverPnlData = player.pnl || [];

      if (!playerData) {
        // Add player to chart data if they don't exist
        lineChartDataDashboard.push({
          name: player.name,
          data: serverPnlData,
        });
      } else {
        // Add new data points
        const existingTimestamps = new Set(playerData.data.map((d) => d[0]));
        serverPnlData.forEach(([timestamp, pnl]) => {
          if (!existingTimestamps.has(timestamp)) {
            playerData.data.push([timestamp, pnl]);
          }
        });
      }
    });
  };

  // Update player positions based on container size
  const updatePlayerPositions = () => {
    if (containerRef.current) {
      const { width, height } = containerRef.current.getBoundingClientRect();

      // Get positions based on current container dimensions
      const positions = getPlayerPositions(players.length, width, height);
      setPlayerPositions(positions);
    }
  };

  // Check if user is at a table and get table ID
  const getCurrentTable = async () => {
    try {
      const response = await PokerGameService.getCurrentTable();
      if (response.isAtTable && response.tableId) {
        setCurrentTableId(response.tableId);
        return response.tableId;
      } else {
        setCurrentTableId(null);
        return null;
      }
    } catch (error) {
      console.error("Error getting current table:", error);
      setCurrentTableId(null);
      return null;
    }
  };

  // Fetch game data from backend
  const fetchGameData = async (tableId) => {
    if (!tableId) return;

    try {
      const response = await PokerGameService.getGameStatus(tableId);

      if (response.success && response.gameState) {
        const transformedData = PokerGameService.transformGameData(response.gameState);

        if (transformedData) {
          // Check for new community cards to trigger animations
          const currentVisibleCards = transformedData.communityCards.filter(card => card !== null);
          const prevVisibleCards = prevCommunityCards.filter(card => card !== null);

          // Only trigger animation if we have genuinely new cards (more cards than before)
          // AND the previous state wasn't empty (to avoid animating on initial load)
          if (currentVisibleCards.length > prevVisibleCards.length && prevVisibleCards.length > 0) {
            const newCardIndices = [];
            for (let i = prevVisibleCards.length; i < currentVisibleCards.length; i++) {
              newCardIndices.push(i);
            }
            setTurningCards(newCardIndices);
          }
          setPrevCommunityCards(transformedData.communityCards);

          // Update state with backend data
          setPlayers(transformedData.players);
          setCommunityCards(transformedData.communityCards);
          setPot(transformedData.pot);
          setDealerIndex(transformedData.dealerIndex);
          setGameActive(transformedData.gameActive);

          // Sync with chart data periodically (preserve existing functionality)
          syncCallCount++;
          if (syncCallCount >= 10) {
            syncChartDataWithPlayers(transformedData.players);
            syncCallCount = 0;
          }
        }

        setConnectionError(false);
      } else {
        // No active game, clear game state but don't show error
        setPlayers([]);
        setCommunityCards([null, null, null, null, null]); // Always 5 cards
        setPot(0);
        setDealerIndex(null);
        setGameActive(false);
        setConnectionError(false);
      }
    } catch (error) {
      console.error("Error fetching game data:", error);

      if (error.message === "AUTHENTICATION_REQUIRED") {
        setConnectionError(false);
        // Clear game state when authentication is required
        setPlayers([]);
        setCommunityCards([null, null, null, null, null]); // Always 5 cards
        setPot(0);
        setDealerIndex(null);
        setGameActive(false);
        return;
      }

      setConnectionError(true);

      // Only load demo data if we're in development mode and not due to auth issues
      if (isLoading) {
        loadDemoData();
      }
    }
  };

  // Load demo data for development/preview purposes
  const loadDemoData = () => {
    console.log("Loading demo data due to connection error");
    const demoData = PokerGameService.getDemoData();

    setPlayers(demoData.players);
    setCommunityCards(demoData.communityCards);
    setPot(demoData.pot);
    setDealerIndex(demoData.dealerIndex);
    setGameActive(demoData.gameActive);
    setConnectionError(true); // Keep connection error visible for demo mode
    setIsLoading(false);
  };

  // Initialize and poll game data
  useEffect(() => {
    const initializeGame = async () => {
      setIsLoading(true);

      // Check if user is at a table
      const tableId = await getCurrentTable();

      if (tableId) {
        // User is at a table, fetch game data
        await fetchGameData(tableId);
      } else {
        // User not at table, show empty state instead of demo data
        setPlayers([]);
        setCommunityCards([null, null, null, null, null]);
        setPot(0);
        setDealerIndex(null);
        setGameActive(false);
        setConnectionError(false);
      }

      setIsLoading(false);
    };

    initializeGame();

    // Set up polling interval
    const interval = setInterval(async () => {
      if (currentTableId) {
        await fetchGameData(currentTableId);
      } else {
        // Check if user joined a table
        const tableId = await getCurrentTable();
        if (tableId) {
          await fetchGameData(tableId);
        }
      }
    }, 2000); // Poll every 2 seconds

    return () => clearInterval(interval);
  }, [currentTableId]);

  // Handle window resize and fullscreen changes
  useEffect(() => {
    const handleResize = () => {
      updatePlayerPositions();
    };

    window.addEventListener("resize", handleResize);
    handleResize(); // Initial position calculation

    return () => window.removeEventListener("resize", handleResize);
  }, [players.length, isFullscreen]);

  // Update positions after players or container change
  useEffect(() => {
    updatePlayerPositions();
  }, [players, isFullscreen]);

  // Reset turning cards after animation completes
  useEffect(() => {
    if (turningCards.length > 0) {
      const timer = setTimeout(() => {
        setTurningCards([]);
      }, 600); // Animation duration

      return () => clearTimeout(timer);
    }
  }, [turningCards]);

  return (
      <div
          ref={containerRef}
          className={`poker-game-ui ${isFullscreen ? 'fullscreen' : ''} ${isMobile ? 'mobile' : ''}`}
      >
        {isLoading ? (
            <LoadingSpinner />
        ) : !currentTableId && !connectionError ? (
            // Enhanced waiting state with empty table
            <div className="waiting-state">
              <PokerTable
                  pot="Waiting for players..."
                  isFullscreen={isFullscreen}
              >
                {/* Entertaining waiting message */}
                <div className="waiting-overlay">
                  {/* Animated floating cards */}
                  <div className="floating-cards">
                    <div className="floating-card card-1">🂡</div>
                    <div className="floating-card card-2">🂮</div>
                    <div className="floating-card card-3">🃁</div>
                    <div className="floating-card card-4">🃞</div>
                  </div>
                </div>

                {/* Show empty community cards */}
                <div className="community-cards">
                  {[...Array(5)].map((_, index) => (
                      <div
                          key={index}
                          className="community-card-container waiting-card"
                          style={{
                            animationDelay: `${index * 0.2}s`
                          }}
                      >
                        <div className="community-card-placeholder waiting-placeholder"></div>
                      </div>
                  ))}
                </div>
              </PokerTable>
            </div>
        ) : (
            <PokerTable
                pot={pot !== null ? pot : "Loading..."}
                isFullscreen={isFullscreen}
            >
              {/* Connection error notification */}
              {connectionError && (
                  <div className="connection-error">
              <span>
                {currentTableId
                    ? "Connection to game server failed. Using demo data."
                    : "Not connected to any table. Showing demo data."
                }
              </span>
                  </div>
              )}

              {/* Game status indicator */}
              {currentTableId && !connectionError && (
                  <div className="game-status">
              <span className={gameActive ? "game-active" : "game-inactive"}>
                {gameActive ? "🟢 Game Active" : "⏸️ Game Paused"}
              </span>
                  </div>
              )}

              {/* Render player components */}
              {players.map((player, index) => {
                const position = playerPositions[index];
                const isDealer = index === dealerIndex;

                return (
                    position && (
                        <Player
                            key={`player-${player.seatPosition || index}`}
                            player={player}
                            position={position}
                            isDealer={isDealer}
                            isFullscreen={isFullscreen}
                        />
                    )
                );
              })}

              {/* Render community cards - always show 5 cards */}
              <div className="community-cards">
                {communityCards.map((card, index) => (
                    <div
                        key={index}
                        className="community-card-container"
                        style={{
                          // Stagger the card appearance slightly
                          animationDelay: `${index * 0.15}s`
                        }}
                    >
                      {card ? (
                          <img
                              src={loadCardImage(card.rank, card.suit, card.faceUp)}
                              alt={card.faceUp ? `${card.rank} of ${card.suit}` : "Card back"}
                              className={`community-card ${turningCards.includes(index) ? 'turning' : ''}`}
                          />
                      ) : (
                          <div className="community-card-placeholder"></div>
                      )}
                    </div>
                ))}
              </div>
            </PokerTable>
        )}
      </div>
  );
};

export default PokerGameUI;