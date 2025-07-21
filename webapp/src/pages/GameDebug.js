// webapp/src/pages/GameDebug.js
import React, { useState, useEffect } from "react";
import { FaGamepad, FaUsers, FaCoins, FaClock } from "react-icons/fa";
import ApiService from "../services/ApiService";

/**
 * Game Debug Page - Displays full game state for testing and debugging
 *
 * @param {Object} props Component props
 * @param {Object} props.user Current user data
 * @returns {JSX.Element} GameDebug component
 */
function GameDebug({ user }) {
    const [gameState, setGameState] = useState(null);
    const [tableId, setTableId] = useState("");
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState("");
    const [lastUpdated, setLastUpdated] = useState(null);
    const [autoRefresh, setAutoRefresh] = useState(false);

    // Auto-refresh interval
    useEffect(() => {
        let interval;
        if (autoRefresh && tableId) {
            interval = setInterval(() => {
                fetchGameState();
            }, 2000); // Refresh every 2 seconds
        }
        return () => {
            if (interval) clearInterval(interval);
        };
    }, [autoRefresh, tableId]);

    // Fetch current table if user is at one
    useEffect(() => {
        if (user) {
            checkCurrentTable();
        }
    }, [user]);

    const checkCurrentTable = async () => {
        try {
            const tableStatus = await ApiService.getCurrentTable();
            if (tableStatus.isAtTable && tableStatus.tableId) {
                setTableId(tableStatus.tableId.toString());
                await fetchGameState(tableStatus.tableId);
            }
        } catch (error) {
            console.log("No current table found");
        }
    };

    const fetchGameState = async (customTableId = null) => {
        const targetTableId = customTableId || tableId;
        if (!targetTableId) {
            setError("Please enter a table ID");
            return;
        }

        setIsLoading(true);
        setError("");

        try {
            const response = await ApiService.getGameStatus(targetTableId);

            if (response.success && response.gameState) {
                setGameState(response.gameState);
                setLastUpdated(new Date());
            } else if (response.hasActiveGame === false) {
                setGameState(null);
                setError("No active game found for this table");
            } else {
                setError(response.message || "Failed to fetch game state");
            }
        } catch (err) {
            setError("Error fetching game state: " + err.message);
            setGameState(null);
        } finally {
            setIsLoading(false);
        }
    };

    const handleRefresh = () => {
        fetchGameState();
    };

    /**
     * Renders a card string into a visual element with proper rank and suit.
     * Maps full rank names (TEN, ACE) and '10' to single characters (T, A).
     *
     * @param {string} cardString The card string (e.g., "TENH", "AS", "2C").
     * @returns {JSX.Element} The rendered card element.
     */
    const renderCard = (cardString) => {
        if (!cardString) return <span className="debug-card empty">--</span>;

        const inputRankStr = cardString.slice(0, -1);
        const suit = cardString.slice(-1);

        let rank;
        switch (inputRankStr.toUpperCase()) {
            case '2': case 'TWO':   rank = '2'; break;
            case '3': case 'THREE': rank = '3'; break;
            case '4': case 'FOUR':  rank = '4'; break;
            case '5': case 'FIVE':  rank = '5'; break;
            case '6': case 'SIX':   rank = '6'; break;
            case '7': case 'SEVEN': rank = '7'; break;
            case '8': case 'EIGHT': rank = '8'; break;
            case '9': case 'NINE':  rank = '9'; break;
            case '10': case 'TEN':  rank = 'T'; break;
            case 'J': case 'JACK':  rank = 'J'; break;
            case 'Q': case 'QUEEN': rank = 'Q'; break;
            case 'K': case 'KING':  rank = 'K'; break;
            case 'A': case 'ACE':   rank = 'A'; break;
            default: rank = inputRankStr; // Fallback if format is unexpected
        }

        const suitSymbol = {
            'S': '♠',
            'H': '♥',
            'D': '♦',
            'C': '♣'
        }[suit] || suit;

        const isRed = suit === 'H' || suit === 'D';

        return (
            <span className={`debug-card ${isRed ? 'red' : 'black'}`}>
                {rank}{suitSymbol}
            </span>
        );
    };


    const formatStage = (stage) => {
        if (!stage) return "N/A";
        return stage.replace(/_/g, ' ').toLowerCase()
            .replace(/\b\w/g, l => l.toUpperCase());
    };

    const getHandDescription = (holeCards) => {
        if (!holeCards || holeCards.length !== 2) return "Invalid hand";

        const [card1, card2] = holeCards;
        const rank1 = card1.slice(0, -1);
        const rank2 = card2.slice(0, -1);
        const suit1 = card1.slice(-1);
        const suit2 = card2.slice(-1);

        // Map ranks to sortable/displayable format (consider using a helper or the renderCard logic)
        const mapRank = (r) => {
            switch (r.toUpperCase()) {
                case 'TEN': return '10';
                case 'ACE': return 'A';
                case 'KING': return 'K';
                case 'QUEEN': return 'Q';
                case 'JACK': return 'J';
                // Add other mappings if necessary, or use a consistent input format
                default: return r;
            }
        }

        const displayRank1 = mapRank(rank1);
        const displayRank2 = mapRank(rank2);


        // Check for pairs
        if (displayRank1 === displayRank2) {
            return `Pair of ${displayRank1}s`;
        }

        // Check for suited
        const suited = suit1 === suit2 ? " suited" : " offsuit";

        // Order ranks for consistent display
        const ranks = [displayRank1, displayRank2].sort((a, b) => {
            const order = ['2','3','4','5','6','7','8','9','10','J','Q','K','A'];
            return order.indexOf(b) - order.indexOf(a);
        });

        return `${ranks[0]}-${ranks[1]}${suited}`;
    };

    return (
        <div className="debug-container">
            <div className="debug-header">
                <h1><FaGamepad /> Game State Debug</h1>
                <p>Real-time game state viewer for testing and debugging</p>
            </div>

            {/* Controls */}
            <div className="debug-controls">
                <div className="control-group">
                    <label htmlFor="tableId">Table ID:</label>
                    <input
                        id="tableId"
                        type="number"
                        value={tableId}
                        onChange={(e) => setTableId(e.target.value)}
                        placeholder="Enter table ID"
                        className="debug-input"
                    />
                    <button
                        onClick={handleRefresh}
                        disabled={isLoading || !tableId}
                        className="debug-button primary"
                    >
                        {isLoading ? "Loading..." : "Fetch State"}
                    </button>
                </div>

                <div className="control-group">
                    <label className="checkbox-label">
                        <input
                            type="checkbox"
                            checked={autoRefresh}
                            onChange={(e) => setAutoRefresh(e.target.checked)}
                        />
                        Auto-refresh (2s)
                    </label>
                    {lastUpdated && (
                        <span className="last-updated">
              <FaClock /> Last: {lastUpdated.toLocaleTimeString()}
            </span>
                    )}
                </div>
            </div>

            {/* Error Display */}
            {error && (
                <div className="debug-error">
                    ⚠️ {error}
                </div>
            )}

            {/* Game State Display */}
            {gameState ? (
                <div className="debug-content">
                    {/* Game Overview */}
                    <div className="debug-section">
                        <h2>🎮 Game Overview</h2>
                        <div className="info-grid">
                            <div className="info-item">
                                <strong>Table ID:</strong> {gameState.tableId}
                            </div>
                            <div className="info-item">
                                <strong>Game ID:</strong> {gameState.gameId || "N/A"}
                            </div>
                            <div className="info-item">
                                <strong>Round:</strong> #{gameState.roundNumber || 0}
                            </div>
                            <div className="info-item">
                                <strong>Stage:</strong> {formatStage(gameState.currentStage)}
                            </div>
                            <div className="info-item">
                                <strong>Active:</strong> {gameState.gameActive ? "✅ Yes" : "❌ No"}
                            </div>
                            <div className="info-item">
                                <strong>Players:</strong> {gameState.players?.length || 0}
                            </div>
                        </div>
                    </div>

                    {/* Pot & Betting */}
                    <div className="debug-section">
                        <h2><FaCoins /> Pot & Betting</h2>
                        <div className="info-grid">
                            <div className="info-item large">
                                <strong>Total Pot:</strong> <span className="pot-amount">${gameState.pot || 0}</span>
                            </div>
                            <div className="info-item">
                                <strong>Current Bet:</strong> ${gameState.currentBet || 0}
                            </div>
                            <div className="info-item">
                                <strong>Current Turn:</strong> {gameState.currentPlayerTurn || "N/A"}
                            </div>
                        </div>
                    </div>

                    {/* Community Cards */}
                    <div className="debug-section">

                        <div className="cards-display">
                            {gameState.communityCards?.length > 0 ? (
                                gameState.communityCards.map((card, index) => (
                                    <span key={index}>{renderCard(card)}</span>
                                ))
                            ) : (
                                <span className="no-cards">No community cards dealt yet</span>
                            )}
                        </div>
                    </div>

                    {/* Players */}
                    <div className="debug-section">
                        <h2><FaUsers /> Players</h2>
                        <div className="players-table">
                            <div className="table-header">
                                <span>Position</span>
                                <span>Player</span>
                                <span>Chips</span>
                                <span>Current Bet</span>
                                <span>Status</span>
                                <span>Special</span>
                                <span>Connected</span>
                                <span>Turn</span>
                            </div>
                            {gameState.players?.map((player, index) => (
                                <div key={player.playerId} className={`player-row ${player.playerTurn ? 'active-turn' : ''}`}>
                                    <span className="position">#{player.seatPosition}</span>
                                    <span className="username">
                    {player.username}
                                        {user && player.username === user.username && <span className="me-indicator">(ME)</span>}
                  </span>
                                    <span className="chips">${player.chips}</span>
                                    <span className="current-bet">${player.currentBet || 0}</span>
                                    <span className={`status ${player.status?.toLowerCase()}`}>
                    {player.status || 'UNKNOWN'}
                  </span>
                                    <span className="special">
                    {player.dealer && <span className="dealer">🎯 Dealer</span>}
                                        {player.smallBlind && <span className="small-blind">🔵 SB</span>}
                                        {player.bigBlind && <span className="big-blind">🟣 BB</span>}
                  </span>
                                    <span className={`connection ${player.connected ? 'connected' : 'disconnected'}`}>
                    {player.connected ? '🟢' : '🔴'}
                  </span>
                                    <span className="turn">
                    {player.playerTurn ? '⏰ Active' : '⏸️ Waiting'}
                  </span>
                                </div>
                            ))}
                        </div>
                    </div>

                    {/* All Player Cards (Testing Only) */}
                    <div className="debug-section">
                        <h2>🃏 All Player Cards (Testing Only)</h2>
                        <div className="all-player-cards">
                            {gameState.players?.map((player) => (
                                <div key={player.playerId} className="player-cards-section">
                                    <div className="player-cards-header">
                                        <h4>
                                            {player.username}
                                            {user && player.username === user.username && <span className="me-indicator">(ME)</span>}
                                            <span className="player-position">Position #{player.seatPosition}</span>
                                        </h4>
                                        <div className="player-info-mini">
                                            <span className="chips-mini">${player.chips}</span>
                                            {player.currentBet > 0 && <span className="bet-mini">Bet: ${player.currentBet}</span>}
                                            <span className={`status-mini ${player.status?.toLowerCase()}`}>
                        {player.status}
                      </span>
                                        </div>
                                    </div>
                                    <div className="player-hole-cards">
                                        {player.holeCards && player.holeCards.length > 0 ? (
                                            player.holeCards.map((card, cardIndex) => (
                                                <span key={cardIndex}>{renderCard(card)}</span>
                                            ))
                                        ) : (
                                            <div className="no-cards-dealt">
                                                <span className="debug-card empty">--</span>
                                                <span className="debug-card empty">--</span>
                                                <span className="no-cards-text">No cards dealt</span>
                                            </div>
                                        )}
                                    </div>
                                    {player.holeCards && player.holeCards.length > 0 && (
                                        <div className="hand-strength">
                                            <small>Hand: {getHandDescription(player.holeCards) || "Unknown"}</small>
                                        </div>
                                    )}
                                </div>
                            ))}

                            {(!gameState.players || gameState.players.length === 0) && (
                                <div className="no-players">
                                    <p>No players found in the current game state.</p>
                                </div>
                            )}

                            {gameState.players && gameState.players.length > 0 &&
                                !gameState.players.some(p => p.holeCards && p.holeCards.length > 0) && (
                                    <div className="no-cards-warning">
                                        <p>⚠️ No hole cards have been dealt yet, or cards are not included in the game state.</p>
                                        <small>Cards will appear here once the round begins and cards are dealt.</small>
                                    </div>
                                )}
                        </div>
                    </div>

                    {/* Raw JSON for Advanced Debugging */}
                    <div className="debug-section">
                        <h2>🔧 Raw Game State (JSON)</h2>
                        <details className="json-details">
                            <summary>Click to expand raw JSON data</summary>
                            <pre className="json-display">
                {JSON.stringify(gameState, null, 2)}
              </pre>
                        </details>
                    </div>
                </div>
            ) : (
                !error && !isLoading && (
                    <div className="no-data">
                        <p>No game state to display. Enter a table ID and click "Fetch State" to begin.</p>
                    </div>
                )
            )}
        </div>
    );
}

export default GameDebug;