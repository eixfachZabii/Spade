import React from "react";
import Card from "../card/Card";
import ChipStack from "../chipStack/ChipStack";
import "./Player.css";

const Player = ({ player, position, isDealer, isFullscreen }) => {
  // Calculate scale based on position and screen size - reduced by 10%
  const getScale = () => {
    // Reduced base scale when not in fullscreen
    const baseScale = isFullscreen ? 1.0 : 0.9; // Changed from 1 to 0.9
    return baseScale * (window.innerWidth > 1600 ? 1.1 :
                       window.innerWidth > 1200 ? 0.95 :
                       window.innerWidth > 768 ? 0.8 : 0.65);
  };

  const scale = getScale();

  // Animate if player is active or just performed an action
  const isActive = player.actionPending;
  const hasFolded = player.folded;

  // Status indicators
  const getStatusIndicator = () => {
    if (hasFolded) return "FOLDED";
    if (isActive) return "ACTIVE";
    if (player.lastAction) return player.lastAction.toUpperCase();
    return null;
  };

  const statusIndicator = getStatusIndicator();

  return (
    <div
      className={`player-container ${isActive ? 'active' : ''} ${hasFolded ? 'folded' : ''}`}
      style={{
        left: `${position.x}px`,
        top: `${position.y}px`,
        transform: `translate(-50%, -50%) scale(${scale})`,
      }}
    >
      {/* Dealer Button */}
      {isDealer && (
        <div className="dealer-button">
          <span>D</span>
        </div>
      )}

      {/* Status Indicator */}
      {statusIndicator && (
        <div className={`status-indicator ${statusIndicator.toLowerCase()}`}>
          {statusIndicator}
        </div>
      )}

      {/* Player Cards */}
      <div className="player-cards">
        {player.cards.map((card, idx) => (
          <Card
            key={idx}
            card={{ ...card, idx }}
            playerFolded={hasFolded}
            isActive={isActive}
          />
        ))}
      </div>

      {/* Player Information */}
      <div className="player-info">
        <div className="player-name-container">
          <h3 className="player-name">{player.name}</h3>
          <span className="win-probability">{player.probWin}%</span>
        </div>

        <div className="player-stats">
          <div className="bet-amount">
            <span className="stat-label">BET</span>
            <span className="stat-value">${player.bet}</span>
          </div>

          <div className="balance-amount">
            <span className="stat-label">BALANCE</span>
            <span className={`stat-value ${player.balance < 0 ? 'negative' : ''}`}>
              ${Math.abs(player.balance)}
              {player.balance < 0 && <span className="negative-indicator">-</span>}
            </span>
          </div>
        </div>
      </div>

      {/* Bet Visualization (chips) */}
      {player.bet > 0 && (
        <div className="player-bet-visualization">
          <ChipStack amount={player.bet} compact={true} />
        </div>
      )}
    </div>
  );
};

export default Player;