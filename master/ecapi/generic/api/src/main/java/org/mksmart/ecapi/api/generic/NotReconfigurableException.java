package org.mksmart.ecapi.api.generic;

/**
 * Thrown whenever a disallowed attempt to change the configuration of an object is detected and prevented.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public class NotReconfigurableException extends Exception {

    /**
	 * 
	 */
    private static final long serialVersionUID = 215188285937711939L;

    /**
     * Creates a new instance of {@link NotReconfigurableException}.
     */
    public NotReconfigurableException() {
        super();
    }

    /**
     * Creates a new instance of {@link NotReconfigurableException}.
     * 
     * @param message
     *            the detail message.
     */
    public NotReconfigurableException(String message) {
        super(message);
    }

}
