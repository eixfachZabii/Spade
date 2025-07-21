import React, { useState, useEffect, useRef } from "react";
import { FaTrash, FaSignOutAlt, FaShieldAlt } from "react-icons/fa";
import ApiService from "../services/ApiService";
import GameWebSocketService from "../services/GameWebSocketService";
import ConfirmationModal from "../components/common/ConfirmationModal";
import LobbySystem from "./LobbySystem";
import CardScanner from "../components/game/CardScanner";
import ActionPanel from "../components/game/ActionPanel";

/**
 * Home page component that displays either the lobby or the poker table UI
 *
 * @param {Object} props Component props
 * @param {Object} props.socket Socket.io connection
 * @param {boolean} props.socketConnected Socket connection status
 * @param {boolean} props.darkMode Dark mode state
 * @param {Object} props.user Current user data
 * @param {Function} props.onTableStatusChange Callback when table status changes
 * @param {Function} props.onBalanceUpdate Callback to update user balance in parent component
 * @returns {JSX.Element} HomePage component
 */
function HomePage({ socket, socketConnected, darkMode, user, onTableStatusChange, onBalanceUpdate }) {
  // State for camera and scanning
  const [isLoading, setIsLoading] = useState(false);
  const [actionStatus, setActionStatus] = useState("");

  // State for current table
  const [currentTable, setCurrentTable] = useState(null);
  const [atTable, setAtTable] = useState(false);
  const [checkingTableStatus, setCheckingTableStatus] = useState(false);
  const [currChips, setCurrChips] = useState(0);

  // Game state - Updated to include player-specific betting info
  const [gameState, setGameState] = useState(null);
  const [isMyTurn, setIsMyTurn] = useState(false);
  const [currentBet, setCurrentBet] = useState(0);
  const [playerCurrentBet, setPlayerCurrentBet] = useState(0); // NEW: Player's current contribution
  const [pot, setPot] = useState(0);
  const [playerCount, setPlayerCount] = useState(0);

  // WebSocket service
  const gameWebSocketRef = useRef(null);
  const [wsConnected, setWsConnected] = useState(false);

  // Error state
  const [error, setError] = useState("");

  // Delete table confirmation state
  const [showDeleteConfirmation, setShowDeleteConfirmation] = useState(false);

  // Check if the user is at a table when the component mounts or user changes
  useEffect(() => {
    if (user) {
      checkTableStatus();
      updateChips();
    }
  }, [user]);

  // Initialize WebSocket when at table
  useEffect(() => {
    if (atTable && currentTable && user) {
      initializeWebSocket();
    } else if (gameWebSocketRef.current) {
      // Cleanup WebSocket when leaving table
      gameWebSocketRef.current.disconnect();
      gameWebSocketRef.current = null;
      setWsConnected(false);
    }

    return () => {
      if (gameWebSocketRef.current) {
        gameWebSocketRef.current.disconnect();
      }
    };
  }, [atTable, currentTable, user]);

  // Initialize WebSocket connection
  const initializeWebSocket = async () => {
    if (!user || !currentTable) return;

    try {
      gameWebSocketRef.current = new GameWebSocketService();

      // Connect to WebSocket server
      await gameWebSocketRef.current.connectWithFallback(ApiService.token);
      setWsConnected(true);

      // Join the table
      gameWebSocketRef.current.joinTable(currentTable.id);

      // Register event handlers
      gameWebSocketRef.current.on('GAME_STARTED', (event) => {
        console.log('Game started!', event);
        fetchGameState();
      });

      gameWebSocketRef.current.on('GAME_ENDED', (event) => {
        console.log('Game ended!', event);
        setGameState(null);
        setIsMyTurn(false);
      });

      gameWebSocketRef.current.on('STAGE_CHANGED', (event) => {
        console.log('Stage changed!', event);
        if (event.payload) {
          updateGameStateFromPayload(event.payload);
        }
      });

      gameWebSocketRef.current.on('PLAYER_TURN', (event) => {
        console.log('Player turn!', event);
        fetchGameState();
      });

      gameWebSocketRef.current.on('PLAYER_ACTION', (event) => {
        console.log('Player action!', event);
        fetchGameState();
      });

      // Fetch initial game state
      fetchGameState();
    } catch (error) {
      console.error('Failed to connect to game server:', error);
      setError('Failed to connect to game server');
    }
  };

  // Fetch current game state
  const fetchGameState = async () => {
    if (!currentTable) return;

    try {
      const response = await ApiService.getGameStatus(currentTable.id);

      if (response.success && response.gameState) {
        updateGameStateFromPayload(response.gameState);
      } else {
        // No active game
        setGameState(null);
        setIsMyTurn(false);
        setPot(0);
        setCurrentBet(0);
        setPlayerCurrentBet(0); // Reset player bet
      }
    } catch (error) {
      console.error('Error fetching game state:', error);
    }
  };

  // UPDATED: Update local state from game state payload with correct betting logic
  const updateGameStateFromPayload = (gameStateData) => {
    setGameState(gameStateData);

    // Update game info
    setPot(gameStateData.pot || 0);
    setCurrentBet(gameStateData.currentBet || 0);
    setPlayerCount(gameStateData.players?.length || 0);

    // Find current player's data
    const myPlayer = gameStateData.players?.find(p => p.username === user.username);

    if (myPlayer) {
      // Check if it's my turn
      setIsMyTurn(myPlayer.playerTurn || false);

      // UPDATED: Set player's current bet contribution for this round
      setPlayerCurrentBet(myPlayer.currentBet || 0);

      // Update chips if different
      if (myPlayer.chips !== currChips) {
        setCurrChips(myPlayer.chips);
      }

      // Debug logging for betting state
      console.log('Betting State Update:', {
        globalCurrentBet: gameStateData.currentBet || 0,
        playerCurrentBet: myPlayer.currentBet || 0,
        toCall: (gameStateData.currentBet || 0) - (myPlayer.currentBet || 0),
        canCheck: (gameStateData.currentBet || 0) === (myPlayer.currentBet || 0),
        playerTurn: myPlayer.playerTurn,
        playerStatus: myPlayer.status
      });
    } else {
      // Reset if player not found
      setIsMyTurn(false);
      setPlayerCurrentBet(0);
    }
  };

  // Update chips
  const updateChips = async () => {
    if (!user) return;

    try {
      const chips = await ApiService.getCurrChips();
      setCurrChips(chips);
    } catch (error) {
      console.error("Error getting chips:", error);
    }
  };

  // Get the updated user balance from the server
  const fetchAndUpdateUserBalance = async () => {
    if (!user) return;

    try {
      const userData = await ApiService.getCurrentUser();
      if (userData && onBalanceUpdate) {
        onBalanceUpdate(userData.balance);
      }
    } catch (error) {
      console.error("Error fetching updated user balance:", error);
    }
  };

  // Function to check table status using the dedicated endpoint
  const checkTableStatus = async () => {
    if (!user) {
      setAtTable(false);
      setCurrentTable(null);
      setCheckingTableStatus(false);
      return;
    }

    setCheckingTableStatus(true);

    try {
      // Call the dedicated endpoint to get table status
      const tableStatus = await ApiService.getCurrentTable();

      if (tableStatus.isAtTable && tableStatus.tableId) {
        setAtTable(true);
        const tableDetails = tableStatus.table || await ApiService.getTableById(tableStatus.tableId);
        setCurrentTable(tableDetails);
        setPlayerCount(tableDetails.currentPlayers || 0);
      } else {
        setAtTable(false);
        setCurrentTable(null);
      }

      // Notify parent component about table status change
      if (onTableStatusChange) {
        onTableStatusChange();
      }
    } catch (error) {
      console.error("Error checking table status:", error);
      setAtTable(false);
      setCurrentTable(null);
    } finally {
      setCheckingTableStatus(false);
    }
  };

  // Function to join a table
  const handleJoinTable = async (tableId, buyIn) => {
    if (!user) {
      setError("You must be logged in to join a table");
      setTimeout(() => setError(""), 3000);
      return;
    }

    setIsLoading(true);
    setError("");

    try {
      // If buy-in is provided, it's a new join
      if (buyIn) {
        await ApiService.joinTable(tableId, buyIn);

        // Get the updated balance after joining
        await fetchAndUpdateUserBalance();
      }

      // Check table status again to update the UI
      await checkTableStatus();
      await updateChips();
    } catch (error) {
      setError("Failed to join table: " + (error.message || "Unknown error"));
      setTimeout(() => setError(""), 3000);
    } finally {
      setIsLoading(false);
    }
  };

  // Function to leave a table
  const handleLeaveTable = async () => {
    if (!currentTable) return;

    setIsLoading(true);
    setError("");

    try {
      await ApiService.leaveTable(currentTable.id);

      // Get the updated balance after leaving
      await fetchAndUpdateUserBalance();

      // Check table status again to update the UI
      await checkTableStatus();
      await updateChips();
    } catch (error) {
      setError("Failed to leave table: " + (error.message || "Unknown error"));
      setTimeout(() => setError(""), 3000);
    } finally {
      setIsLoading(false);
    }
  };

  // Function to delete a table (owner only)
  const handleDeleteTable = async () => {
    if (!currentTable) return;

    setIsLoading(true);
    setError("");
    setShowDeleteConfirmation(false);

    try {
      await ApiService.deleteTable(currentTable.id);

      // Get the updated balance after table deletion
      await fetchAndUpdateUserBalance();

      // Show success message
      setActionStatus("Table successfully deleted");

      // Check table status to update UI (user will no longer be at a table)
      await checkTableStatus();

      // Clear success message after delay
      setTimeout(() => setActionStatus(""), 3000);
    } catch (error) {
      setError("Failed to delete table: " + (error.message || "Unknown error"));
      setTimeout(() => setError(""), 3000);
    } finally {
      setIsLoading(false);
    }
  };

  // Function to start game (owner only)
  const handleStartGame = async () => {
    if (!currentTable) return;

    setIsLoading(true);
    setError("");

    try {
      const response = await ApiService.startGame(currentTable.id);

      if (response.success) {
        setActionStatus("Game started successfully!");
        // Game state will be updated via WebSocket
      } else {
        setError(response.message || "Failed to start game");
      }
    } catch (error) {
      setError("Failed to start game: " + (error.message || "Unknown error"));
    } finally {
      setIsLoading(false);
      setTimeout(() => setActionStatus(""), 3000);
    }
  };

  // Function to end game (owner only)
  const handleEndGame = async () => {
    if (!currentTable) return;

    setIsLoading(true);
    setError("");

    try {
      const response = await ApiService.endGame(currentTable.id);

      if (response.success) {
        setActionStatus("Game ended successfully!");
        setGameState(null);
        setIsMyTurn(false);
      } else {
        setError(response.message || "Failed to end game");
      }
    } catch (error) {
      setError("Failed to end game: " + (error.message || "Unknown error"));
    } finally {
      setIsLoading(false);
      setTimeout(() => setActionStatus(""), 3000);
    }
  };

  // Function to send poker actions
  const handleAction = async (action, amount = null) => {
    if (!gameWebSocketRef.current || !wsConnected) {
      setError("Not connected to game server");
      return;
    }

    setIsLoading(true);

    // UPDATED: Show more specific action status messages
    let statusMessage = `Processing: ${action}...`;
    if (action === 'call') {
      const toCall = currentBet - playerCurrentBet;
      statusMessage = `Calling $${toCall}...`;
    } else if (action === 'raise' && amount) {
      statusMessage = `Raising by $${amount}...`;
    }

    setActionStatus(statusMessage);

    try {
      // Send action via WebSocket
      const actionData = {
        action: action.toUpperCase(),
        amount: amount
      };

      gameWebSocketRef.current.sendAction(currentTable.id, actionData);

      // The response will come via WebSocket events
      // Clear status after a delay
      setTimeout(() => setActionStatus(""), 2000);
    } catch (error) {
      setError("Failed to send action: " + error.message);
      setTimeout(() => setError(""), 3000);
    } finally {
      setIsLoading(false);
    }
  };

  // Show loading state while checking table status
  if (checkingTableStatus) {
    return (
        <div className="loading-container">
          <div className="loading-spinner"></div>
          <p>Loading table status...</p>
        </div>
    );
  }

  // Conditionally render either the lobby system or the poker table
  return (
      <div className={`main-content ${!atTable ? "lobby-layout" : ""}`}>
        {error && <div className="error-message global-error">{error}</div>}

        {!atTable ? (
            // Lobby System when not at a table
            <LobbySystem
                user={user}
                onJoinTable={handleJoinTable}
                currentTable={currentTable}
                darkMode={darkMode}
                onBalanceUpdate={onBalanceUpdate}
            />
        ) : (
            // Poker Table UI when at a table - using grid layout
            <div className="poker-table-layout">
              {/* Header area with table name and controls */}
              <div className="table-header" style={{ gridArea: "header", width: "100%" }}>
                <h2>Table: {currentTable?.name}</h2>
                <div className="table-info">
                  {currentTable?.ownerId === user?.id && (
                      <>
                  <span className="owner-badge" title="You are the owner of this table">
                    <FaShieldAlt className="owner-icon" />
                  </span>
                        <button
                            className="delete-table-button"
                            onClick={() => setShowDeleteConfirmation(true)}
                            disabled={isLoading || gameState?.gameActive}
                            title={gameState?.gameActive ? "Cannot delete during active game" : "Delete Table"}
                        >
                          <FaTrash />
                        </button>
                      </>
                  )}
                  <button
                      className="leave-table-button"
                      onClick={handleLeaveTable}
                      disabled={isLoading || gameState?.gameActive}
                      title={gameState?.gameActive ? "Cannot leave during active game" : "Leave Table"}
                  >
                    <FaSignOutAlt />
                  </button>
                </div>
              </div>

              {/* Scanner area */}
              <div style={{ gridArea: "scanner", width: "100%" }}>
                <CardScanner
                    socketConnected={socketConnected}
                    socket={socket}
                    actionStatus={actionStatus}
                    isLoading={isLoading}
                />
              </div>

              {/* Action panel area - UPDATED: Pass player betting info */}
              <div style={{ gridArea: "actions", width: "100%" }}>
                <ActionPanel
                    chips={currChips}
                    isLoading={isLoading}
                    actionStatus={actionStatus}
                    onAction={handleAction}
                    isTableOwner={currentTable?.ownerId === user?.id}
                    gameActive={gameState?.gameActive || false}
                    onStartGame={handleStartGame}
                    onEndGame={handleEndGame}
                    playerCount={playerCount}
                    isMyTurn={isMyTurn}
                    currentBet={currentBet}
                    playerCurrentBet={playerCurrentBet} // NEW: Pass player's current contribution
                    pot={pot}
                />
              </div>

              {/* Confirmation Modal for deleting table */}
              <ConfirmationModal
                  show={showDeleteConfirmation}
                  title="Delete Table"
                  message="Are you sure you want to delete this table? All players will be removed."
                  confirmText="Delete Table"
                  cancelText="Cancel"
                  confirmButtonClass="danger"
                  onConfirm={handleDeleteTable}
                  onCancel={() => setShowDeleteConfirmation(false)}
              />
            </div>
        )}
      </div>
  );
}

export default HomePage;