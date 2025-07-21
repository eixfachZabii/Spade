// src/main/java/com/pokerapp/domain/game/Table.java
package com.spadeboot.domain.game;

import com.spadeboot.domain.user.Player;
import com.spadeboot.domain.user.PlayerStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "poker_tables")
public class PokerTable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    private Integer maxPlayers;

    private Integer minBuyIn;

    private Integer maxBuyIn;

    private Boolean isPrivate = false;

    @ManyToOne
    private Player owner;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "poker_tables_players",
            joinColumns = @JoinColumn(name = "poker_table_id"),
            inverseJoinColumns = @JoinColumn(name = "players_id")
    )
    private Set<Player> players = new HashSet<>();

    @OneToOne(cascade = CascadeType.ALL)
    private Game game;

    public boolean addPlayer(Player player, Integer buyIn) {

        if(players.stream().anyMatch(p -> p.getUserId().equals(player.getUserId()))) {
            return true;
        }

        if (players.size() >= maxPlayers) {
            return false;
        }

        if (buyIn < minBuyIn || buyIn > maxBuyIn) {
            return false;
        }

        if (buyIn > player.getUser().getBalance()) {
            return false;
        }

        player.setChips(buyIn);
        player.getUser().setBalance(player.getUser().getBalance() - buyIn);
        player.setCurrentTableId(this.id);
        player.setStatus(PlayerStatus.ACTIVE);
        players.add(player);

        return true;
    }

    public boolean removePlayer(Player player) {
        if (!players.contains(player)) {
            return false;
        }

        Integer remainingChips = player.getChips();
        player.leaveTable(remainingChips);
        return players.remove(player);
    }
}
