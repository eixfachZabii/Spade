// client/src/services/ApiService.js
import EnvironmentConfig from '../config/environment';

/**
 * Service for handling all API calls to the backend
 * Fixed to match webapp authentication pattern
 */
class ApiService {
    static API_BASE_URL = EnvironmentConfig.getApiBaseUrl();

    // Store the JWT token - using same key as webapp for consistency
    static token = localStorage.getItem("token") || null;

    /**
     * Set authentication token
     * @param {string} token - JWT token
     */
    static setToken(token) {
        this.token = token;
        localStorage.setItem("token", token);
    }

    /**
     * Clear authentication token
     */
    static clearToken() {
        this.token = null;
        localStorage.removeItem("token");
    }

    /**
     * Get authentication headers
     * @returns {Object} Headers object with Authorization
     */
    static getHeaders() {
        const headers = {
            "Content-Type": "application/json",
        };

        if (this.token) {
            headers["Authorization"] = `Bearer ${this.token}`;
        }

        return headers;
    }

    /**
     * Make API call
     * @param {string} endpoint - API endpoint
     * @param {Object} options - Fetch options
     * @returns {Promise} API response
     */
    static async apiCall(endpoint, options = {}) {
        const url = `${this.API_BASE_URL}${endpoint}`;

        const fetchOptions = {
            ...options,
            headers: {
                ...this.getHeaders(),
                ...options.headers, // Allow overriding headers
            },
        };

        try {
            const response = await fetch(url, fetchOptions);

            // If token expired or invalid, clear it
            if (response.status === 401) {
                this.clearToken();
                // Redirect to login page
                window.location.href = '/authentication/sign-in';
                throw new Error('Authentication required');
            }

            const data = await response.json();

            if (!response.ok) {
                throw new Error(data.message || "An error occurred");
            }

            return data;
        } catch (error) {
            console.error("API error:", error);
            throw error;
        }
    }

    // ============ User/Auth API ============

    /**
     * Register a new user
     * @param {Object} userData - User registration data
     * @returns {Promise} Registration response
     */
    static async register(userData) {
        return this.apiCall("/users/register", {
            method: "POST",
            body: JSON.stringify(userData),
        });
    }

    /**
     * Login a user
     * @param {Object} credentials - User login credentials
     * @returns {Promise} Login response with token
     */
    static async login(credentials) {
        const data = await this.apiCall("/users/login", {
            method: "POST",
            body: JSON.stringify(credentials),
        });

        if (data.token) {
            this.setToken(data.token);
        }

        return data;
    }

    /**
     * Get current user profile
     * @returns {Promise} User profile data
     */
    static async getCurrentUser() {
        return this.apiCall("/users/me");
    }

    /**
     * Update current user profile
     * @param {Object} userData - Updated user data
     * @returns {Promise} Updated user profile
     */
    static async updateUser(userData) {
        return this.apiCall("/users/me", {
            method: "PUT",
            body: JSON.stringify(userData),
        });
    }

    /**
     * Update user password
     * @param {Object} passwordData - Password update data
     * @returns {Promise} Update response
     */
    static async updatePassword(passwordData) {
        return this.apiCall("/users/me/password", {
            method: "PUT",
            body: JSON.stringify(passwordData),
        });
    }

    /**
     * Upload user avatar
     * @param {FormData} formData - Form data containing avatar file
     * @returns {Promise} Updated user profile
     */
    static async uploadAvatar(formData) {
        const url = `${this.API_BASE_URL}/users/me/avatar`;

        try {
            const response = await fetch(url, {
                method: "PUT",
                headers: {
                    "Authorization": `Bearer ${this.token}`,
                    // Note: Do NOT set Content-Type here for multipart/form-data
                },
                body: formData,
            });

            if (response.status === 401) {
                this.clearToken();
                window.location.href = '/authentication/sign-in';
                throw new Error('Authentication required');
            }

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || "Failed to upload avatar");
            }

            const userData = await response.json();
            return userData;
        } catch (error) {
            console.error("Avatar upload error:", error);
            throw error;
        }
    }

    /**
     * Check if user is authenticated
     * @returns {boolean} True if authenticated
     */
    static isAuthenticated() {
        return !!this.token;
    }

    // ============ Player API ============

    /**
     * Get current player info
     * @returns {Promise} Current player data
     */
    static async getCurrentPlayer() {
        return this.apiCall("/players/me");
    }

    /**
     * Check if the current user is at a table
     * @returns {Promise} Table status information
     */
    static async getCurrentTable() {
        return this.apiCall("/players/current-table");
    }

    /**
     * Get the current players chips
     * @returns {Promise} Current players chips
     */
    static async getCurrentChips() {
        const player = await this.apiCall("/players/me");
        return player.chips;
    }

    // ============ Table API ============

    /**
     * Get all tables
     * @returns {Promise} List of all tables
     */
    static async getAllTables() {
        return this.apiCall("/tables");
    }

    /**
     * Get public tables
     * @returns {Promise} List of public tables
     */
    static async getPublicTables() {
        return this.apiCall("/tables/public");
    }

    /**
     * Get table by ID
     * @param {number} tableId - Table ID
     * @returns {Promise} Table details
     */
    static async getTableById(tableId) {
        return this.apiCall(`/tables/${tableId}`);
    }

    /**
     * Create a new table
     * @param {Object} tableData - Table creation data
     * @returns {Promise} Created table
     */
    static async createTable(tableData) {
        return this.apiCall("/tables", {
            method: "POST",
            body: JSON.stringify(tableData),
        });
    }

    /**
     * Join a table
     * @param {number} tableId - Table ID
     * @param {number} buyIn - Buy-in amount
     * @returns {Promise} Updated table
     */
    static async joinTable(tableId, buyIn) {
        return this.apiCall(`/tables/${tableId}/join?buyIn=${buyIn}`, {
            method: "POST",
        });
    }

    /**
     * Leave a table
     * @param {number} tableId - Table ID
     * @returns {Promise} Updated table
     */
    static async leaveTable(tableId) {
        return this.apiCall(`/tables/${tableId}/leave`, {
            method: "POST",
        });
    }

    /**
     * Delete a table (owner only)
     * @param {number} tableId - Table ID to delete
     * @returns {Promise} Deleted table data
     */
    static async deleteTable(tableId) {
        return this.apiCall(`/tables/${tableId}`, {
            method: "DELETE",
        });
    }

    // ============ Game API ============

    /**
     * Get game status for a table
     * @param {number} tableId - Table ID
     * @returns {Promise} Game status response
     */
    static async getGameStatus(tableId) {
        return this.apiCall(`/games/tables/${tableId}/status`);
    }

    /**
     * Start a poker game
     * @param {number} tableId - Table ID
     * @param {number} bigBlind - Big blind amount (default: 20)
     * @returns {Promise} Game state response
     */
    static async startGame(tableId, bigBlind = 20) {
        return this.apiCall(`/games/tables/${tableId}/start?bigBlind=${bigBlind}`, {
            method: "POST",
        });
    }

    /**
     * End a poker game
     * @param {number} tableId - Table ID
     * @returns {Promise} End game response
     */
    static async endGame(tableId) {
        return this.apiCall(`/games/tables/${tableId}/end`, {
            method: "POST",
        });
    }

    /**
     * Get player's cards (if stored on backend)
     * @param {number} tableId - Table ID
     * @returns {Promise} Player's cards
     */
    static async getMyCards(tableId) {
        return this.apiCall(`/games/tables/${tableId}/my-cards`);
    }

    /**
     * Submit scanned cards (integration with AI)
     * @param {number} tableId - Table ID
     * @param {object} scanData - Scanned card data
     * @returns {Promise} Validation response
     */
    static async submitScannedCards(tableId, scanData) {
        return this.apiCall(`/games/tables/${tableId}/cards/scan`, {
            method: "POST",
            body: JSON.stringify(scanData),
        });
    }

    /**
     * Get game history for a table
     * @param {number} tableId - Table ID
     * @returns {Promise} Game history
     */
    static async getGameHistory(tableId) {
        return this.apiCall(`/games/tables/${tableId}/history`);
    }

    /**
     * Request AI hint for current situation
     * @param {number} tableId - Table ID
     * @returns {Promise} AI suggestion
     */
    static async getAIHint(tableId) {
        return this.apiCall(`/games/tables/${tableId}/ai-hint`);
    }

    // ============ Statistics API ============

    /**
     * Get player statistics
     * @param {number} playerId - Player ID (optional, defaults to current user)
     * @returns {Promise} Player statistics
     */
    static async getPlayerStats(playerId) {
        const endpoint = playerId
            ? `/statistics/players/${playerId}`
            : `/statistics/me`;
        return this.apiCall(endpoint);
    }

    /**
     * Get table statistics
     * @param {number} tableId - Table ID
     * @returns {Promise} Table statistics
     */
    static async getTableStats(tableId) {
        return this.apiCall(`/statistics/tables/${tableId}`);
    }
}

export default ApiService;