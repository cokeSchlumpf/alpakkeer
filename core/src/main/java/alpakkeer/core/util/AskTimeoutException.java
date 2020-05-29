package alpakkeer.core.util;

public final class AskTimeoutException extends RuntimeException {

    private AskTimeoutException(String message) {
        super(message);
    }

    public static AskTimeoutException apply() {
        String message = "Operation reached timeout";
        return new AskTimeoutException(message);
    }

}
