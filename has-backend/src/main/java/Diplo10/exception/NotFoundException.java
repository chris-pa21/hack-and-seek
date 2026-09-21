package Diplo10.exception;

/**
 * Thrown when a requested entity does not exist. Mapped to 404 by
 * GlobalExceptionHandler.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
