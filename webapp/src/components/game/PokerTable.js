import React from "react";

/**
 * Poker table container component
 *
 * @param {Object} props Component props
 * @param {Object} props.currentTable Current table data
 * @param {React.ReactNode} props.children Child components
 * @returns {JSX.Element} PokerTable component
 */
const PokerTable = ({ currentTable, children }) => {
  return (
    <div className="poker-table-container">
      {children}
    </div>
  );
};

export default PokerTable;