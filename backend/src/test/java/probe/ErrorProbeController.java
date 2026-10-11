package probe;

import com.spade.domain.DomainException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Throws on purpose so ApiErrorsTest can look at what a client receives. Test-only. */
@RestController
public class ErrorProbeController {

    public record Named(@NotBlank String name) {
    }

    @GetMapping("/probe/boom")
    public String boom() {
        throw new IllegalStateException("secret-db-password=hunter2 at com.spade.Internal.line(42)");
    }

    @GetMapping("/probe/conflict")
    public String conflict() {
        throw new DomainException(DomainException.Kind.CONFLICT, "The table is full.");
    }

    @GetMapping("/probe/duplicate")
    public String duplicate() {
        throw new DataIntegrityViolationException("duplicate key value violates unique constraint \"seat_taken\"");
    }

    @PostMapping("/probe/named")
    public String named(@Valid @RequestBody Named named) {
        return named.name();
    }
}
