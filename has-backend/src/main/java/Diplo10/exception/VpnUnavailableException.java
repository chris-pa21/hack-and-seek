package Diplo10.exception;

/**
 * Thrown when the WireGuard server cannot answer, for example because its
 * container is not running. Mapped to 503 by GlobalExceptionHandler.
 */
public class VpnUnavailableException extends RuntimeException {

    public VpnUnavailableException(String message) {
        super(message);
    }
}
