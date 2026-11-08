package eu.isygoit.plan.exception;

import eu.isygoit.annotation.MsgLocale;
import eu.isygoit.exception.ManagedException;
import eu.isygoit.plan.domain.Diagnostics;
import org.springframework.http.HttpStatus;

@MsgLocale(value = "plan.error.infeasible", httpStatus = HttpStatus.UNPROCESSABLE_ENTITY)
public final class InfeasibleException extends ManagedException {
    private final Diagnostics diagnostics;

    public InfeasibleException(Diagnostics diagnostics) {
        super(diagnostics == null ? "Planning problem is infeasible" : diagnostics.message());
        this.diagnostics = diagnostics == null ? new Diagnostics(getMessage(), null, null) : diagnostics;
    }

    public Diagnostics diagnostics() {
        return diagnostics;
    }
}
