package com.spadeboot.domain.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.spadeboot.domain.card.Card;
import com.spadeboot.domain.user.Player;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.util.Pair;

@Getter
@Setter
@Entity
@Table(name = "rounds")
public class Round {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(cascade = CascadeType.ALL)
    private Stage preFlop;

    @OneToOne(cascade = CascadeType.ALL)
    private Stage flop;

    @OneToOne(cascade = CascadeType.ALL)
    private Stage turn;

    @OneToOne(cascade = CascadeType.ALL)
    private Stage river;

    @ManyToOne
    @JoinColumn(name = "game_id")
    private Game game;

    @ManyToMany
    private List<Player> winner = new ArrayList<>();

    @ManyToMany
    private List<Player> players = new ArrayList<>();

    @Transient
    private Map<Long, Pair<Card, Card>> playerHands;

    @OneToMany(cascade = CascadeType.ALL)
    private List<Card> communityCards = new ArrayList<>();

    private int playerCount;
}
