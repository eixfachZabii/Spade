package com.spade.service;

import com.spade.domain.Chips;
import com.spade.domain.DomainException;
import com.spade.repository.BuyInEntity;
import com.spade.repository.BuyInRepository;
import com.spade.repository.CashOutEntity;
import com.spade.repository.CashOutRepository;
import com.spade.repository.PlayerEntity;
import com.spade.repository.PlayerRepository;
import com.spade.repository.SeatEntity;
import com.spade.repository.SeatRepository;
import com.spade.repository.TableEntity;
import com.spade.repository.TableRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The lobby's rules (salvage map: concept only), rebuilt: host, seats, buy-ins, counted cash-outs. */
@Service
public class TableService {

    private final TableRepository tables;
    private final SeatRepository seats;
    private final BuyInRepository buyIns;
    private final CashOutRepository cashOuts;
    private final PlayerRepository players;
    private final Clock clock;

    public TableService(TableRepository tables, SeatRepository seats, BuyInRepository buyIns,
                        CashOutRepository cashOuts, PlayerRepository players, Clock clock) {
        this.tables = tables;
        this.seats = seats;
        this.buyIns = buyIns;
        this.cashOuts = cashOuts;
        this.players = players;
        this.clock = clock;
    }

    @Transactional
    public TableView open(SpadePrincipal me, String name, long smallBlind, long bigBlind, int maxSeats) {
        Chips.requireStake(smallBlind, "The small blind");
        Chips.requireStake(bigBlind, "The big blind");
        if (bigBlind < smallBlind) {
            throw invalid("The big blind can't be smaller than the small blind.");
        }
        if (maxSeats < 2 || maxSeats > 10) {
            throw invalid("A table has 2 to 10 seats.");
        }
        if (tables.existsByHostIdAndClosedAtIsNull(me.playerId())) {
            throw conflict("You're already hosting an open table. Close it first.");
        }
        return view(tables.save(new TableEntity(name.strip(), me.playerId(), smallBlind, bigBlind, maxSeats, clock.instant())));
    }

    @Transactional(readOnly = true)
    public List<TableView> openTables() {
        return tables.findByClosedAtIsNullOrderByOpenedAtAsc().stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public TableView get(long tableId) {
        return view(table(tableId));
    }

    @Transactional
    public TableView join(SpadePrincipal me, long tableId, long buyIn) {
        TableEntity table = lockedOpenTable(tableId);
        Chips.requireStake(buyIn, "A buy-in");
        seats.findByPlayerIdAndLeftAtIsNull(me.playerId()).ifPresent(seat -> {
            throw conflict(seat.getTableId() == tableId
                    ? "You're already sitting at this table."
                    : "You're sitting at another table. Cash out there first.");
        });
        List<SeatEntity> taken = seats.findByTableIdAndLeftAtIsNullOrderBySeatNumberAsc(tableId);
        if (taken.size() >= table.getMaxSeats()) {
            throw conflict("The table is full.");
        }
        Instant now = clock.instant();
        SeatEntity seat = seats.save(new SeatEntity(tableId, me.playerId(), lowestFreeSeat(taken), buyIn, now));
        buyIns.save(new BuyInEntity(seat.getId(), buyIn, now));
        return view(table);
    }

    @Transactional
    public TableView topUp(SpadePrincipal me, long tableId, long points) {
        TableEntity table = lockedOpenTable(tableId);
        Chips.requireStake(points, "A top-up");
        SeatEntity seat = mySeat(me, tableId);
        seat.addToStack(points);
        buyIns.save(new BuyInEntity(seat.getId(), points, clock.instant()));
        return view(table);
    }

    @Transactional
    public TableView cashOut(SpadePrincipal me, long tableId, long countedPoints) {
        TableEntity table = lockedOpenTable(tableId);
        Chips.requireCount(countedPoints, "A cash-out");
        SeatEntity seat = mySeat(me, tableId);
        Instant now = clock.instant();
        cashOuts.save(new CashOutEntity(seat.getId(), countedPoints, now));
        seat.leave(now);
        return view(table);
    }

    @Transactional
    public TableView close(SpadePrincipal me, long tableId) {
        TableEntity table = lockedOpenTable(tableId);
        if (table.getHostId() != me.playerId()) {
            throw new DomainException(DomainException.Kind.FORBIDDEN, "Only the host can close this table.");
        }
        int seated = seats.findByTableIdAndLeftAtIsNullOrderBySeatNumberAsc(tableId).size();
        if (seated > 0) {
            throw conflict(seated + " player(s) are still seated. Everyone cashes out first.");
        }
        table.close(clock.instant());
        return view(table);
    }

    private TableEntity table(long tableId) {
        return tables.findById(tableId).orElseThrow(TableService::noSuchTable);
    }

    /**
     * The table, locked for this transaction, and checked to be open once the lock is ours. Everything that
     * seats, tops up, cashes out or closes starts here, so those writes can't interleave on one table
     * (a top-up must not undo a cash-out, a join must not land after a close).
     */
    private TableEntity lockedOpenTable(long tableId) {
        TableEntity table = tables.findForUpdateById(tableId).orElseThrow(TableService::noSuchTable);
        if (!table.isOpen()) {
            throw conflict("This table is closed.");
        }
        return table;
    }

    private static DomainException noSuchTable() {
        return new DomainException(DomainException.Kind.NOT_FOUND, "No such table.");
    }

    private SeatEntity mySeat(SpadePrincipal me, long tableId) {
        return seats.findByTableIdAndPlayerIdAndLeftAtIsNull(tableId, me.playerId())
                .orElseThrow(() -> conflict("You're not sitting at this table."));
    }

    private static int lowestFreeSeat(List<SeatEntity> taken) {
        int number = 1;
        for (SeatEntity seat : taken) {
            if (seat.getSeatNumber() != number) {
                break;
            }
            number++;
        }
        return number;
    }

    private TableView view(TableEntity table) {
        List<SeatEntity> open = table.isOpen()
                ? seats.findByTableIdAndLeftAtIsNullOrderBySeatNumberAsc(table.getId())
                : List.of();
        Map<Long, String> names = players.findAllById(open.stream().map(SeatEntity::getPlayerId).toList()).stream()
                .collect(Collectors.toMap(PlayerEntity::getId, PlayerEntity::getDisplayName, (a, b) -> a));
        List<SeatView> seatViews = open.stream()
                .map(s -> new SeatView(s.getSeatNumber(), s.getPlayerId(), names.get(s.getPlayerId()), s.getStack()))
                .toList();
        return new TableView(table.getId(), table.getName(), table.getHostId(), table.getSmallBlind(),
                table.getBigBlind(), table.getMaxSeats(), table.isOpen(), seatViews);
    }

    private static DomainException invalid(String message) {
        return new DomainException(DomainException.Kind.INVALID, message);
    }

    private static DomainException conflict(String message) {
        return new DomainException(DomainException.Kind.CONFLICT, message);
    }
}
