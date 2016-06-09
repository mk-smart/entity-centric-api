package org.mksmart.ecapi.access;

/**
 * Thrown to indicate that no attempt to check what permissions are configured is going to be made.
 * 
 * This exception should only be thrown by API key drivers that will by default allow an operation.
 * 
 * @author Alessandro Adamou <alexdma@apache.org>
 *
 */
public class NotCheckingRightsException extends Exception {

    private static final long serialVersionUID = 6476253121079203687L;

}
