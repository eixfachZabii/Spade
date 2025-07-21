// src/test/java/com/pokerapp/service/GameServiceTest.java
package com.spadeboot;

import com.spadeboot.api.dto.PlayerActionResponse;
import com.spadeboot.domain.game.PokerTable;
import com.spadeboot.domain.user.Player;
import com.spadeboot.exception.InvalidMoveException;
import com.spadeboot.repository.PlayerRepository;
import com.spadeboot.repository.TableRepository;
import com.spadeboot.service.GameService;
import com.spadeboot.session.GameSession;
import com.spadeboot.session.SessionManager;
import com.spadeboot.websocket.GameEventPublisher;
import com.spadeboot.api.dto.GameStateDto;
import com.spadeboot.api.dto.PlayerActionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private TableRepository tableRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private SessionManager sessionManager;

    @Mock
    private GameEventPublisher eventPublisher;

    @InjectMocks
    private GameService gameService;

    private PokerTable testTable;
    private Player testOwner;
    private Player testPlayer1;
    private Player testPlayer2;

    @BeforeEach
    void setUp() {
        // Setup test data
        testOwner = new Player();
        testOwner.setId(1L);
        testOwner.setChips(1000);

        testPlayer1 = new Player();
        testPlayer1.setId(2L);
        testPlayer1.setChips(1000);

        testPlayer2 = new Player();
        testPlayer2.setId(3L);
        testPlayer2.setChips(1000);

        testTable = new PokerTable();
        testTable.setId(1L);
        testTable.setOwner(testOwner);
        testTable.setPlayers(new HashSet<>(Arrays.asList(testOwner, testPlayer1, testPlayer2)));
    }

    @Test
    void testStartGame_Success() {
        // Given
        Long tableId = 1L;
        Long userId = 1L;
        int bigBlind = 20;

        when(tableRepository.findById(tableId)).thenReturn(Optional.of(testTable));
        when(sessionManager.hasActiveGame(tableId)).thenReturn(false);

        GameSession mockSession = mock(GameSession.class);
        GameStateDto mockGameState = new GameStateDto();
        when(mockSession.getCurrentGameState()).thenReturn(mockGameState);
        when(sessionManager.createGameSession(eq(tableId), anyList(), eq(bigBlind))).thenReturn(mockSession);

        // When
        GameStateDto result = gameService.startGame(tableId, userId, bigBlind);

        // Then
        assertNotNull(result);
        verify(mockSession).start();
        verify(eventPublisher).publishGameStarted(tableId, mockGameState);
    }

    @Test
    void testStartGame_NotOwner_ThrowsException() {
        // Given
        Long tableId = 1L;
        Long userId = 2L; // Not the owner
        int bigBlind = 20;

        when(tableRepository.findById(tableId)).thenReturn(Optional.of(testTable));

        // When/Then
        assertThrows(InvalidMoveException.class, () -> {
            gameService.startGame(tableId, userId, bigBlind);
        });
    }

    @Test
    void testStartGame_GameAlreadyActive_ThrowsException() {
        // Given
        Long tableId = 1L;
        Long userId = 1L;
        int bigBlind = 20;

        when(tableRepository.findById(tableId)).thenReturn(Optional.of(testTable));
        when(sessionManager.hasActiveGame(tableId)).thenReturn(true);

        // When/Then
        assertThrows(InvalidMoveException.class, () -> {
            gameService.startGame(tableId, userId, bigBlind);
        });
    }

    @Test
    void testStartGame_NotEnoughPlayers_ThrowsException() {
        // Given
        Long tableId = 1L;
        Long userId = 1L;
        int bigBlind = 20;

        testTable.setPlayers(new HashSet<>(Arrays.asList(testOwner))); // Only 1 player
        when(tableRepository.findById(tableId)).thenReturn(Optional.of(testTable));

        // When/Then
        assertThrows(InvalidMoveException.class, () -> {
            gameService.startGame(tableId, userId, bigBlind);
        });
    }

    @Test
    void testProcessPlayerAction_Success() {
        // Given
        Long tableId = 1L;
        Long userId = 2L;
        PlayerActionDto action = new PlayerActionDto();
        action.setAction(PlayerActionDto.ActionType.CALL);

        GameSession mockSession = mock(GameSession.class);
        when(sessionManager.getGameSession(tableId)).thenReturn(mockSession);
        when(playerRepository.findByUserId(userId)).thenReturn(Optional.of(testPlayer1));
        when(mockSession.isPlayerTurn(testPlayer1.getId())).thenReturn(true);

        PlayerActionResponse mockResponse = new PlayerActionResponse();
        mockResponse.setSuccess(true);
        mockResponse.setStateChanged(false);
        when(mockSession.processAction(testPlayer1, action)).thenReturn(mockResponse);

        // When
        PlayerActionResponse result = gameService.processPlayerAction(tableId, userId, action);

        // Then
        assertTrue(result.isSuccess());
        verify(eventPublisher).publishPlayerAction(tableId, testPlayer1.getId(), action, mockResponse);
    }

    @Test
    void testProcessPlayerAction_NotPlayerTurn_ThrowsException() {
        // Given
        Long tableId = 1L;
        Long userId = 2L;
        PlayerActionDto action = new PlayerActionDto();

        GameSession mockSession = mock(GameSession.class);
        when(sessionManager.getGameSession(tableId)).thenReturn(mockSession);
        when(playerRepository.findByUserId(userId)).thenReturn(Optional.of(testPlayer1));
        when(mockSession.isPlayerTurn(testPlayer1.getId())).thenReturn(false);

        // When/Then
        assertThrows(InvalidMoveException.class, () -> {
            gameService.processPlayerAction(tableId, userId, action);
        });
    }

    @Test
    void testEndGame_Success() {
        // Given
        Long tableId = 1L;
        Long userId = 1L;

        GameSession mockSession = mock(GameSession.class);
        when(tableRepository.findById(tableId)).thenReturn(Optional.of(testTable));
        when(sessionManager.getGameSession(tableId)).thenReturn(mockSession);

        // When
        gameService.endGame(tableId, userId);

        // Then
        verify(mockSession).endGame();
        verify(sessionManager).removeGameSession(tableId);
        verify(eventPublisher).publishGameEnded(tableId);
    }

    @Test
    void testHandlePlayerDisconnect() {
        // Given
        Long tableId = 1L;
        Long userId = 2L;

        GameSession mockSession = mock(GameSession.class);
        when(sessionManager.getGameSession(tableId)).thenReturn(mockSession);
        when(playerRepository.findByUserId(userId)).thenReturn(Optional.of(testPlayer1));

        // When
        gameService.handlePlayerDisconnect(tableId, userId);

        // Then
        verify(mockSession).markPlayerDisconnected(testPlayer1.getId());
        verify(eventPublisher).publishPlayerDisconnected(tableId, testPlayer1.getId());
    }

    @Test
    void testHandlePlayerReconnect() {
        // Given
        Long tableId = 1L;
        Long userId = 2L;

        GameSession mockSession = mock(GameSession.class);
        GameStateDto mockGameState = new GameStateDto();
        when(sessionManager.getGameSession(tableId)).thenReturn(mockSession);
        when(playerRepository.findByUserId(userId)).thenReturn(Optional.of(testPlayer1));
        when(mockSession.getCurrentGameState()).thenReturn(mockGameState);

        // When
        GameStateDto result = gameService.handlePlayerReconnect(tableId, userId);

        // Then
        assertNotNull(result);
        verify(mockSession).markPlayerReconnected(testPlayer1.getId());
        verify(eventPublisher).publishPlayerReconnected(tableId, testPlayer1.getId());
    }
}