package com.smith.domain.exception;

import java.util.List;

public class UnknownModelException extends RuntimeException {

    public UnknownModelException(String alias, List<String> available) {
        super("Unknown model alias: '" + alias + "'. Available models: " + available);
    }
}
