package com.spade.api;

import com.spade.api.dto.JoinTableRequest;
import com.spade.api.dto.OpenTableRequest;
import com.spade.api.dto.PointsRequest;
import com.spade.api.dto.TableResponse;
import com.spade.service.SpadePrincipal;
import com.spade.service.TableService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tables")
public class TableController {

    private final TableService tables;

    public TableController(TableService tables) {
        this.tables = tables;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TableResponse open(@AuthenticationPrincipal SpadePrincipal me, @Valid @RequestBody OpenTableRequest r) {
        return TableResponse.from(tables.open(me, r.name(), r.smallBlind(), r.bigBlind(), r.maxSeats()));
    }

    @GetMapping
    public List<TableResponse> openTables() {
        return tables.openTables().stream().map(TableResponse::from).toList();
    }

    @GetMapping("/{tableId}")
    public TableResponse get(@PathVariable long tableId) {
        return TableResponse.from(tables.get(tableId));
    }

    @PostMapping("/{tableId}/seats")
    @ResponseStatus(HttpStatus.CREATED)
    public TableResponse join(@AuthenticationPrincipal SpadePrincipal me, @PathVariable long tableId,
                              @RequestBody JoinTableRequest r) {
        return TableResponse.from(tables.join(me, tableId, r.buyIn()));
    }

    @PostMapping("/{tableId}/seats/me/buy-ins")
    public TableResponse topUp(@AuthenticationPrincipal SpadePrincipal me, @PathVariable long tableId,
                               @RequestBody PointsRequest r) {
        return TableResponse.from(tables.topUp(me, tableId, r.points()));
    }

    @PostMapping("/{tableId}/seats/me/cash-out")
    public TableResponse cashOut(@AuthenticationPrincipal SpadePrincipal me, @PathVariable long tableId,
                                 @RequestBody PointsRequest r) {
        return TableResponse.from(tables.cashOut(me, tableId, r.points()));
    }

    @PostMapping("/{tableId}/close")
    public TableResponse close(@AuthenticationPrincipal SpadePrincipal me, @PathVariable long tableId) {
        return TableResponse.from(tables.close(me, tableId));
    }
}
