package com.warehouseos.exception;

import java.util.Map;

public class InvalidStateTransitionException extends ApiException {

    public InvalidStateTransitionException(String entity, Object from, Object to) {
        super(ErrorCode.INVALID_STATE_TRANSITION,
                "%s cannot move from %s to %s".formatted(entity, from, to),
                Map.of("entity", entity, "from", String.valueOf(from), "to", String.valueOf(to)));
    }
}
