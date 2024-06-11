/*
 * Copyright (C) 2007-2023 Crafter Software Corporation. All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as published by
 * the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.craftercms.studio.impl.v2.utils.git;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.studio.api.v2.exception.git.cli.GitCliException;
import org.craftercms.studio.api.v2.exception.git.cli.GitCliOutputException;
import org.craftercms.studio.api.v2.utils.git.cli.GitCliOutputExceptionResolver;
import org.craftercms.studio.impl.v2.utils.git.cli.CompositeGitCliExceptionResolver;
import org.craftercms.studio.impl.v2.utils.git.cli.NoChangesToCommitExceptionResolver;
import org.craftercms.studio.impl.v2.utils.git.cli.RepositoryLockedExceptionResolver;
import org.eclipse.jgit.lib.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.apache.commons.lang3.StringUtils.trim;
import static org.craftercms.studio.api.v1.constant.StudioConstants.TMP_FILE_SUFFIX;
import static org.craftercms.studio.api.v2.utils.StudioUtils.getStudioTemporaryFilesRoot;

/**
 * Allows doing Git operations throw the CLI.
 * <br />
 * If you ever use this class, please lock/synchronize the calls (hopefully with the
 * {@link org.craftercms.studio.api.v1.service.GeneralLockService})
 *
 * @author Sumer Jabri
 * @author Alfonso Vasquez
 * @since 3.1.23
 */
public class GitCli {

    private static final Logger logger = LoggerFactory.getLogger(GitCli.class);

    private static final String DEFAULT_GIT_COMMAND_NAME = "git";
    private static final int DEFAULT_GIT_PROC_WAIT_FOR_TIMEOUT = 60 * 5; // 5 minutes
    private static final int DEFAULT_GIT_PROC_DESTROY_WAIT_FOR_TIMEOUT = 30;
    private static final int PATHS_BATCH_SIZE = 10;

    /**
     * The first 0 means to remove the path; the SHA-1 does not matter as long as it is well formatted. Then a tab followed by the path.
     * <a href="https://git-scm.com/docs/git-update-index#_using_index_info">See git docs</a>
     */
    private static final String DELETE_INDEX_INFO_FORMAT = "0 0000000000000000000000000000000000000000\t%s\n";

    // Exception resolvers
    public final GitCliOutputExceptionResolver DEFAULT_EX_RESOLVER = RepositoryLockedExceptionResolver.INSTANCE;
    public final GitCliOutputExceptionResolver COMMIT_EX_RESOLVER = new CompositeGitCliExceptionResolver(
            RepositoryLockedExceptionResolver.INSTANCE, NoChangesToCommitExceptionResolver.INSTANCE);

    private final String gitProcName;
    private final int gitProcWaitForTimeoutSecs;
    private final int gitProcDestroyWaitForTimeoutSecs;

    public GitCli() {
        this.gitProcName = DEFAULT_GIT_COMMAND_NAME;
        this.gitProcWaitForTimeoutSecs = DEFAULT_GIT_PROC_WAIT_FOR_TIMEOUT;
        this.gitProcDestroyWaitForTimeoutSecs = DEFAULT_GIT_PROC_DESTROY_WAIT_FOR_TIMEOUT;
    }

    public GitCli(String gitProcName, int gitProcWaitForTimeoutSecs, int gitProcDestroyWaitForTimeoutSecs) {
        this.gitProcName = gitProcName;
        this.gitProcWaitForTimeoutSecs = gitProcWaitForTimeoutSecs;
        this.gitProcDestroyWaitForTimeoutSecs = gitProcDestroyWaitForTimeoutSecs;
    }

    protected String executeGitCommand(String directory, GitCommandLine commandLine) throws IOException, InterruptedException {
        return executeGitCommand(directory, commandLine, DEFAULT_EX_RESOLVER, null);
    }

    protected String executeGitCommand(String directory, GitCommandLine commandLine, File inputFile)
            throws IOException, InterruptedException {
        return executeGitCommand(directory, commandLine, DEFAULT_EX_RESOLVER, inputFile);
    }

    protected String executeGitCommand(String directory, GitCommandLine commandLine, GitCliOutputExceptionResolver exceptionResolver) throws IOException, InterruptedException {
        return executeGitCommand(directory, commandLine, exceptionResolver, null);
    }

    protected String executeGitCommand(String directory, GitCommandLine commandLine,
                                       final GitCliOutputExceptionResolver exceptionResolver, final File inputFile) throws IOException, InterruptedException {
        checkGitDirectory(directory);
        return doExecuteGitCommand(directory, commandLine, exceptionResolver, inputFile);
    }

    private String doExecuteGitCommand(final String directory, final GitCommandLine commandLine,
                                       final GitCliOutputExceptionResolver exceptionResolver)
            throws IOException, InterruptedException {
        return doExecuteGitCommand(directory, commandLine, exceptionResolver, null);
    }

    private String doExecuteGitCommand(final String directory, final GitCommandLine commandLine,
                                       final GitCliOutputExceptionResolver exceptionResolver, final File inputFile)
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(commandLine).directory(new File(directory));
        if (inputFile != null) {
            pb.redirectInput(inputFile);
        }
        logger.debug("Executing git command: '{}'", commandLine);

        // Start process
        Process p = pb.start();

        InputStream processInputStream = p.getInputStream();
        InputStream processErrorStream = p.getErrorStream();
        try {
            // Wait for the process to finish, up to gitProcWaitForTimeoutSecs
            boolean exited = p.waitFor(gitProcWaitForTimeoutSecs, TimeUnit.SECONDS);
            if (!exited) {
                handleProcessTimeout(p, directory, processInputStream, processErrorStream);
            }

            int exitValue = p.exitValue();
            if (exitValue != 0) {
                handleErrorExitValue(directory, exceptionResolver, p, processInputStream);
            }

            // Read std output if process has finished successfully
            String output = IOUtils.toString(p.getInputStream(), Charset.defaultCharset());
            logger.debug("Git command successfully executed on '{}':\n'{}'", directory, output);
            return output;
        } finally {
            IOUtils.closeQuietly(processInputStream);
            IOUtils.closeQuietly(processErrorStream);
            if (p.isAlive()) {
                // Destroy process
                destroyProcess(p);
            }
        }
    }

    private void handleErrorExitValue(String directory, GitCliOutputExceptionResolver exceptionResolver,
                                      Process p, InputStream processInputStream) throws IOException {
        int exitValue = p.exitValue();
        String errorOutput = IOUtils.toString(p.getErrorStream(), Charset.defaultCharset());
        String stdOutput = IOUtils.toString(processInputStream, Charset.defaultCharset());

        String errorMessage = format("Git command failed with exit value '%s' on '%s':\n\nSTDOUT: '%s'\nSTDERR: '%s'", exitValue, directory, stdOutput, errorOutput);
        logger.debug(errorMessage);

        throw Optional
                .ofNullable(exceptionResolver.resolveException(exitValue, errorOutput))
                .or(() -> Optional.ofNullable(exceptionResolver.resolveException(exitValue, stdOutput)))
                .orElse(new GitCliOutputException(exitValue, errorMessage));
    }

    private void handleProcessTimeout(Process p, String directory, InputStream processInputStream, InputStream processErrorStream) throws IOException {
        // Read available bytes, avoiding blocking
        String stdOutput = new String(processInputStream.readNBytes(processInputStream.available()));
        String errorOutput = new String(processErrorStream.readNBytes(processErrorStream.available()));
        destroyProcess(p);
        String errorMessage = format("Timeout while waiting for git command to exit on '%s'\nSTDOUT: '%s'\nSTDERR: '%s'", directory, stdOutput, errorOutput);
        logger.debug(errorMessage);
        throw new GitCliException(errorMessage);
    }

    /**
     * Destroys the process. It will wait for {@link #gitProcDestroyWaitForTimeoutSecs} seconds for the process to
     * exit, and if it does not, it will destroy it forcibly.
     *
     * @param process the process
     */
    private void destroyProcess(Process process) {
        try {
            logger.debug("Destroying process with PID '{}'", process.pid());
            process.destroy();
            boolean destroyed = process.waitFor(gitProcDestroyWaitForTimeoutSecs, TimeUnit.SECONDS);
            if (!destroyed) {
                logger.warn("Git process with PID '{}' did not exit after '{}' seconds, destroying it", process.pid(), gitProcDestroyWaitForTimeoutSecs);
                process.destroyForcibly();
                process.waitFor();
                logger.debug("Process with PID '{}' destroyed", process.pid());
            }
        } catch (InterruptedException e) {
            logger.warn("Interrupted while waiting for process with PID '{}' to exit", process.pid(), e);
        }
    }

    /**
     * Checks if the given directory exists and is a Git repository.
     *
     * @param directory the directory to check
     * @throws GitCliException if the directory does not exist or is not a Git repository
     */
    private void checkGitDirectory(final String directory) throws GitCliException {
        if (Files.notExists(Paths.get(directory))) {
            throw new GitCliException(format("Directory '%s' does not exist", directory));
        }

        GitCommandLine checkGitDir = new GitCommandLine("rev-parse", "--git-dir");
        try {
            doExecuteGitCommand(directory, checkGitDir, DEFAULT_EX_RESOLVER);
        } catch (Exception e) {
            throw new GitCliException(format("Directory '%s' is not a Git repository", directory), e);
        }
    }

    public void add(String directory, String... paths) throws GitCliException {
        try {
            executeGitCommand(directory, new GitCommandLine("add", paths));
        } catch (Exception e) {
            throw new GitCliException("Git add failed on directory " + directory + " for paths " +
                    ArrayUtils.toString(paths), e);
        }
    }

    /**
     * Remove the given paths from the index and discard changes
     * @param directory the git repository directory
     * @param paths the paths to restore
     * @return the output of the git restore command
     */
    public String restore(String directory, String... paths) throws GitCliException {
        GitCommandLine restoreCl = new GitCommandLine("restore");
        restoreCl.addParam("--source=HEAD");
        restoreCl.addParam("--staged");
        restoreCl.addParam("--worktree");
        restoreCl.addParams(paths);
        try {
            return trim(executeGitCommand(directory, restoreCl));
        } catch (Exception e) {
            throw new GitCliException(format("Git restore failed on directory '%s' for paths %s", directory, ArrayUtils.toString(paths)), e);
        }
    }

    public String commit(String directory, String author, String message, String... paths) throws GitCliException {
        GitCommandLine commitCl = new GitCommandLine("commit");
        GitCommandLine revParseCl = new GitCommandLine("rev-parse", "HEAD");

        commitCl.addOption("--author", author);
        commitCl.addOption("--message", message);
        commitCl.addParams(paths);

        try {
            executeGitCommand(directory, commitCl, COMMIT_EX_RESOLVER);
            return trim(executeGitCommand(directory, revParseCl));
        } catch (Exception e) {
            throw new GitCliException("Git commit failed on directory " + directory + " for paths " +
                    ArrayUtils.toString(paths), e);
        }
    }

    /**
     * Commit a tree to the repository
     *
     * @param repoDir        the repository directory
     * @param treeId           id of the tree to commit
     * @param parentCommitId the parent commit id
     * @param comment        the commit comment
     * @return the commit id (git commit-tree output)
     * @throws IOException if an error occurs executing the git command or when accessing the file system
     */
    public String commitTree(File repoDir, String treeId, ObjectId parentCommitId, String comment) throws IOException {
        GitCommandLine commitTreeCl = new GitCommandLine("commit-tree");
        commitTreeCl.addParam(treeId);
        commitTreeCl.addParams("-p", parentCommitId.getName());
        final Path commentTempFile = Files.createTempFile(getStudioTemporaryFilesRoot(), UUID.randomUUID().toString(), TMP_FILE_SUFFIX);

        try {
            Files.write(commentTempFile, comment.getBytes());
            commitTreeCl.addParams("-F", commentTempFile.toRealPath().toString());
            return trim(executeGitCommand(repoDir.getAbsolutePath(), commitTreeCl));
        } catch (Exception e) {
            throw new GitCliException("Git commit-tree failed on directory " + repoDir.getAbsolutePath(), e);
        } finally {
            Files.deleteIfExists(commentTempFile);
        }
    }

    public boolean isRepoClean(String directory) throws GitCliException {
        GitCommandLine statusCl = new GitCommandLine("status");
        // The --porcelain option is a short version specifically for scripts
        statusCl.addParam("--porcelain");

        try {
            String result = executeGitCommand(directory, statusCl, DEFAULT_EX_RESOLVER);

            // No result means there's no changes, so the repo is clean
            return StringUtils.isEmpty(result);
        } catch (Exception e) {
            throw new GitCliException("Git GC failed on directory " + directory, e);
        }
    }

    /**
     * Update index and write a new tree to the repository
     * This method will read a tree from the parent commit, update the index with the <code>commitId</code> version
     * of the files and then write the tree
     *
     * @param directory      the repository directory
     * @param paths          the paths to write to the tree
     * @param deletedPaths   the paths to delete from the tree
     * @param commitId       the commit id to get the new versions from
     * @param parentCommitId the parent to read initial tree from
     * @return the new tree id
     * @throws IOException
     * @throws InterruptedException
     */
    public String writeTree(final File directory, final List<String> paths, final List<String> deletedPaths, final String commitId, final ObjectId parentCommitId)
            throws IOException, InterruptedException {
        // git read-tree target_branch
        GitCommandLine readTreeCl = new GitCommandLine("read-tree", parentCommitId.getName());
        executeGitCommand(directory.getAbsolutePath(), readTreeCl);
        final Path indexInfoTempFile = Files.createTempFile(getStudioTemporaryFilesRoot(), UUID.randomUUID().toString(), TMP_FILE_SUFFIX);
        try {
            for (String deletedPath : emptyIfNull(deletedPaths)) {
                Files.writeString(indexInfoTempFile, format(DELETE_INDEX_INFO_FORMAT, deletedPath), StandardOpenOption.APPEND);
            }
            // In batches, call git ls-tree and create index info file
            for (List<String> pathsBatch : ListUtils.partition(emptyIfNull(paths), PATHS_BATCH_SIZE)) {
                GitCommandLine lsTreeCl = new GitCommandLine("ls-tree", commitId);
                pathsBatch.forEach(lsTreeCl::addParam);
                String lsTreeOutput = executeGitCommand(directory.getAbsolutePath(), lsTreeCl);
                Files.write(indexInfoTempFile, lsTreeOutput.getBytes(), StandardOpenOption.APPEND);
            }
            // Update index with correct version of published files
            GitCommandLine updateIndexCl = new GitCommandLine("update-index", "--index-info");
            executeGitCommand(directory.getAbsolutePath(), updateIndexCl, indexInfoTempFile.toRealPath().toFile());

            // git write-tree
            GitCommandLine writeTreeCl = new GitCommandLine("write-tree");
            return trim(executeGitCommand(directory.getAbsolutePath(), writeTreeCl));
        } finally {
            Files.deleteIfExists(indexInfoTempFile);
        }
    }

    protected class GitCommandLine extends ArrayList<String> {

        public GitCommandLine(String command) {
            add(gitProcName);
            add(command);
        }

        public GitCommandLine(String command, String... params) {
            add(gitProcName);
            add(command);
            addParams(params);
        }

        public void addParam(String param) {
            add(param);
        }

        public void addParams(String... params) {
            if (ArrayUtils.isNotEmpty(params)) {
                for (String arg : params) {
                    addParam(arg);
                }
            }
        }

        public void addOption(String optName, String optValue) {
            addParam(optName);
            addParam("\"" + optValue + "\"");
        }

    }

}
