package org.mksmart.ecapi.it;

import java.io.File;
import java.util.Arrays;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteResultHandler;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.apache.commons.exec.util.StringUtils;
import org.apache.stanbol.commons.testing.jarexec.JarExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A modified version of the Stanbol {@link JarExecutor} that accepts custom program arguments after VM args.
 * Note that argument p is still reserved to server port.
 * 
 * 
 * @see JarExecutor
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public class JarRunner {

    private static JarRunner instance;
    private final File jarToExecute;
    private final String javaExecutable;
    private final File workingDirectory;
    private final int serverPort;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final int DEFAULT_PORT = 8765;
    public static final String DEFAULT_JAR_FOLDER = "target/dependency";
    public static final String DEFAULT_JAR_NAME_REGEXP = "org.apache.stanbol.*full.*jar$";
    public static final String PROP_PREFIX = "jar.executor.";
    public static final String PROP_SERVER_PORT = PROP_PREFIX + "server.port";
    public static final String PROP_JAR_FOLDER = PROP_PREFIX + "jar.folder";
    public static final String PROP_JAR_NAME_REGEXP = PROP_PREFIX + "jar.name.regexp";
    public static final String PROP_WORKING_DIRECTORY = PROP_PREFIX + "workingdirectory";

    @SuppressWarnings("serial")
    public static class ExecutorException extends Exception {

        ExecutorException(String reason) {
            super(reason);
        }

        ExecutorException(String reason, Throwable cause) {
            super(reason, cause);
        }
    }

    public int getServerPort() {
        return serverPort;
    }

    public static JarRunner getInstance(Properties config) throws ExecutorException {
        if (instance == null) {
            synchronized (JarRunner.class) {
                if (instance == null) {
                    instance = new JarRunner(config);
                }
            }
        }
        return instance;
    }

    /**
     * Build a JarExecutor, locate the jar to run, etc
     */
    private JarRunner(Properties config) throws ExecutorException {
        final boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");

        String portStr = config.getProperty(PROP_SERVER_PORT);
        serverPort = portStr == null ? DEFAULT_PORT : Integer.valueOf(portStr);

        javaExecutable = isWindows ? "java.exe" : "java";

        String jarFolderPath = config.getProperty(PROP_JAR_FOLDER);
        jarFolderPath = jarFolderPath == null ? DEFAULT_JAR_FOLDER : jarFolderPath;
        final File jarFolder = new File(jarFolderPath);

        String jarNameRegexp = config.getProperty(PROP_JAR_NAME_REGEXP);
        jarNameRegexp = jarNameRegexp == null ? DEFAULT_JAR_NAME_REGEXP : jarNameRegexp;
        final Pattern jarPattern = Pattern.compile(jarNameRegexp);

        // Find executable jar
        final String[] candidates = jarFolder.list();
        if (candidates == null) {
            throw new ExecutorException("No files found in jar folder specified by " + PROP_JAR_FOLDER
                                        + " property: " + jarFolder.getAbsolutePath());
        }
        File f = null;
        if (candidates != null) {
            for (String filename : candidates) {
                if (jarPattern.matcher(filename).matches()) {
                    f = new File(jarFolder, filename);
                    break;
                }
            }
        }

        if (f == null) {
            throw new ExecutorException("Executable jar matching '" + jarPattern + "' not found in "
                                        + jarFolder.getAbsolutePath() + ", candidates are "
                                        + Arrays.asList(candidates));
        }
        jarToExecute = f;

        String workingDirectoryName = config.getProperty(PROP_WORKING_DIRECTORY);
        if (workingDirectoryName != null) {
            this.workingDirectory = new File(workingDirectoryName);
            if (!this.workingDirectory.exists()) {
                this.workingDirectory.mkdirs();
            } else {
                if (!this.workingDirectory.isDirectory()) {
                    throw new ExecutorException("Specified working directory " + workingDirectoryName
                                                + " is not a directory.");
                }

                if (!this.workingDirectory.canRead()) {
                    throw new ExecutorException("Can't access specified working directory "
                                                + workingDirectoryName);
                }
            }
            log.info("Using " + this.workingDirectory.getAbsolutePath() + " as working directory");
        } else {
            this.workingDirectory = null;
        }
    }

    /**
     * Start the jar if not done yet, and setup runtime hook to stop it.
     */
    public void start() throws Exception {
        final ExecuteResultHandler h = new ExecuteResultHandler() {
            @Override
            public void onProcessFailed(ExecuteException ex) {
                log.error("Process execution failed:" + ex, ex);
            }

            @Override
            public void onProcessComplete(int result) {
                log.info("Process execution complete, exit code=" + result);
            }
        };

        final String vmOptions = System.getProperty("jar.executor.vm.options");
        final String progArgs = System.getProperty("jar.executor.program.options");
        final Executor e = new DefaultExecutor();
        if (this.workingDirectory != null) {
            e.setWorkingDirectory(this.workingDirectory);
        }
        final CommandLine cl = new CommandLine(javaExecutable);
        if (vmOptions != null && vmOptions.length() > 0) {
            // TODO: this will fail if one of the vm options as a quoted value with a space in it, but this is
            // not the case for common usage patterns
            for (String option : StringUtils.split(vmOptions, " "))
                cl.addArgument(option);
        }
        cl.addArgument("-jar");
        cl.addArgument(jarToExecute.getAbsolutePath());
        cl.addArgument("-p");
        cl.addArgument(String.valueOf(serverPort));
        if (progArgs != null && progArgs.length() > 0) {
            // TODO: same as above for vmOptions
            for (String option : StringUtils.split(progArgs, " "))
                cl.addArgument(option);
        }
        log.info("Executing " + cl);
        e.setStreamHandler(new PumpStreamHandler());
        e.setProcessDestroyer(new ShutdownHookProcessDestroyer());
        e.execute(cl, h);
    }

}
