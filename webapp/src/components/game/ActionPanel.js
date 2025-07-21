import React, { useState } from "react";
import { FaPlay, FaStop, FaUsers } from "react-icons/fa";

/**
 * Component for poker action controls (fold, check, call, raise)
 *
 * @param {Object} props Component props
 * @param {number} props.chips Current player chips
 * @param {boolean} props.isLoading Loading state for actions
 * @param {string} props.actionStatus Current action status message
 * @param {function} props.onAction Function to handle player actions
 * @param {boolean} props.isTableOwner Whether the current user is the table owner
 * @param {boolean} props.gameActive Whether a game is currently active
 * @param {function} props.onStartGame Function to start the game
 * @param {function} props.onEndGame Function to end the game
 * @param {number} props.playerCount Number of players at the table
 * @param {boolean} props.isMyTurn Whether it's the current player's turn
 * @param {number} props.currentBet Current bet amount to match
 * @param {number} props.playerCurrentBet Player's current contribution this round
 * @param {number} props.pot Current pot size
 * @returns {JSX.Element} ActionPanel component
 */
const ActionPanel = ({
                         chips,
                         isLoading,
                         actionStatus,
                         onAction,
                         isTableOwner,
                         gameActive,
                         onStartGame,
                         onEndGame,
                         playerCount,
                         isMyTurn,
                         currentBet = 0,
                         playerCurrentBet = 0, // NEW: Player's current contribution
                         pot = 0
                     }) => {
    const [showRaiseInput, setShowRaiseInput] = useState(false);
    const [raiseAmount, setRaiseAmount] = useState("");

    // UPDATED: Calculate the actual amount needed to call
    const toCallAmount = Math.max(0, currentBet - playerCurrentBet);
    const canCheck = toCallAmount === 0; // Can check if no additional money needed
    const needsToCall = toCallAmount > 0; // Needs to call if there's a difference

    // Button handlers for poker actions
    const handleRaiseClick = () => {
        setShowRaiseInput((prev) => !prev);
    };

    const handleConfirmRaise = () => {
        if (raiseAmount && parseInt(raiseAmount) > 0) {
            onAction("raise", parseInt(raiseAmount));
            setShowRaiseInput(false);
            setRaiseAmount("");
        }
    };

    // Determine if action buttons should be enabled
    const canAct = gameActive && isMyTurn && !isLoading;

    // UPDATED: Enhanced logging for debugging
    console.log('ActionPanel Debug:', {
        currentBet,
        playerCurrentBet,
        toCallAmount,
        canCheck,
        needsToCall,
        chips,
        canAct,
        isMyTurn
    });

    return (
        <div className={`action-panel ${isMyTurn ? 'my-turn' : ''}`}>
            <h2>
                Player Actions
                {isMyTurn && gameActive && (
                    <span className="turn-indicator">It's Your Turn!</span>
                )}
            </h2>

            {/* Game Controls for Table Owner */}
            {isTableOwner && (
                <div className="game-controls">
                    {!gameActive ? (
                        <button
                            className="game-control-button start"
                            onClick={onStartGame}
                            disabled={isLoading || playerCount < 2}
                            title={playerCount < 2 ? "Need at least 2 players to start" : "Start the game"}
                        >
                            <FaPlay /> Start Game
                        </button>
                    ) : (
                        <button
                            className="game-control-button end"
                            onClick={onEndGame}
                            disabled={isLoading}
                            title="End the current game"
                        >
                            <FaStop /> End Game
                        </button>
                    )}
                </div>
            )}

            {/* Player Count and Game Info */}
            <div className="game-info">
                <div className="info-item">
                    <FaUsers className="info-icon" />
                    <span>{playerCount} Players</span>
                </div>
                {gameActive && (
                    <>
                        <div className="info-item">
                            <span className="info-label">Pot:</span>
                            <span className="info-value pot">${pot}</span>
                        </div>
                        {/* UPDATED: Show accurate call amount */}
                        <div className="info-item">
                            <span className="info-label">To Call:</span>
                            <span className="info-value">${toCallAmount}</span>
                        </div>
                        {/* NEW: Show player's current contribution */}
                        <div className="info-item">
                            <span className="info-label">Your Bet:</span>
                            <span className="info-value">${playerCurrentBet}</span>
                        </div>
                    </>
                )}
            </div>

            <div className="player-info">
                <div className="chips-display">
                    <span className="chips-label">Your Chips:</span>
                    <span className="chips-value">${chips}</span>
                </div>
            </div>

            <div className="action-buttons">
                {/* UPDATED: Raise button logic */}
                <button
                    className="action-button raise"
                    onClick={handleRaiseClick}
                    disabled={!canAct || chips <= toCallAmount}
                    title={!gameActive ? "Game not started" :
                        !isMyTurn ? "Wait for your turn" :
                            chips <= toCallAmount ? "Insufficient chips to raise" :
                                "Raise the bet"}
                >
                    <span className="button-icon">♦</span>
                    <span>Raise</span>
                </button>

                {/* UPDATED: Call button - only show when needed */}
                <button
                    className="action-button call"
                    onClick={() => {
                        setShowRaiseInput(false);
                        onAction("call");
                    }}
                    disabled={!canAct || !needsToCall || chips < toCallAmount}
                    title={!gameActive ? "Game not started" :
                        !isMyTurn ? "Wait for your turn" :
                            !needsToCall ? "Nothing to call - you can check" :
                                chips < toCallAmount ? "Insufficient chips" :
                                    `Call $${toCallAmount}`}
                >
                    <span className="button-icon">♣</span>
                    <span>Call{needsToCall ? ` $${toCallAmount}` : ''}</span>
                </button>

                {/* UPDATED: Check button - only enabled when no call needed */}
                <button
                    className="action-button check"
                    onClick={() => {
                        setShowRaiseInput(false);
                        onAction("check");
                    }}
                    disabled={!canAct || !canCheck}
                    title={!gameActive ? "Game not started" :
                        !isMyTurn ? "Wait for your turn" :
                            !canCheck ? `Cannot check - must call $${toCallAmount} or fold` :
                                "Check"}
                >
                    <span className="button-icon">♥</span>
                    <span>Check</span>
                </button>

                {/* Fold button - always available when it's player's turn */}
                <button
                    className="action-button fold"
                    onClick={() => {
                        setShowRaiseInput(false);
                        onAction("fold");
                    }}
                    disabled={!canAct}
                    title={!gameActive ? "Game not started" :
                        !isMyTurn ? "Wait for your turn" :
                            "Fold your hand"}
                >
                    <span className="button-icon">♠</span>
                    <span>Fold</span>
                </button>
            </div>

            {/* UPDATED: Raise input with correct minimum calculations */}
            {showRaiseInput && canAct && (
                <div className="raise-input-container">
                    <input
                        type="number"
                        placeholder={`Min raise: $20`} // Assuming min raise is big blind
                        value={raiseAmount}
                        onChange={(e) => setRaiseAmount(e.target.value)}
                        min="20" // Minimum raise amount
                        max={chips - toCallAmount} // Max is remaining chips after call
                    />
                    <button
                        onClick={handleConfirmRaise}
                        disabled={!raiseAmount ||
                            parseInt(raiseAmount) <= 0 ||
                            parseInt(raiseAmount) + toCallAmount > chips}
                        title={parseInt(raiseAmount) + toCallAmount > chips ?
                            "Insufficient chips for this raise" :
                            `Raise by $${raiseAmount} (total bet: $${parseInt(raiseAmount || 0) + toCallAmount})`}
                    >
                        Confirm
                    </button>
                    <button
                        className="cancel-button"
                        onClick={() => {
                            setShowRaiseInput(false);
                            setRaiseAmount("");
                        }}
                    >
                        Cancel
                    </button>
                </div>
            )}

            {/* UPDATED: Enhanced action status with betting context */}
            {actionStatus && (
                <div className="action-status">
                    {isLoading && <div className="loading-spinner"></div>}
                    <p>{actionStatus}</p>
                </div>
            )}
        </div>
    );
};

export default ActionPanel;