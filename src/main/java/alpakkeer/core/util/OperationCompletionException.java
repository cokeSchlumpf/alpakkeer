package alpakkeer.core.util;

public final class OperationCompletionException extends RuntimeException {

    private OperationCompletionException(Throwable cause) {
        super(cause);
    }

    public static OperationCompletionException apply(Throwable cause) {
        return new OperationCompletionException(cause);
    }

}
