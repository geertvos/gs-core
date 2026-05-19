package net.geertvos.gvm.bridge;

public class NativeError extends Exception {

    private final String errorMessage;

    public NativeError(String message) {
        super(message);
        this.errorMessage = message;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
