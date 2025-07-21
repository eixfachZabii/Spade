import React from "react";
import { loadCardImage } from "../../utils/cardUtils";
import "./Card.css";

const Card = ({ card, playerFolded, isActive }) => {
  // Dynamic card rotation based on index and active state
  const getCardRotation = () => {
    // Base rotation alternates between cards
    const baseRotation = card.idx === 0 ? -5 : 5;

    // If player is active, cards straighten up slightly
    return isActive ? baseRotation * 0.6 : baseRotation;
  };

  // Dynamic position for overlapping cards
  const getCardPosition = () => {
    return card.idx !== 0 ? `-${card.idx * 12}px` : "0";
  };

  // Get CSS classes for the card
  const getCardClasses = () => {
    let classes = ["poker-card"];

    if (playerFolded) classes.push("folded");
    if (isActive) classes.push("active");

    return classes.join(" ");
  };

  return (
    <div
      className={getCardClasses()}
      style={{
        transform: `rotate(${getCardRotation()}deg)`,
        marginLeft: getCardPosition(),
        zIndex: 10 - card.idx, // Ensure proper stacking
      }}
    >
      <img
        src={loadCardImage(card.rank, card.suit, card.faceUp)}
        alt={card.faceUp ? `${card.rank} of ${card.suit}` : "Card back"}
        className="card-image"
      />

      {/* Card shine effect overlay */}
      <div className="card-shine"></div>

      {/* Edge highlight for active player cards */}
      {isActive && <div className="card-highlight"></div>}
    </div>
  );
};

export default Card;