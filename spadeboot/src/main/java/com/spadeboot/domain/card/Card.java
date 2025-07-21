// src/main/java/com/pokerapp/domain/card/Card.java
package com.spadeboot.domain.card;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Entity
@Table(name = "cards")
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Suit suit;

    @Enumerated(EnumType.STRING)
    private Value value;

    private boolean isShowing;

    @Override
    public String toString() {
        return value + " of " + suit;
    }
}
