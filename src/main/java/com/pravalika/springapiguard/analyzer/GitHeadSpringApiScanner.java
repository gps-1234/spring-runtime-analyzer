package com.pravalika.springapiguard.analyzer;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiJavaFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class GitHeadSpringApiScanner {

    private final Project project;
    private final SpringApiScanner scanner;

    public GitHeadSpringApiScanner(
            Project project,
            SpringApiScanner scanner
    ) {
        this.project = project;
        this.scanner = scanner;
    }

    public GitHeadResult scanHead()
            throws IOException, InterruptedException {

        File gitRoot =
                findGitRoot();

        String root =
                gitRoot.getAbsolutePath();

        /*
         * Git operations happen OUTSIDE a read action.
         */
        String commit =
                runGit(
                        root,
                        List.of(
                                "rev-parse",
                                "HEAD"
                        )
                ).trim();

        if (commit.isBlank()) {
            throw new IllegalStateException(
                    "Could not determine Git HEAD."
            );
        }

        String fileList =
                runGit(
                        root,
                        List.of(
                                "ls-tree",
                                "-r",
                                "--name-only",
                                "HEAD"
                        )
                );

        List<ApiEndpoint> endpoints =
                new ArrayList<>();

        for (String path :
                fileList.split("\\R")) {

            if (path.isBlank() ||
                    !path.endsWith(".java")) {
                continue;
            }

            /*
             * Get the committed source using Git.
             * Still outside any read action.
             */
            String source =
                    runGit(
                            root,
                            List.of(
                                    "show",
                                    "HEAD:" + path
                            )
                    );

            /*
             * PSI creation and PSI inspection MUST happen
             * inside a read action.
             */
            List<ApiEndpoint> fileEndpoints =
                    ApplicationManager
                            .getApplication()
                            .runReadAction(
                                    (Computable<List<ApiEndpoint>>)
                                            () -> {

                                                PsiFileFactory factory =
                                                        PsiFileFactory
                                                                .getInstance(
                                                                        project
                                                                );

                                                PsiJavaFile psiFile =
                                                        (PsiJavaFile)
                                                                factory
                                                                        .createFileFromText(
                                                                                path,
                                                                                JavaFileType.INSTANCE,
                                                                                source
                                                                        );

                                                return scanner.scanPsiFile(
                                                        psiFile
                                                );
                                            }
                            );

            endpoints.addAll(
                    fileEndpoints
            );
        }

        return new GitHeadResult(
                commit,
                endpoints
        );
    }

    private File findGitRoot() {

        String basePath =
                project.getBasePath();

        if (basePath == null) {
            throw new IllegalStateException(
                    "Project path is unavailable."
            );
        }

        File current =
                new File(basePath)
                        .getAbsoluteFile();

        while (current != null) {

            File git =
                    new File(
                            current,
                            ".git"
                    );

            if (git.exists()) {
                return current;
            }

            current =
                    current.getParentFile();
        }

        try {

            String root =
                    runGit(
                            basePath,
                            List.of(
                                    "rev-parse",
                                    "--show-toplevel"
                            )
                    ).trim();

            if (!root.isBlank()) {
                return new File(root);
            }

        } catch (Exception ignored) {
            // Fall through to the clear error below.
        }

        throw new IllegalStateException(
                """
                No Git repository was found.

                Open a project inside a Git repository.
                """.trim()
        );
    }

    private String runGit(
            String workingDirectory,
            List<String> arguments
    ) throws IOException, InterruptedException {

        List<String> command =
                new ArrayList<>();

        command.add("git");
        command.add("-C");
        command.add(workingDirectory);
        command.addAll(arguments);

        Process process =
                new ProcessBuilder(command)
                        .redirectErrorStream(false)
                        .start();

        String output =
                new String(
                        process.getInputStream()
                                .readAllBytes(),
                        StandardCharsets.UTF_8
                );

        String error =
                new String(
                        process.getErrorStream()
                                .readAllBytes(),
                        StandardCharsets.UTF_8
                );

        int exitCode =
                process.waitFor();

        if (exitCode != 0) {

            throw new IllegalStateException(
                    "Git command failed:\n" +
                            error
            );
        }

        return output;
    }

    public static final class GitHeadResult {

        private final String commit;
        private final List<ApiEndpoint> endpoints;

        public GitHeadResult(
                String commit,
                List<ApiEndpoint> endpoints
        ) {
            this.commit = commit;
            this.endpoints = endpoints;
        }

        public String getCommit() {
            return commit;
        }

        public List<ApiEndpoint> getEndpoints() {
            return endpoints;
        }
    }
}