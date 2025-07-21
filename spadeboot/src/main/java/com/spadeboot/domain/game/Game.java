package com.spadeboot.domain.game;

import java.util.ArrayList;
import java.util.List;

import com.spadeboot.domain.user.Player;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "games")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Round> gameRounds = new ArrayList<>();

    @ManyToMany
    private List<Player> allPlayers = new ArrayList<>();

    @ManyToMany
    private List<Player> currentPlayers = new ArrayList<>();
}
