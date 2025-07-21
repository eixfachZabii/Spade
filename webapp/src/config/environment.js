/**
 * Environment Configuration Service
 * Manages API URLs and other environment-specific settings
 */
class EnvironmentConfig {
  static getApiBaseUrl() {
    return process.env.REACT_APP_API_BASE_URL || '/api';
  }

  static getWebSocketBaseUrl() {
    return process.env.REACT_APP_WS_BASE_URL || '/ws';
  }

  static getEnvironment() {
    return process.env.REACT_APP_ENVIRONMENT || 'production';
  }

  static isDevelopment() {
    return this.getEnvironment() === 'development';
  }

  static isProduction() {
    return this.getEnvironment() === 'production';
  }

  static getCheatsheetApiUrl() {
    return `${this.getApiBaseUrl()}/cheatsheet`;
  }

  static getFullApiUrl(endpoint) {
    const baseUrl = this.getApiBaseUrl();
    const cleanEndpoint = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
    return `${baseUrl}${cleanEndpoint}`;
  }

  static getWebSocketUrl() {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const baseUrl = this.getWebSocketBaseUrl();
    
    // Handle different WebSocket URL formats
    if (this.isDevelopment()) {
      return window.location.hostname === 'localhost'
        ? 'http://localhost:8080/ws'
        : `${window.location.origin}${baseUrl}`;
    }
    
    return `${protocol}//${window.location.host}${baseUrl}`;
  }
}

export default EnvironmentConfig;
