package org.mksmart.ecapi.api;

public class MisconfiguredException extends RuntimeException {

    protected Object[] failures;

    public MisconfiguredException(String message) {
        super(message);
    }

    public MisconfiguredException(String message, Object... failures) {
        this(message);
    }

    public Object[] getFailingItems() {
        return failures;
    }

}
