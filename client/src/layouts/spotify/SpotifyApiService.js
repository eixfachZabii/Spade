// client/src/services/SpotifyApiService.js
import axios from 'axios';
import EnvironmentConfig from '../../config/environment';

const API_BASE_URL = EnvironmentConfig.getApiBaseUrl();

/**
 * Service for Spotify-related API calls
 */
class SpotifyApiService {
  /**
   * Get the Spotify login URL
   * @returns {string} URL to redirect to for Spotify login
   */
  static getLoginUrl() {
    return `${API_BASE_URL}/spotify/login`;
  }

  /**
   * Refresh an access token using a refresh token
   * @param {string} refreshToken - The refresh token
   * @returns {Promise<Object>} The response with new tokens
   */
  static async refreshToken(refreshToken) {
    try {
      const response = await axios.get(`${API_BASE_URL}/spotify/refresh_token`, {
        params: { refresh_token: refreshToken }
      });
      return response.data;
    } catch (error) {
      console.error('Error refreshing token:', error);
      throw error;
    }
  }

  /**
   * Get lyrics for a song
   * @param {string} artist - The artist name
   * @param {string} title - The song title
   * @returns {Promise<Object>} The response with lyrics data
   */
  static async getLyrics(artist, title) {
    try {

      const response = await axios.get(`${API_BASE_URL}/spotify/lyrics`, {
        params: { artist, title }
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching lyrics:', error);
      throw error;
    }
  }
}

export default SpotifyApiService;