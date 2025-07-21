package com.spadeboot.domain.game;

import com.spadeboot.domain.card.Card;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "stages")
public class Stage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "round_id")
    private Round round;

    @OneToMany(mappedBy = "stage", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Turn> turns = new ArrayList<>();

    private int playerCount;

    @Enumerated(EnumType.STRING)
    private StageType type;

    @OneToMany(cascade = CascadeType.ALL)
    private List<Card> newCards = new ArrayList<>();
}
