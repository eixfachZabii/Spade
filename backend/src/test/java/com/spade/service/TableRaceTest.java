package com.spade.service;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;

import com.spade.SpadeIntegrationTest;
import com.spade.repository.PlayerEntity;
import com.spade.repository.PlayerRepository;
import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Two writers on one table, interleaved on purpose against the real database. Writer A is held part-way through its
 * transaction; writer B starts once A has done its work. With no lock on the table, B finishes while A is still open,
 * and A's commit then overwrites B. With the table row locked, B queues behind A, so A waits until B is either done
 * or visibly blocked on a lock (pg_stat_activity), then commits. Ordering is by latches; every wait has a timeout.
 */
@SpadeIntegrationTest
class TableRaceTest {

    @Autowired
    TableService tables;

    @Autowired
    PlayerRepository players;

    @Autowired
    PlatformTransactionManager transactions;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoSpyBean
    Clock clock;

    private SpadePrincipal newPlayer(String prefix) {
        String name = prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
        PlayerEntity saved = players.save(new PlayerEntity(name, name, "not-a-real-hash", clock.instant()));
        return new SpadePrincipal(saved.getId(), 0);
    }

    /** Returns once the other writer has finished, or is queued on a database lock behind us. */
    private void waitUntilOtherWriterIsDoneOrBlocked(CountDownLatch otherDone) throws InterruptedException {
        long deadline = System.nanoTime() + SECONDS.toNanos(10);
        while (!otherDone.await(20, java.util.concurrent.TimeUnit.MILLISECONDS)) {
            Long blocked = jdbc.queryForObject(
                    "select count(*) from pg_stat_activity where wait_event_type = 'Lock'", Long.class);
            if (blocked != null && blocked > 0) {
                return;
            }
            if (System.nanoTime() > deadline) {
                throw new IllegalStateException("the other writer neither finished nor blocked within 10 s");
            }
        }
    }

    private void awaitQuietly(Future<?> future) throws Exception {
        try {
            future.get(10, SECONDS);
        } catch (ExecutionException refused) {
            // a refusal (409/400) is an acceptable outcome of losing a race; the invariants below decide
        }
    }

    @Test
    void aTopUpRacingACashOutCannotBringTheSeatBack() throws Exception {
        SpadePrincipal host = newPlayer("rhost");
        SpadePrincipal cleo = newPlayer("rcleo");
        long table = tables.open(host, "Race", 5, 10, 6).id();
        tables.join(cleo, table, 1000);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch topUpHasReadTheSeat = new CountDownLatch(1);
            CountDownLatch cashOutDone = new CountDownLatch(1);
            AtomicReference<Thread> topUpThread = new AtomicReference<>();
            // TableService.topUp reads the seat, then asks the clock for the buy-in's time, then flushes: that is
            // the window the cash-out has to land in. The spy holds the top-up there.
            doAnswer(call -> {
                if (Thread.currentThread() == topUpThread.get() && topUpHasReadTheSeat.getCount() > 0) {
                    topUpHasReadTheSeat.countDown();
                    waitUntilOtherWriterIsDoneOrBlocked(cashOutDone);
                }
                return call.callRealMethod();
            }).when(clock).instant();

            Future<?> topUp = pool.submit(() -> {
                topUpThread.set(Thread.currentThread());
                tables.topUp(cleo, table, 500);
            });
            assertThat(topUpHasReadTheSeat.await(10, SECONDS)).isTrue();
            Future<?> cashOut = pool.submit(() -> {
                try {
                    tables.cashOut(cleo, table, 1000);
                } finally {
                    cashOutDone.countDown();
                }
            });
            topUp.get(10, SECONDS);
            awaitQuietly(cashOut);
        } finally {
            pool.shutdownNow();
        }

        var seat = jdbc.queryForMap("select id, stack, left_at from seat where table_id = ?", table);
        long seatId = ((Number) seat.get("id")).longValue();
        boolean open = seat.get("left_at") == null;
        Long cashOuts = jdbc.queryForObject("select count(*) from cash_out where seat_id = ?", Long.class, seatId);
        Long bought = jdbc.queryForObject("select sum(points) from buy_in where seat_id = ?", Long.class, seatId);

        // never both: an open seat that also has a counted cash-out
        assertThat(open).as("seat is open").isEqualTo(cashOuts == 0);
        if (open) {
            assertThat(((Number) seat.get("stack")).longValue()).as("stack mirrors the buy-ins").isEqualTo(bought);
            tables.cashOut(cleo, table, 1500);   // a later cash-out still works
        } else {
            assertThat(((Number) seat.get("stack")).longValue()).isZero();
        }
    }

    @Test
    void aJoinRacingTheHostsCloseCannotSeatSomeoneAtAClosedTable() throws Exception {
        SpadePrincipal host = newPlayer("chost");
        SpadePrincipal dev = newPlayer("cdev");
        long table = tables.open(host, "Race", 5, 10, 6).id();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch closeDidItsWork = new CountDownLatch(1);
            CountDownLatch joinDone = new CountDownLatch(1);
            TransactionTemplate tx = new TransactionTemplate(transactions);
            Future<?> close = pool.submit(() -> tx.executeWithoutResult(status -> {
                tables.close(host, table);
                closeDidItsWork.countDown();
                try {
                    waitUntilOtherWriterIsDoneOrBlocked(joinDone);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            }));
            assertThat(closeDidItsWork.await(10, SECONDS)).isTrue();
            Future<?> join = pool.submit(() -> {
                try {
                    tables.join(dev, table, 500);
                } finally {
                    joinDone.countDown();
                }
            });
            close.get(10, SECONDS);
            awaitQuietly(join);
        } finally {
            pool.shutdownNow();
        }

        Long seatedAtClosedTable = jdbc.queryForObject("""
                select count(*) from seat s join poker_table t on t.id = s.table_id
                where s.table_id = ? and s.left_at is null and t.closed_at is not null""", Long.class, table);
        assertThat(seatedAtClosedTable).as("open seats at a closed table").isZero();
    }
}
