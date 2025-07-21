// client/src/services/AuthService.js
import EnvironmentConfig from '../config/environment';

/**
 * Service for handling authentication API calls
 */
class AuthService {
  static API_BASE_URL = EnvironmentConfig.getApiBaseUrl();

  /**
   * Set authentication token
   * @param {string} token - JWT token
   */
  static setToken(token) {
    localStorage.setItem("spade_token", token);
  }

  /**
   * Get authentication token
   * @returns {string|null} JWT token
   */
  static getToken() {
    return localStorage.getItem("spade_token");
  }

  /**
   * Clear authentication token
   */
  static clearToken() {
    localStorage.removeItem("spade_token");
  }

  /**
   * Get authentication headers
   * @returns {Object} Headers object with Authorization
   */
  static getHeaders() {
    const headers = {
      "Content-Type": "application/json",
    };

    const token = this.getToken();
    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
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
      headers: this.getHeaders(),
    };

    try {
      const response = await fetch(url, fetchOptions);

      // If token expired or invalid, clear it
      if (response.status === 401) {
        this.clearToken();
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

  /**
   * Register a new user
   * @param {Object} userData - User registration data (username, email, password)
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
   * @param {Object} credentials - User login credentials (username, password)
   * @returns {Promise} Login response with token and user object
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
   * Check if user is authenticated
   * @returns {boolean} True if authenticated
   */
  static isAuthenticated() {
    return !!this.getToken();
  }
}

export default AuthService;