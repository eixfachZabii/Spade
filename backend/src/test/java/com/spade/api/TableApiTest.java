package com.spade.api;

import static com.spade.api.TestPlayers.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.spade.SpadeIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpadeIntegrationTest
class TableApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    TestPlayers players;

    @BeforeEach
    void setUp() {
        players = new TestPlayers(mvc);
    }

    private ResultActions postJson(String path, String token, String json) throws Exception {
        return mvc.perform(post(path).header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long openTable(String hostToken, int maxSeats) throws Exception {
        String body = postJson("/api/v1/tables", hostToken, """
                {"name":"Friday","smallBlind":5,"bigBlind":10,"maxSeats":%d}""".formatted(maxSeats))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private ResultActions join(long table, String token, long buyIn) throws Exception {
        return join(table, token, (Object) buyIn);
    }

    private ResultActions join(long table, String token, Object buyIn) throws Exception {
        return postJson("/api/v1/tables/" + table + "/seats", token, "{\"buyIn\":" + buyIn + "}");
    }

    @Test
    void aHostOpensATableAndPlayersSitDownWithBuyIns() throws Exception {
        String host = players.newPlayerToken("host");
        String ana = players.newPlayerToken("ana");
        long table = openTable(host, 6);

        join(table, ana, 1000)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seats[0].seatNumber").value(1))
                .andExpect(jsonPath("$.seats[0].stack").value(1000));
        join(table, host, 1000).andExpect(jsonPath("$.seats[1].seatNumber").value(2));
        mvc.perform(get("/api/v1/tables/" + table).header("Authorization", bearer(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.open").value(true))
                .andExpect(jsonPath("$.seats.length()").value(2));
    }

    @Test
    void amountsMustBeCountableInChips() throws Exception {
        String host = players.newPlayerToken("chips");
        postJson("/api/v1/tables", host, """
                {"name":"Odd","smallBlind":7,"bigBlind":10,"maxSeats":6}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("steps of 5")));
        long table = openTable(host, 6);
        for (long refused : new long[] {0, -100, 1003}) {
            join(table, host, refused)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value(containsString("5 points")))
                    .andExpect(jsonPath("$.detail").value(containsString("smallest chip")));
        }
        join(table, host, 2_000_000)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("typo")));
    }

    @Test
    void onePlayerSitsAtOneTableAtATime() throws Exception {
        String host = players.newPlayerToken("h1");
        String other = players.newPlayerToken("h2");
        String ben = players.newPlayerToken("ben");
        long first = openTable(host, 6);
        long second = openTable(other, 6);
        join(first, ben, 500).andExpect(status().isCreated());
        join(first, ben, 500).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("already sitting at this table")));
        join(second, ben, 500).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("another table")));
        postJson("/api/v1/tables", host, """
                {"name":"Second","smallBlind":5,"bigBlind":10,"maxSeats":6}""").andExpect(status().isConflict());
    }

    @Test
    void aFullTableRefusesTheNextPlayer() throws Exception {
        String host = players.newPlayerToken("full");
        long table = openTable(host, 2);
        join(table, players.newPlayerToken("p1"), 500).andExpect(status().isCreated());
        join(table, players.newPlayerToken("p2"), 500).andExpect(status().isCreated());
        join(table, players.newPlayerToken("p3"), 500).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("The table is full."));
    }

    @Test
    void topUpsAddAndACountedCashOutFreesTheSeat() throws Exception {
        String host = players.newPlayerToken("tu");
        String cleo = players.newPlayerToken("cleo");
        long table = openTable(host, 6);
        join(table, cleo, 1000);
        postJson("/api/v1/tables/" + table + "/seats/me/buy-ins", cleo, "{\"points\":500}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.seats[0].stack").value(1500));
        postJson("/api/v1/tables/" + table + "/seats/me/cash-out", cleo, "{\"points\":1730}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.seats.length()").value(0));

        Long bought = jdbc.queryForObject("""
                select sum(b.points) from buy_in b join seat s on s.id = b.seat_id where s.table_id = ?""", Long.class, table);
        Long counted = jdbc.queryForObject("""
                select c.points from cash_out c join seat s on s.id = c.seat_id where s.table_id = ?""", Long.class, table);
        assertThat(bought).isEqualTo(1500L);
        assertThat(counted).isEqualTo(1730L);

        String dev = players.newPlayerToken("dev");
        join(table, dev, 1000).andExpect(jsonPath("$.seats[0].seatNumber").value(1));   // seat 1 is free again
    }

    @Test
    void aFractionalAmountIsRefusedNotTruncated() throws Exception {
        String host = players.newPlayerToken("frac");
        long table = openTable(host, 6);
        join(table, host, 999.5).andExpect(status().isBadRequest());
        join(table, host, 1000).andExpect(status().isCreated());
        postJson("/api/v1/tables/" + table + "/seats/me/buy-ins", host, "{\"points\":500.9}")
                .andExpect(status().isBadRequest());
        postJson("/api/v1/tables/" + table + "/seats/me/cash-out", host, "{\"points\":1000.9}")
                .andExpect(status().isBadRequest());

        Long cashOuts = jdbc.queryForObject("""
                select count(*) from cash_out c join seat s on s.id = c.seat_id where s.table_id = ?""", Long.class, table);
        Long bought = jdbc.queryForObject("""
                select sum(b.points) from buy_in b join seat s on s.id = b.seat_id where s.table_id = ?""", Long.class, table);
        assertThat(cashOuts).isZero();
        assertThat(bought).isEqualTo(1000L);
        mvc.perform(get("/api/v1/tables/" + table).header("Authorization", bearer(host)))
                .andExpect(jsonPath("$.seats[0].stack").value(1000));
    }

    @Test
    void aCashOutOfNothingIsAllowed() throws Exception {
        String host = players.newPlayerToken("zero");
        long table = openTable(host, 6);
        join(table, host, 1000);
        postJson("/api/v1/tables/" + table + "/seats/me/cash-out", host, "{\"points\":0}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.seats.length()").value(0));

        Long rows = jdbc.queryForObject("""
                select count(*) from cash_out c join seat s on s.id = c.seat_id where s.table_id = ?""", Long.class, table);
        Long counted = jdbc.queryForObject("""
                select c.points from cash_out c join seat s on s.id = c.seat_id where s.table_id = ?""", Long.class, table);
        Long leftAt = jdbc.queryForObject("select count(*) from seat where table_id = ? and left_at is not null", Long.class, table);
        assertThat(rows).isEqualTo(1L);
        assertThat(counted).isZero();
        assertThat(leftAt).isEqualTo(1L);
        join(table, host, 500).andExpect(status().isCreated());   // the seat is free: the same player can sit again
    }

    @Test
    void aCashOutWithoutAPointsFieldIsRefusedNotTakenAsZero() throws Exception {
        String host = players.newPlayerToken("nopts");
        long table = openTable(host, 6);
        join(table, host, 1000);
        postJson("/api/v1/tables/" + table + "/seats/me/cash-out", host, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(containsString("points")));

        Long cashOuts = jdbc.queryForObject("""
                select count(*) from cash_out c join seat s on s.id = c.seat_id where s.table_id = ?""", Long.class, table);
        assertThat(cashOuts).isZero();
        mvc.perform(get("/api/v1/tables/" + table).header("Authorization", bearer(host)))
                .andExpect(jsonPath("$.seats.length()").value(1));   // still seated
    }

    @Test
    void anUnreadableBodyNamesTheFieldAndTheFixButNeverTheValue() throws Exception {
        String host = players.newPlayerToken("body");
        long table = openTable(host, 6);
        join(table, host, 1000);
        String cashOut = "/api/v1/tables/" + table + "/seats/me/cash-out";

        postJson(cashOut, host, "{\"points\":1234.56}")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("points: use a whole number of points, in steps of 5"));
        postJson(cashOut, host, "{\"points\":true}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("points: use a whole number of points, in steps of 5"));
        postJson(cashOut, host, "{\"points\":\"secret-value\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(not(containsString("secret-value"))))
                .andExpect(jsonPath("$.detail").value(containsString("points")));
        postJson("/api/v1/tables", host, "{\"name\":\"Friday\",\"smallBlind\":5,\"bigBlind\":10}")   // no maxSeats
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("maxSeats")));
        postJson("/api/v1/tables", host, "{\"name\":\"Friday\",\"smallBlind\":5,\"bigBlind\":10,\"maxSeats\":\"six\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("maxSeats")))
                .andExpect(jsonPath("$.detail").value(not(containsString("six"))));
        postJson(cashOut, host, "{\"points\":")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("The request body isn't valid JSON."));
        postJson(cashOut, host, "[1,2]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("The request body isn't valid JSON."));
    }

    @Test
    void onlyTheHostClosesAndOnlyAnEmptyTable() throws Exception {
        String host = players.newPlayerToken("closer");
        String eli = players.newPlayerToken("eli");
        long table = openTable(host, 6);
        join(table, eli, 1000);
        postJson("/api/v1/tables/" + table + "/close", eli, "{}").andExpect(status().isForbidden());
        postJson("/api/v1/tables/" + table + "/close", host, "{}").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("still seated")));
        postJson("/api/v1/tables/" + table + "/seats/me/cash-out", eli, "{\"points\":1000}");
        postJson("/api/v1/tables/" + table + "/close", host, "{}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.open").value(false));
        join(table, eli, 1000).andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value(containsString("closed")));
        String list = mvc.perform(get("/api/v1/tables").header("Authorization", bearer(eli)))
                .andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<java.util.List<Integer>>read(list, "$[*].id")).doesNotContain((int) table);
    }

    @Test
    void theDatabaseItselfRefusesTwoOpenSeatsForOnePlayer() throws Exception {
        String host = players.newPlayerToken("db");
        long table = openTable(host, 6);
        join(table, host, 500);
        Long playerId = jdbc.queryForObject("select player_id from seat where table_id = ?", Long.class, table);
        assertThatThrownBy(() -> jdbc.update("""
                insert into seat (table_id, player_id, seat_number, stack, joined_at) values (?, ?, 2, 500, now())""",
                table, playerId)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
