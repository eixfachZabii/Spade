import React, { useState, useEffect } from "react";
import { FaPlus, FaUsers, FaSearch,
         FaSync, FaDoorOpen, FaExclamationCircle, FaTrophy,
         FaSignOutAlt } from "react-icons/fa";
import ApiService from "../services/ApiService";
import TableCard from "../components/lobby/TableCard";

/**
 * Lobby system component for browsing and joining tables
 *
 * @param {Object} props Component props
 * @param {Object} props.user Current user data
 * @param {function} props.onJoinTable Function to handle joining a table
 * @param {Object} props.currentTable Current table data if user is at a table
 * @param {boolean} props.darkMode Dark mode state
 * @param {function} props.onBalanceUpdate Function to update user balance
 * @returns {JSX.Element} LobbySystem component
 */
function LobbySystem({ user, onJoinTable, currentTable, darkMode, onBalanceUpdate }) {
  // Tables state
  const [tables, setTables] = useState([]);
  const [filteredTables, setFilteredTables] = useState([]);

  // UI state
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [joinTableId, setJoinTableId] = useState(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [showOnlyPublic, setShowOnlyPublic] = useState(true);

  // Form state for creating table
  const [tableForm, setTableForm] = useState({
    name: "",
    description: "",
    maxPlayers: 6,
    minBuyIn: 100,
    maxBuyIn: 1000,
    isPrivate: false,
  });

  // Form state for joining table
  const [buyInAmount, setBuyInAmount] = useState("");

  // Load tables on component mount
  useEffect(() => {
    loadTables();
  }, [showOnlyPublic]);

  // Filter tables when search query changes
  useEffect(() => {
    if (searchQuery.trim() === "") {
      setFilteredTables(tables);
    } else {
      const filtered = tables.filter(table =>
        table.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        table.description.toLowerCase().includes(searchQuery.toLowerCase())
      );
      setFilteredTables(filtered);
    }
  }, [searchQuery, tables]);

  // Load tables from API
  const loadTables = async () => {
    setIsLoading(true);
    setError("");

    try {
      const tablesData = showOnlyPublic
        ? await ApiService.getPublicTables()
        : await ApiService.getAllTables();

      setTables(tablesData);
      setFilteredTables(tablesData);
    } catch (error) {
      setError("Failed to load tables: " + (error.message || "Unknown error"));
    } finally {
      setIsLoading(false);
    }
  };

  // Handle table form input changes
  const handleFormChange = (e) => {
    const { name, value, type, checked } = e.target;
    setTableForm({
      ...tableForm,
      [name]: type === "checkbox" ? checked : value,
    });
  };

  // Handle table creation
  const handleCreateTable = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setError("");

    try {
      const createdTable = await ApiService.createTable(tableForm);

      // Reset form and hide it
      setTableForm({
        name: "",
        description: "",
        maxPlayers: 6,
        minBuyIn: 100,
        maxBuyIn: 1000,
        isPrivate: false,
      });
      setShowCreateForm(false);

      // Refresh tables list
      loadTables();

      // Auto-join the created table
      if (createdTable.id) {
        onJoinTable(createdTable.id, createdTable.minBuyIn);
      }
    } catch (error) {
      setError("Failed to create table: " + (error.message || "Unknown error"));
    } finally {
      setIsLoading(false);
    }
  };

  // Handle table join
  const handleJoinTable = async (e) => {
    e.preventDefault();

    if (!joinTableId) return;

    setIsLoading(true);
    setError("");

    const buyIn = parseInt(buyInAmount, 10);

    if (isNaN(buyIn) || buyIn <= 0) {
      setError("Please enter a valid buy-in amount");
      setIsLoading(false);
      return;
    }

    try {
      // Pass the table ID and buy-in amount to the parent component
      await onJoinTable(joinTableId, buyIn);

      // Fetch updated user balance
      if (onBalanceUpdate) {
        try {
          const userData = await ApiService.getCurrentUser();
          onBalanceUpdate(userData.balance);
        } catch (balanceError) {
          console.error("Failed to update balance:", balanceError);
        }
      }

      // Reset join form
      setJoinTableId(null);
      setBuyInAmount("");
    } catch (error) {
      setError("Failed to join table: " + (error.message || "Unknown error"));
    } finally {
      setIsLoading(false);
    }
  };

  // Cancel join table
  const cancelJoinTable = () => {
    setJoinTableId(null);
    setBuyInAmount("");
  };

  // Calculate minimum and maximum buy-in for the selected table
  const selectedTable = tables.find(table => table.id === joinTableId);

  return (
    <div className="lobby-container">
      <div className="lobby-header">
        <h2>Poker Tables</h2>
        <div className="featured-banner">
          <FaTrophy className="featured-icon" />
          <span>Pro tournament starting tonight at 8PM! <a href="#">Register now</a></span>
        </div>

        <div className="lobby-controls">
          <div className="search-filter">
            <div className="search-container">
              <div className="search-icon-wrapper">
                <FaSearch />
              </div>
              <input
                type="text"
                placeholder="Search tables..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="search-input"
              />
            </div>
            <div className="filter-toggle">
              <label>
                <input
                  type="checkbox"
                  checked={showOnlyPublic}
                  onChange={() => setShowOnlyPublic(!showOnlyPublic)}
                />
                <span>Show public tables only</span>
              </label>
            </div>
          </div>

          <div className="table-actions">
            <button
              className="refresh-button"
              onClick={loadTables}
              disabled={isLoading}
              title="Refresh tables"
            >
              <FaSync className={isLoading ? "rotating" : ""} />
            </button>

            <button
              className="create-table-button"
              onClick={() => setShowCreateForm(true)}
              disabled={isLoading}
            >
              <FaPlus /> <span>Create Table</span>
            </button>
          </div>
        </div>
      </div>

      {error && (
        <div className="error-message">
          <FaExclamationCircle />
          {error}
        </div>
      )}

      {/* Current Table notification */}
      {currentTable && (
        <div className="current-table-notification">
          <div className="notification-content">
            <h3>You're currently at table: {currentTable.name}</h3>
            <div className="notification-buttons">
              <button
                className="primary-button"
                onClick={() => onJoinTable(currentTable.id)}
              >
                <FaDoorOpen /> Return to Table
              </button>
              <button
                className="leave-table-button"
                onClick={() => alert("Leave table functionality would go here")}
              >
                <FaSignOutAlt /> Leave Table
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Tables list */}
      <div className="tables-list">
        {isLoading && tables.length === 0 ? (
          <div className="loading-tables">
            <div className="loading-spinner"></div>
            <p>Loading tables...</p>
          </div>
        ) : filteredTables.length === 0 ? (
          <div className="no-tables-message">
            <p>
              {searchQuery
                ? "No tables found matching your search."
                : "No tables available. Be the first to create one!"}
            </p>
            {searchQuery && (
              <button
                className="secondary-button"
                onClick={() => setSearchQuery("")}
              >
                Clear search
              </button>
            )}
          </div>
        ) : (
          filteredTables.map((table, index) => (
            <TableCard
              key={table.id}
              table={table}
              onJoin={(tableId, buyIn) => {
                setJoinTableId(tableId);
                setBuyInAmount(buyIn.toString());
              }}
              index={index}
            />
          ))
        )}
      </div>

      {/* Table creation modal */}
      {showCreateForm && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h3>Create New Table</h3>
            <form onSubmit={handleCreateTable} className="create-table-form">
              <div className="form-group">
                <label htmlFor="table-name">Table Name</label>
                <input
                  type="text"
                  id="table-name"
                  name="name"
                  value={tableForm.name}
                  onChange={handleFormChange}
                  placeholder="Enter table name"
                  required
                  minLength="3"
                  maxLength="50"
                />
              </div>

              <div className="form-group">
                <label htmlFor="table-description">Description</label>
                <textarea
                  id="table-description"
                  name="description"
                  value={tableForm.description}
                  onChange={handleFormChange}
                  placeholder="Enter table description"
                  maxLength="255"
                  rows="3"
                />
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="max-players">Max Players</label>
                  <input
                    type="number"
                    id="max-players"
                    name="maxPlayers"
                    value={tableForm.maxPlayers}
                    onChange={handleFormChange}
                    min="2"
                    max="10"
                    required
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="min-buy-in">Min Buy-in</label>
                  <input
                    type="number"
                    id="min-buy-in"
                    name="minBuyIn"
                    value={tableForm.minBuyIn}
                    onChange={handleFormChange}
                    min="10"
                    required
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="max-buy-in">Max Buy-in</label>
                  <input
                    type="number"
                    id="max-buy-in"
                    name="maxBuyIn"
                    value={tableForm.maxBuyIn}
                    onChange={handleFormChange}
                    min={tableForm.minBuyIn}
                    required
                  />
                </div>
              </div>

              <div className="form-group checkbox-group">
                <label htmlFor="is-private">
                  <input
                    type="checkbox"
                    id="is-private"
                    name="isPrivate"
                    checked={tableForm.isPrivate}
                    onChange={handleFormChange}
                  />
                  <span>Private Table</span>
                </label>
              </div>

              <div className="form-buttons">
                <button
                  type="submit"
                  className="primary-button"
                  disabled={isLoading}
                >
                  {isLoading ? (
                    <>
                      <span className="button-spinner"></span>
                      Creating...
                    </>
                  ) : (
                    "Create Table"
                  )}
                </button>
                <button
                  type="button"
                  className="secondary-button"
                  onClick={() => setShowCreateForm(false)}
                  disabled={isLoading}
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Join table modal */}
      {joinTableId && selectedTable && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h3>Join Table</h3>
            <form onSubmit={handleJoinTable} className="join-table-form">
              <div className="selected-table-info">
                <h4>{selectedTable.name}</h4>
                <p className="table-description">{selectedTable.description}</p>
                <div className="buy-in-limits">
                  <div className="limit">
                    <span className="limit-label">Minimum:</span>
                    <span className="limit-value">{selectedTable.minBuyIn} chips</span>
                  </div>
                  <div className="limit">
                    <span className="limit-label">Maximum:</span>
                    <span className="limit-value">{selectedTable.maxBuyIn} chips</span>
                  </div>
                </div>
              </div>

              <div className="form-group">
                <label htmlFor="buy-in-amount">
                  Enter Buy-in Amount
                  <span className="balance-indicator">
                    (Your balance: {user?.balance || 0} chips)
                  </span>
                </label>
                <input
                  type="number"
                  id="buy-in-amount"
                  value={buyInAmount}
                  onChange={(e) => setBuyInAmount(e.target.value)}
                  min={selectedTable.minBuyIn}
                  max={Math.min(selectedTable.maxBuyIn, user?.balance || 0)}
                  required
                />
              </div>

              <div className="form-buttons">
                <button
                  type="submit"
                  className="primary-button"
                  disabled={isLoading || (parseInt(buyInAmount, 10) > (user?.balance || 0))}
                >
                  {isLoading ? (
                    <>
                      <span className="button-spinner"></span>
                      Joining...
                    </>
                  ) : (
                    "Take a Seat"
                  )}
                </button>
                <button
                  type="button"
                  className="secondary-button"
                  onClick={cancelJoinTable}
                  disabled={isLoading}
                >
                  Cancel
                </button>
              </div>

              {parseInt(buyInAmount, 10) > (user?.balance || 0) && (
                <div className="error-message">
                  <FaUsers /> Insufficient balance for this buy-in amount
                </div>
              )}
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

export default LobbySystem;