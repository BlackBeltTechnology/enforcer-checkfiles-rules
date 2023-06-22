package org.apache.maven.plugins.enforcer;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rules.AbstractStandardEnforcerRule;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This rule verifies if the globs listed have any match. It doesn't extends
 * <tt>AbstractRequireFiles</tt> because it doesn't handle multiple file's error messages as intended.
 *
 * */
@SuppressWarnings("UnusedDeclaration") // Plugin
public class RequireGlobMatches extends AbstractStandardEnforcerRule {

    /**
     * Location globs relative to.
     */
    public File location;

    /**
     * Files to be checked.
     */
    public String[] globs;

    /**
     * If null file handles should be allowed. If they are allowed, it means treat it as a success.
     */
    public boolean allowNulls = false; // Adopted from other RequireFiles implementation

    @Override
    public void execute() throws EnforcerRuleException {
        checkNotNull(globs, "glob is mandatory");
        checkArgument(globs.length > 0, "at least 1 glob must be specified");

        String newLine = System.getProperty("line.separator");
        StringBuilder sb = new StringBuilder();

        for (String glob : globs) {
            Result result = checkGlobMatchInPath(glob, location);
            if (!result.successful) {
                sb.append(result.errorMessage);
                sb.append(newLine);
            }
        }

        if (sb.length() != 0) {
            throw new EnforcerRuleException(sb.toString() +
                    (getMessage() != null ? getMessage() :
                            "Some files produce errors, please check the error message for the individual file above."));
        }
    }

    protected static void checkNotNull(final Object reference, String errorMessage) {
        if (reference == null) {
            throw new NullPointerException(errorMessage);
        }
    }

    protected static void checkArgument(final boolean condition, String errorMessage) throws EnforcerRuleException {
        if (!condition) {
            throw new EnforcerRuleException(errorMessage);
        }
    }

    /**
     * Scans the given location and return if matches with the given glob.
     * @param glob
     * @param location
     * @return
     * @throws IOException
     */
    private Result checkGlobMatchInPath(String glob, File location) {

        final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);
        final AtomicBoolean ret = new AtomicBoolean(false);

        try {
            Files.walkFileTree(location.toPath(), new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                    Path relPath = location.toPath().relativize(path);
                    getLog().info("Check file: " + relPath);
                    if (pathMatcher.matches(relPath)) {
                        ret.set(true);
                        return FileVisitResult.TERMINATE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc)
                        throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            return Result.fail("IO error not find file with: " + glob + " on location:" + location);
        }

        if (ret.get()) {
            return Result.success();
        } else {
            return Result.fail("Could not find file matches with: " + glob + " on location:" + location);
        }
    }

    /**
     * The checkFile() result object.
     */
    protected static class Result {

        /**
         * True if the check result is successful; otherwise false.
         */
        public boolean successful;
        /**
         * The error message if the result is NOT successful; otherwise empty string.
         */
        public String errorMessage = "";

        /**
         * Creates successful result object.
         *
         * @return the newly created result object
         */
        public static Result success() {
            return new Result(true, "");
        }

        /**
         * Creates successful result object.
         *
         * @return the newly created result object
         */
        public static Result fail(String errorMessage) {
            return new Result(false, errorMessage);
        }

        /**
         * @param successful   true if the check result is successful; otherwise false
         * @param errorMessage the error message if the result is NOT successful; otherwise empty string
         */
        private Result(final boolean successful, final String errorMessage) {
            this.successful = successful;
            this.errorMessage = errorMessage;
        }
    }
    @Override
    public String getCacheId() {
        return "0";
    }
}
