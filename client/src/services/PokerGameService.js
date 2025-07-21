// client/src/services/PokerGameService.js

import ApiService from "./ApiService";

/**
 * Service for handling poker game data transformation and API calls
 */
class PokerGameService {

    /**
     * Card mapping from backend format to frontend format
     */
    static CARD_MAPPING = {
        // Ranks
        'TWOS': '2', 'THREES': '3', 'FOURS': '4', 'FIVES': '5', 'SIXES': '6',
        'SEVENS': '7', 'EIGHTS': '8', 'NINES': '9', 'TENS': '10', 'TEN': '10',
        'JACKS': 'jack', 'QUEENS': 'queen', 'KINGS': 'king', 'ACES': 'ace',

        // Suits
        'C': 'clubs', 'D': 'diamonds', 'H': 'hearts', 'S': 'spades'
    };

    /**
     * Parse a card string from backend format (e.g., "THREES", "SEVENC") to frontend format
     * @param {string} cardString - Card string from backend
     * @returns {Object} Card object with rank, suit, and faceUp properties
     */
    static parseCard(cardString) {
        if (!cardString || typeof cardString !== 'string') {
            return null;
        }

        // Handle different formats:
        // "THREES" -> rank only, suit unknown
        // "SEVENC" -> rank + suit (last character)
        // "TENC" -> rank + suit (TEN + C)

        let rank = null;
        let suit = null;

        // Check if last character is a suit
        const lastChar = cardString.slice(-1);
        if (['C', 'D', 'H', 'S'].includes(lastChar)) {
            // Card has suit
            const rankPart = cardString.slice(0, -1);
            suit = this.CARD_MAPPING[lastChar] || 'clubs';
            rank = this.CARD_MAPPING[rankPart] || this.parseRankFallback(rankPart);
        } else {
            // Card without suit (just rank)
            rank = this.CARD_MAPPING[cardString] || this.parseRankFallback(cardString);
            suit = 'clubs'; // Default suit if none specified
        }

        return {
            rank: rank || '2',
            suit: suit || 'clubs',
            faceUp: true // Always show face up as requested
        };
    }

    /**
     * Fallback rank parsing for unknown formats
     * @param {string} rankString - Rank string
     * @returns {string} Parsed rank
     */
    static parseRankFallback(rankString) {
        const rank = rankString.toLowerCase();
        if (rank.includes('ace') || rank.includes('a')) return 'ace';
        if (rank.includes('king') || rank.includes('k')) return 'king';
        if (rank.includes('queen') || rank.includes('q')) return 'queen';
        if (rank.includes('jack') || rank.includes('j')) return 'jack';
        if (rank.includes('ten') || rank === '10') return '10';
        if (rank.includes('nine') || rank === '9') return '9';
        if (rank.includes('eight') || rank === '8') return '8';
        if (rank.includes('seven') || rank === '7') return '7';
        if (rank.includes('six') || rank === '6') return '6';
        if (rank.includes('five') || rank === '5') return '5';
        if (rank.includes('four') || rank === '4') return '4';
        if (rank.includes('three') || rank === '3') return '3';
        if (rank.includes('two') || rank === '2') return '2';

        // Default fallback
        return '2';
    }

    /**
     * Transform raw game data from backend to frontend format
     * @param {Object} gameState - Raw game state from backend
     * @returns {Object} Transformed game data
     */
    static transformGameData(gameState) {
        if (!gameState) return null;

        try {
            // Transform players
            const transformedPlayers = (gameState.players || []).map(player => {
                // Parse hole cards
                const cards = (player.holeCards || []).map(cardString => {
                    const card = this.parseCard(cardString);
                    return card ? { ...card, idx: 0 } : null;
                }).filter(Boolean);

                // Add index for card positioning
                cards.forEach((card, index) => {
                    card.idx = index;
                });

                return {
                    name: player.username || `Player ${player.seatPosition + 1}`,
                    probWin: 0, // Will be calculated later or provided by AI
                    balance: player.chips || 0,
                    bet: player.currentBet || 0,
                    folded: player.status === 'FOLDED',
                    actionPending: player.playerTurn || false,
                    lastAction: this.getLastActionString(player),
                    cards: cards,
                    seatPosition: player.seatPosition,
                    connected: player.connected,
                    isDealer: player.dealer || false,
                    isSmallBlind: player.smallBlind || false,
                    isBigBlind: player.bigBlind || false
                };
            });

            // Transform community cards - always return 5 cards
            const transformedCommunityCards = [];
            const serverCommunityCards = gameState.communityCards || [];

            // Add the actual community cards from server
            for (let i = 0; i < 5; i++) {
                if (i < serverCommunityCards.length) {
                    const card = this.parseCard(serverCommunityCards[i]);
                    if (card) {
                        transformedCommunityCards.push(card);
                    } else {
                        // If parsing fails, add placeholder
                        transformedCommunityCards.push(null);
                    }
                } else {
                    // Add null for missing cards (will be rendered as placeholders)
                    transformedCommunityCards.push(null);
                }
            }

            // Find dealer index from dealer position
            let dealerIndex = null;
            if (gameState.dealerPosition !== null && gameState.dealerPosition !== undefined) {
                dealerIndex = transformedPlayers.findIndex(p => p.seatPosition === gameState.dealerPosition);
                if (dealerIndex === -1) dealerIndex = null;
            }

            return {
                players: transformedPlayers,
                communityCards: transformedCommunityCards,
                pot: gameState.pot || 0,
                dealerIndex: dealerIndex,
                gameActive: gameState.gameActive || false,
                currentStage: gameState.currentStage,
                currentPlayerTurn: gameState.currentPlayerTurn,
                currentBet: gameState.currentBet || 0
            };
        } catch (error) {
            console.error('Error transforming game data:', error);
            return null;
        }
    }

    /**
     * Get human-readable last action string
     * @param {Object} player - Player object
     * @returns {string} Last action string
     */
    static getLastActionString(player) {
        if (player.status === 'FOLDED') return 'fold';
        if (player.currentBet > 0) return 'bet';
        return '';
    }

    /**
     * API call to get current table information
     * @returns {Promise} Current table response
     */
    static async getCurrentTable() {
        try {
            return await ApiService.getCurrentTable();
        } catch (error) {
            console.error('Error getting current table:', error);
            throw error;
        }
    }

    /**
     * API call to get game status for a table
     * @param {number} tableId - Table ID
     * @returns {Promise} Game status response
     */
    static async getGameStatus(tableId) {
        try {
            return await ApiService.getGameStatus(tableId);
        } catch (error) {
            console.error('Error getting game status:', error);
            if (error.message.includes('401') || error.message.includes('Unauthorized')) {
                throw new Error('AUTHENTICATION_REQUIRED');
            }
            throw error;
        }
    }

    /**
     * Get demo data for development/testing
     * @returns {Object} Demo game data
     */
    static getDemoData() {
        return {
            players: [
                {
                    name: "Player 1",
                    probWin: 75,
                    balance: 1245,
                    bet: 50,
                    folded: false,
                    actionPending: true,
                    cards: [
                        { rank: "ace", suit: "hearts", faceUp: true, idx: 0 },
                        { rank: "king", suit: "hearts", faceUp: true, idx: 1 },
                    ],
                    seatPosition: 0,
                    connected: true,
                    isDealer: true
                },
                {
                    name: "Player 2",
                    probWin: 15,
                    balance: 580,
                    bet: 50,
                    folded: false,
                    lastAction: "call",
                    cards: [
                        { rank: "2", suit: "clubs", faceUp: true, idx: 0 },
                        { rank: "7", suit: "diamonds", faceUp: true, idx: 1 },
                    ],
                    seatPosition: 1,
                    connected: true
                },
                {
                    name: "Player 3",
                    probWin: 10,
                    balance: 120,
                    bet: 0,
                    folded: true,
                    cards: [
                        { rank: "ace", suit: "hearts", faceUp: true, idx: 0 },
                        { rank: "king", suit: "hearts", faceUp: true, idx: 1 },
                    ],
                    seatPosition: 2,
                    connected: false
                },
            ],
            communityCards: [
                { rank: "10", suit: "spades", faceUp: true },
                { rank: "9", suit: "clubs", faceUp: true },
                { rank: "8", suit: "hearts", faceUp: true },
                { rank: "7", suit: "diamonds", faceUp: true },
                null // Fifth card not dealt yet
            ],
            pot: 350,
            dealerIndex: 0,
            gameActive: true,
            currentStage: "TURN",
            currentBet: 50
        };
    }
}

export default PokerGameService;