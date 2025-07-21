package com.spadeboot.service;

import com.spadeboot.api.dto.response.TableDto;
import com.spadeboot.api.dto.request.TableSettingsDto;
import com.spadeboot.domain.game.PokerTable;
import com.spadeboot.domain.user.Player;
import com.spadeboot.domain.user.User;
import com.spadeboot.exception.NotFoundException;
import com.spadeboot.repository.PlayerRepository;
import com.spadeboot.repository.TableRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TableService {

    private final TableRepository tableRepository;
    private final PlayerRepository playerRepository;

    @Autowired
    public TableService(
            TableRepository tableRepository,
            PlayerRepository playerRepository) {
        this.tableRepository = tableRepository;
        this.playerRepository = playerRepository;
    }

    @Transactional
    public TableDto createTable(TableSettingsDto settings, User owner) {
        // Find the player associated with this user
        Player ownerAsPlayer = playerRepository.findByUserId(owner.getId())
                .orElseThrow(() -> new NotFoundException("Player not found for user: " + owner.getUsername()));

        PokerTable pokerTable = new PokerTable();
        pokerTable.setName(settings.getName());
        pokerTable.setDescription(settings.getDescription());
        pokerTable.setMaxPlayers(settings.getMaxPlayers());
        pokerTable.setMinBuyIn(settings.getMinBuyIn());
        pokerTable.setMaxBuyIn(settings.getMaxBuyIn());
        pokerTable.setIsPrivate(settings.getIsPrivate());
        pokerTable.setOwner(ownerAsPlayer);

        PokerTable savedTable = tableRepository.save(pokerTable);
        return convertToDto(savedTable);
    }

    @Transactional
    public TableDto joinTable(Long tableId, Long userId, Integer buyIn) {
        PokerTable pokerTable = getTableEntityById(tableId);

        // Find player by user ID
        Player player = playerRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Player not found for user ID: " + userId));

        if (player.getCurrentTableId() != null) {
            throw new IllegalStateException("Player is already at a table");
        }

        boolean joined = pokerTable.addPlayer(player, buyIn);
        if (!joined) {
            throw new IllegalStateException("Could not join table - Check buy-in amount and table capacity");
        }

        playerRepository.save(player);
        PokerTable updatedTable = tableRepository.save(pokerTable);

        return convertToDto(updatedTable);
    }



    @Transactional
    public TableDto leaveTable(Long tableId, Long userId) {
        PokerTable pokerTable = getTableEntityById(tableId);

        // Find player by user ID
        Player player = playerRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Player not found for user ID: " + userId));

        boolean removed = pokerTable.removePlayer(player);
        if (!removed) {
            throw new IllegalStateException("Player not at this table");
        }

        playerRepository.save(player);
        PokerTable updatedTable = tableRepository.save(pokerTable);

        return convertToDto(updatedTable);
    }

    @Transactional
    public TableDto deleteTable(Long tableId, Long userId) {
        PokerTable pokerTable = getTableEntityById(tableId);

        // Check if the user is the owner of the table
        if (!pokerTable.getOwner().getUserId().equals(userId)) {
            throw new IllegalStateException("Only the owner can delete this table");
        }

        // Check if there's an active game
        if (pokerTable.getGame() != null) {
            throw new IllegalStateException("Cannot delete table with an active game");
        }

        // Remove all players from the table before deletion
        for (Player player : new HashSet<>(pokerTable.getPlayers())) {
            pokerTable.removePlayer(player);
            playerRepository.save(player);
        }

        // Create a DTO copy before deletion for return value
        TableDto tableDto = convertToDto(pokerTable);

        // Delete the table
        tableRepository.delete(pokerTable);

        return tableDto;
    }


    public TableDto getTableById(Long id) {
        PokerTable pokerTable = tableRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Table not found with ID: " + id));
        return convertToDto(pokerTable);
    }


    public PokerTable getTableEntityById(Long id) {
        return tableRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Table not found with ID: " + id));
    }


    public List<TableDto> getAllTables() {
        return tableRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }


    public List<TableDto> getPublicTables() {
        return tableRepository.findAll().stream()
                .filter(table -> !table.getIsPrivate())
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private TableDto convertToDto(PokerTable pokerTable) {
        TableDto dto = new TableDto();
        dto.setId(pokerTable.getId());
        dto.setName(pokerTable.getName());
        dto.setDescription(pokerTable.getDescription());
        dto.setMaxPlayers(pokerTable.getMaxPlayers());
        dto.setCurrentPlayers(pokerTable.getPlayers().size());
        dto.setMinBuyIn(pokerTable.getMinBuyIn());
        dto.setMaxBuyIn(pokerTable.getMaxBuyIn());
        dto.setIsPrivate(pokerTable.getIsPrivate());
        dto.setOwnerId(pokerTable.getOwner().getUserId());
        dto.setHasActiveGame(pokerTable.getGame() != null);
        return dto;
    }
}