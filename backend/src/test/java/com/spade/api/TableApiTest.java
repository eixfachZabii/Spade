package com.spade.api;

import static com.spade.api.TestPlayers.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        join(table, host, 0).andExpect(status().isBadRequest());
        join(table, host, -100).andExpect(status().isBadRequest());
        join(table, host, 1003).andExpect(status().isBadRequest());
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
    void aCashOutOfNothingIsAllowed() throws Exception {
        String host = players.newPlayerToken("zero");
        long table = openTable(host, 6);
        join(table, host, 1000);
        postJson("/api/v1/tables/" + table + "/seats/me/cash-out", host, "{\"points\":0}").andExpect(status().isOk());
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
