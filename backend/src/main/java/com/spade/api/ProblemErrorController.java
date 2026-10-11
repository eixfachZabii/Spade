package com.spade.api;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Where the servlet container sends errors that never reached a controller: a security filter's
 * {@code sendError}, the firewall's 400, a container 413, an exception thrown inside a filter. Boot's own
 * {@code BasicErrorController} answers those with plain {@code application/json} (or an HTML page); this
 * bean replaces it so that every non-2xx response is RFC 9457 problem+json, whatever the Accept header says.
 *
 * <p>Only the status is used. The error message, the exception and the request path are deliberately never
 * read, so nothing a request or a failure carried can reach the client.
 */
@RestController
public class ProblemErrorController implements ErrorController {

    @RequestMapping("${server.error.path:${error.path:/error}}")
    public ResponseEntity<ProblemDetail> error(HttpServletRequest request) {
        HttpStatus status = statusOf(request);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ProblemDetail.forStatusAndDetail(status, detail(status)));
    }

    private static HttpStatus statusOf(HttpServletRequest request) {
        Object code = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (code instanceof Integer value) {
            HttpStatus status = HttpStatus.resolve(value);
            if (status != null && status.isError()) {
                return status;
            }
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /** Fixed text per status; written for the person at the table. */
    static String detail(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "That request was not valid. Check it and try again.";
            case UNAUTHORIZED -> "Sign in and try again.";
            case FORBIDDEN -> "You are not allowed to do that.";
            case NOT_FOUND -> "There is nothing at that address.";
            case METHOD_NOT_ALLOWED -> "That address does not accept this kind of request.";
            case PAYLOAD_TOO_LARGE -> "That request was too large.";
            case TOO_MANY_REQUESTS -> "Too many requests. Wait a moment and try again.";
            default -> status.is4xxClientError()
                    ? "That request could not be processed."
                    : "Something went wrong on our side.";
        };
    }
}
