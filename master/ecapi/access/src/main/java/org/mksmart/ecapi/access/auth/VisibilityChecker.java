package org.mksmart.ecapi.access.auth;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.mksmart.ecapi.access.Config;
import org.mksmart.ecapi.access.UnavailablePolicyTableException;
import org.mksmart.ecapi.api.Catalogue;
import org.mksmart.ecapi.api.access.PolicyTable;
import org.mksmart.ecapi.core.LaunchConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The object responsible for answering requests on whether one or more datasets are openly accessible.
 * 
 * This implementation uses a thread that periodically rebuilds the whole table and relies on a supplied
 * {@link Catalogue} to obtain the list of known datasets and an {@link AuthServerPolicyChecker} to check
 * their access policies.
 * 
 * You should expect an instance of this class to be interrogated even when no authorisation server is set. If
 * so, it will immediately respond that all the datasets are open.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public class VisibilityChecker {

    private static VisibilityChecker me;

    static final String USER_EVERYONE = "guest";

    public static VisibilityChecker getInstance() {
        if (me == null) throw new IllegalStateException(
                "The singleton for " + VisibilityChecker.class
                        + " cannot be obtained immediately. It must be initialised"
                        + " at least once by a call to a non-default constructor.");
        return me;
    }

    private Catalogue catalogue;

    private AuthServerPolicyChecker checker;

    private Logger log = LoggerFactory.getLogger(getClass());

    /*
     * Not setting as volatile right now as we rely on a thread-safe implementation.
     */
    private PolicyTable policies;

    public VisibilityChecker(final Catalogue catalogue) {
        if (me != null) log
                .warn("Re-instantiating existing VisibilityChecker! There should be no need for this.");
        LaunchConfiguration config = LaunchConfiguration.getInstance();
        this.catalogue = catalogue;
        this.checker = new AuthServerPolicyChecker();
        if (!config.has(Config.KEYMGMT_AUTHSVR_HOST)) {
            log.warn("No authorisation server URI given. Treating all datasets as open!");
            // Run once if there is no authorisation server.
            doPolicies();
        } else {
            // Otherwise, schedule a periodic check.
            final Runnable poller = new Runnable() {
                public void run() {
                    doPolicies();
                }
            };
            final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            final ScheduledFuture<?> handle = scheduler
                    .scheduleWithFixedDelay(poller, 0, 2, TimeUnit.MINUTES);
        }
        me = this; // Important because there is no default constructor.
    }

    protected void doPolicies() {
        log.trace("Scheduled rebuilding of policy table starts.");
        Set<String> dsSt = new HashSet<>();
        log.trace("List of datasets to be checked follows:");
        long before = System.currentTimeMillis();
        for (URI ds : catalogue.getDatasets()) {
            log.trace(" - {}", ds);
            dsSt.add(ds.toString());
        }
        log.trace("Dataset list rebuilt. Time: {} ms", System.currentTimeMillis() - before);
        before = System.currentTimeMillis();
        PolicyTable tempPt = this.checker.getPolicies(dsSt);
        if (tempPt == null) log.error("Policy table rebuild failed! Locking down ECAPI until next attempt.");
        policies = tempPt;
        log.debug("Policy table rebuilt. Time: {} ms", System.currentTimeMillis() - before);
    }

    public Set<String> filter(final Set<String> datasets) throws UnavailablePolicyTableException {
        return filter(datasets, USER_EVERYONE);
    }

    public Set<String> filter(final Set<String> datasets, String user) throws UnavailablePolicyTableException {
        if (policies == null) throw new UnavailablePolicyTableException(
                "Could not determine access policies for contributing datasets. Policy table is null.");
        long before = System.currentTimeMillis();
        Set<String> filtered = new HashSet<>();
        for (String ds : datasets)
            if (!this.policies.containsKey(ds) || this.policies.get(ds)) {
                log.debug("Marking dataset <{}> as OPEN.", ds);
                filtered.add(ds);
            } else log.debug("Marking dataset <{}> as CLOSED.", ds);
        log.debug(" ... time taken for filtering datasets: {} ms", System.currentTimeMillis() - before);
        return filtered;
    }

    public boolean isDatasetOpen(final String dataset) {
        return isDatasetOpen(dataset, USER_EVERYONE);
    }

    public boolean isDatasetOpen(final String dataset, String user) {
        return !this.policies.containsKey(dataset) || this.policies.get(dataset);
    }

}
