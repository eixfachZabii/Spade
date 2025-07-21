import React from "react";
import { FaUsers, FaCoins, FaLock, FaLockOpen, FaStar } from "react-icons/fa";

/**
 * Component for displaying a poker table in the lobby
 *
 * @param {Object} props Component props
 * @param {Object} props.table Table data object
 * @param {function} props.onJoin Function to handle joining the table
 * @param {number} props.index Table index for styling
 * @returns {JSX.Element} TableCard component
 */
const TableCard = ({ table, onJoin, index }) => {
  // Assign a random theme class for visual variety
  const themes = ['theme-spades', 'theme-hearts', 'theme-diamonds', 'theme-clubs'];
  const randomTheme = themes[index % themes.length];

  // Check if it's a VIP table based on buy-in
  const isVIP = table.minBuyIn > 500;

  // If the description is empty, use a placeholder
  const descriptions = [
    "A friendly table for casual players looking to enjoy the game without pressure.",
    "High-stakes action with experienced players. Not for the faint of heart!",
    "Fast-paced games with 3-minute timers. Perfect for quick rounds.",
    "Tournament practice table with professional-style play.",
    "Beginners welcome! Learn the ropes in a supportive environment.",
    "Weekly regulars table - all skill levels welcome."
  ];

  // Use table description or a random one if empty
  const tableDescription = table.description || descriptions[index % descriptions.length];

  return (
    <div className={`table-card ${randomTheme}`}>
      <div className="table-header">
        <h3>
          {table.name}
          {isVIP && <FaStar className="vip-icon" title="VIP Table" />}
        </h3>
        {table.isPrivate ? (
          <FaLock className="table-status-icon private" title="Private table" />
        ) : (
          <FaLockOpen className="table-status-icon public" title="Public table" />
        )}
      </div>

      <p className={`table-description ${isVIP ? 'vip' : ''}`}>
        {tableDescription}
      </p>

      <div className="table-details">
        <div className="table-detail">
          <FaUsers className="detail-icon" />
          <span>
            {table.currentPlayers}/{table.maxPlayers} players
          </span>
        </div>

        <div className="table-detail">
          <FaCoins className="detail-icon" />
          <span>
            Buy-in: {table.minBuyIn} - {table.maxBuyIn}
          </span>
        </div>
      </div>

      <div className="table-actions">
        <button
          className="join-button"
          onClick={() => onJoin(table.id, table.minBuyIn)}
          disabled={table.currentPlayers >= table.maxPlayers}
        >
          {table.currentPlayers >= table.maxPlayers
            ? "Table Full"
            : "Take a Seat"}
        </button>
      </div>
    </div>
  );
};

export default TableCard;