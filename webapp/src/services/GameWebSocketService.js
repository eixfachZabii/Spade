// webapp/src/services/GameWebSocketService.js

import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';
import EnvironmentConfig from '../config/environment';

class GameWebSocketService {
    constructor() {
        this.stompClient = null;
        this.connected = false;
        this.subscriptions = new Map();
        this.eventHandlers = new Map();
    }

    /**
     * Connect to the WebSocket server
     * @param {string} token - JWT authentication token
     * @returns {Promise} Connection promise
     */
    connect(token) {
        return new Promise((resolve, reject) => {
            try {
                // Use SockJS for better compatibility and fallback support
                const socket = new SockJS(
                    EnvironmentConfig.getWebSocketUrl(),
                    null,
                    {
                    // Additional SockJS options for CORS
                    transports: ['websocket', 'xhr-polling']
                });

                this.stompClient = Stomp.over(socket);

                // Enable debug for troubleshooting (disable in production)
                this.stompClient.debug = (str) => {
                    console.log('STOMP Debug:', str);
                };

                const headers = {
                    Authorization: `Bearer ${token}`,
                    // Additional headers for CORS
                    'Access-Control-Allow-Origin': 'https://localhost:3000',
                    'Access-Control-Allow-Credentials': 'true'
                };

                this.stompClient.connect(
                    headers,
                    (frame) => {
                        this.connected = true;
                        console.log('Connected to game server:', frame);
                        resolve();
                    },
                    (error) => {
                        console.error('Connection error:', error);
                        this.connected = false;
                        reject(error);
                    }
                );

                // Handle connection errors
                socket.onerror = (error) => {
                    console.error('SockJS error:', error);
                    reject(error);
                };

            } catch (error) {
                console.error('Failed to create WebSocket connection:', error);
                reject(error);
            }
        });
    }

    /**
     * Alternative connection method using native WebSocket
     * @param {string} token - JWT authentication token
     * @returns {Promise} Connection promise
     */
    connectNative(token) {
        return new Promise((resolve, reject) => {
            try {
                // Direct WebSocket connection (fallback)
                const wsUrl = EnvironmentConfig.getWebSocketUrl().replace('http://', 'ws://').replace('https://', 'wss://');
                this.stompClient = Stomp.client(wsUrl);

                this.stompClient.debug = (str) => {
                    console.log('STOMP Debug:', str);
                };

                const headers = {
                    Authorization: `Bearer ${token}`
                };

                this.stompClient.connect(
                    headers,
                    (frame) => {
                        this.connected = true;
                        console.log('Connected to game server (native):', frame);
                        resolve();
                    },
                    (error) => {
                        console.error('Native connection error:', error);
                        reject(error);
                    }
                );

            } catch (error) {
                console.error('Failed to create native WebSocket connection:', error);
                reject(error);
            }
        });
    }

    /**
     * Connect with automatic fallback
     * @param {string} token - JWT authentication token
     * @returns {Promise} Connection promise
     */
    async connectWithFallback(token) {
        try {
            // Try SockJS first
            await this.connect(token);
            console.log('Connected using SockJS');
        } catch (error) {
            console.log('SockJS failed, trying native WebSocket...');
            try {
                await this.connectNative(token);
                console.log('Connected using native WebSocket');
            } catch (nativeError) {
                console.error('Both connection methods failed:', { sockjs: error, native: nativeError });
                throw new Error('Failed to establish WebSocket connection');
            }
        }
    }

    /**
     * Join a poker table
     * @param {number} tableId - Table ID to join
     */
    joinTable(tableId) {
        if (!this.connected) {
            throw new Error('Not connected to server');
        }

        // Send connect message
        this.stompClient.send(`/app/game/${tableId}/connect`, {}, JSON.stringify({}));

        // Subscribe to table events
        const subscription = this.stompClient.subscribe(
            `/topic/tables/${tableId}`,
            (message) => {
                try {
                    const event = JSON.parse(message.body);
                    this.handleGameEvent(tableId, event);
                } catch (error) {
                    console.error('Error parsing message:', error);
                }
            }
        );

        this.subscriptions.set(`table-${tableId}`, subscription);

        // Subscribe to personal error queue
        const errorSub = this.stompClient.subscribe(
            '/user/queue/errors',
            (message) => {
                try {
                    const error = JSON.parse(message.body);
                    console.error('Game error:', error);
                    this.handleError(error);
                } catch (parseError) {
                    console.error('Error parsing error message:', parseError);
                }
            }
        );

        this.subscriptions.set('errors', errorSub);
    }

    /**
     * Send a player action
     * @param {number} tableId - Table ID
     * @param {object} action - Player action object
     */
    sendAction(tableId, action) {
        if (!this.connected) {
            throw new Error('Not connected to server');
        }

        this.stompClient.send(
            `/app/game/${tableId}/action`,
            {},
            JSON.stringify(action)
        );
    }

    /**
     * Leave a table
     * @param {number} tableId - Table ID to leave
     */
    leaveTable(tableId) {
        // Send disconnect message
        if (this.connected) {
            this.stompClient.send(`/app/game/${tableId}/disconnect`, {}, JSON.stringify({}));
        }

        // Unsubscribe from table events
        const subscription = this.subscriptions.get(`table-${tableId}`);
        if (subscription) {
            subscription.unsubscribe();
            this.subscriptions.delete(`table-${tableId}`);
        }
    }

    /**
     * Register an event handler
     * @param {string} eventType - Event type to handle
     * @param {function} handler - Handler function
     */
    on(eventType, handler) {
        if (!this.eventHandlers.has(eventType)) {
            this.eventHandlers.set(eventType, []);
        }
        this.eventHandlers.get(eventType).push(handler);
    }

    /**
     * Handle incoming game events
     * @param {number} tableId - Table ID
     * @param {object} event - Game event
     */
    handleGameEvent(tableId, event) {
        console.log('Game event:', event);

        const handlers = this.eventHandlers.get(event.type) || [];
        handlers.forEach(handler => {
            try {
                handler(event, tableId);
            } catch (error) {
                console.error('Error in event handler:', error);
            }
        });

        // Handle specific event types
        switch (event.type) {
            case 'GAME_STARTED':
                this.handleGameStarted(event.payload);
                break;
            case 'PLAYER_TURN':
                this.handlePlayerTurn(event.payload);
                break;
            case 'PLAYER_ACTION':
                this.handlePlayerAction(event.payload);
                break;
            case 'COMMUNITY_CARDS_REVEALED':
                this.handleCommunityCards(event.payload);
                break;
            case 'WINNER_DECLARED':
                this.handleWinner(event.payload);
                break;
            case 'STAGE_CHANGED':
                this.handleStageChange(event.payload);
                break;
        }
    }

    handleGameStarted(gameState) {
        console.log('Game started:', gameState);
    }

    handlePlayerTurn(playerId) {
        console.log('Player turn:', playerId);
    }

    handlePlayerAction(actionResponse) {
        console.log('Player action:', actionResponse);
    }

    handleCommunityCards(cards) {
        console.log('Community cards:', cards);
    }

    handleWinner(winner) {
        console.log('Winner:', winner);
    }

    handleStageChange(gameState) {
        console.log('Stage changed:', gameState);
    }

    handleError(error) {
        console.error('Game error:', error);
    }

    /**
     * Disconnect from server
     */
    disconnect() {
        // Unsubscribe all
        this.subscriptions.forEach(sub => sub.unsubscribe());
        this.subscriptions.clear();

        // Disconnect
        if (this.stompClient && this.connected) {
            this.stompClient.disconnect(() => {
                console.log('Disconnected from game server');
            });
        }

        this.connected = false;
    }

    /**
     * Check connection status
     */
    isConnected() {
        return this.connected;
    }

    /**
     * Reconnect if disconnected
     */
    async reconnect(token) {
        if (!this.connected) {
            try {
                await this.connectWithFallback(token);
                return true;
            } catch (error) {
                console.error('Reconnection failed:', error);
                return false;
            }
        }
        return true;
    }
}

export default GameWebSocketService;