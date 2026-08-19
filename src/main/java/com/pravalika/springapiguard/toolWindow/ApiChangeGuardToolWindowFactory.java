package com.pravalika.springapiguard.toolWindow;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.ContentFactory;
import com.pravalika.springapiguard.analyzer.ApiChange;
import com.pravalika.springapiguard.analyzer.ApiEndpoint;
import com.pravalika.springapiguard.analyzer.ApiChangeAnalyzer;
import com.pravalika.springapiguard.analyzer.GitHeadSpringApiScanner;
import com.pravalika.springapiguard.analyzer.SpringApiScanner;

import javax.swing.JEditorPane;
import javax.swing.JButton;
import javax.swing.event.HyperlinkEvent;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

public final class ApiChangeGuardToolWindowFactory
        implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(
            Project project,
            ToolWindow toolWindow
    ) {

        SpringRuntimeAnalyzerPanel panel =
                new SpringRuntimeAnalyzerPanel(
                        project
                );

        ContentFactory contentFactory =
                ContentFactory.getInstance();

        toolWindow.getContentManager()
                .addContent(
                        contentFactory.createContent(
                                panel.getPanel(),
                                null,
                                false
                        )
                );
    }

    @Override
    public boolean shouldBeAvailable(
            Project project
    ) {
        return true;
    }
}

final class SpringRuntimeAnalyzerPanel {

    private final Project project;

    private final JBPanel<JBPanel<?>> panel =
            new JBPanel<>(
                    new BorderLayout()
            );

    private final JEditorPane resultPane =
            new JEditorPane();

    private final JButton checkButton =
            new JButton(
                    "Check Git HEAD"
            );

    private final SpringApiScanner scanner;

    private final GitHeadSpringApiScanner gitScanner;

    private final ApiChangeAnalyzer changeAnalyzer =
            new ApiChangeAnalyzer();

    SpringRuntimeAnalyzerPanel(
            Project project
    ) {

        this.project = project;

        this.scanner =
                new SpringApiScanner(
                        project
                );

        this.gitScanner =
                new GitHeadSpringApiScanner(
                        project,
                        scanner
                );

        createUi();
    }

    JBPanel<JBPanel<?>> getPanel() {
        return panel;
    }

    private void createUi() {

        JBPanel<JBPanel<?>> header =
                new JBPanel<>(
                        new BorderLayout()
                );

        JBLabel title =
                new JBLabel(
                        "SPRING API CHANGE GUARD"
                );

        JBPanel<JBPanel<?>> buttons =
                new JBPanel<>(
                        new FlowLayout(
                                FlowLayout.LEFT
                        )
                );

        buttons.add(checkButton);

        header.add(
                title,
                BorderLayout.NORTH
        );

        header.add(
                buttons,
                BorderLayout.CENTER
        );

        configureResultPane();

        resultPane.setText(
                html(
                        """
                        <h2>Spring API Change Guard</h2>
                        <p><b>Project:</b> %s</p>
                        <p>Compare the current Spring REST API against Git HEAD.</p>
                        """.formatted(
                                escapeHtml(
                                        project.getName()
                                )
                        )
                )
        );

        checkButton.addActionListener(
                event -> checkChanges()
        );

        panel.add(
                header,
                BorderLayout.NORTH
        );

        panel.add(
                new JBScrollPane(
                        resultPane
                ),
                BorderLayout.CENTER
        );
    }

    private void configureResultPane() {

        resultPane.setContentType(
                "text/html"
        );

        resultPane.setEditable(
                false
        );

        resultPane.setOpaque(
                false
        );

        resultPane.addHyperlinkListener(
                event -> {

                    if (
                            event.getEventType()
                                    != HyperlinkEvent.EventType.ACTIVATED
                    ) {
                        return;
                    }

                    String description =
                            event.getDescription();

                    if (description == null) {
                        return;
                    }

                    navigateToEndpoint(
                            description
                    );
                }
        );
    }

    private void checkChanges() {

        checkButton.setEnabled(
                false
        );

        resultPane.setText(
                html(
                        """
                        <h2>Spring API Change Guard</h2>
                        <p>Reading Git HEAD...</p>
                        """
                )
        );

        new Task.Backgroundable(
                project,
                "Checking Spring API Changes",
                false
        ) {

            private String error;

            private String commit;

            private List<ApiEndpoint> baseline;

            private List<ApiEndpoint> current;

            private List<ApiChange> changes;

            @Override
            public void run(
                    ProgressIndicator indicator
            ) {

                try {

                    indicator.setText(
                            "Reading Git HEAD..."
                    );

                    GitHeadSpringApiScanner.GitHeadResult
                            headResult =
                            gitScanner.scanHead();

                    commit =
                            headResult.getCommit();

                    baseline =
                            headResult.getEndpoints();

                    indicator.setText(
                            "Analyzing current APIs..."
                    );

                    current =
                            ApplicationManager
                                    .getApplication()
                                    .runReadAction(
                                            (Computable<List<ApiEndpoint>>)
                                                    scanner::scanProject
                                    );

                    indicator.setText(
                            "Comparing API contracts..."
                    );

                    changes =
                            changeAnalyzer.compare(
                                    baseline,
                                    current
                            );

                } catch (Exception exception) {

                    error =
                            exception.getMessage();
                }
            }

            @Override
            public void onSuccess() {

                if (error != null) {

                    showError(
                            error
                    );

                } else {

                    resultPane.setText(
                            buildResult(
                                    commit,
                                    baseline,
                                    current,
                                    changes
                            )
                    );
                }

                checkButton.setEnabled(
                        true
                );
            }

            @Override
            public void onThrowable(
                    Throwable throwable
            ) {

                showError(
                        throwable.getMessage()
                );

                checkButton.setEnabled(
                        true
                );
            }
        }.queue();
    }

    private void showError(
            String message
    ) {

        resultPane.setText(
                html(
                        """
                        <h2>Spring API Change Guard</h2>
                        <p><b>Analysis failed.</b></p>
                        <p>%s</p>
                        """.formatted(
                                escapeHtml(
                                        message == null
                                                ? "Unknown error"
                                                : message
                                )
                        )
                )
        );
    }

    private String buildResult(
            String commit,
            List<ApiEndpoint> baseline,
            List<ApiEndpoint> current,
            List<ApiChange> changes
    ) {

        StringBuilder html =
                new StringBuilder();

        html.append(
                "<html><body style='font-family:sans-serif;'>"
        );

        html.append(
                "<h2>Spring API Change Guard</h2>"
        );

        html.append(
                "<p><b>Compared against Git HEAD:</b> "
        );

        html.append(
                escapeHtml(
                        commit.substring(
                                0,
                                Math.min(
                                        12,
                                        commit.length()
                                )
                        )
                )
        );

        html.append(
                "</p>"
        );

        html.append(
                "<p>"
        );

        html.append(
                "<b>Baseline APIs:</b> "
        );

        html.append(
                baseline.size()
        );

        html.append(
                "&nbsp;&nbsp;&nbsp;"
        );

        html.append(
                "<b>Current APIs:</b> "
        );

        html.append(
                current.size()
        );

        html.append(
                "</p>"
        );

        if (changes.isEmpty()) {

            html.append(
                    "<p style='color:#22863a;'>" +
                            "<b>✓ No API changes detected.</b>" +
                            "</p>"
            );

            html.append(
                    "</body></html>"
            );

            return html.toString();
        }

        long breaking =
                changes.stream()
                        .filter(
                                ApiChange::isBreaking
                        )
                        .count();

        long other =
                changes.size() -
                        breaking;

        html.append(
                "<p><b>Changes:</b> "
        );

        html.append(
                changes.size()
        );

        html.append(
                "&nbsp;&nbsp;&nbsp;"
        );

        html.append(
                "<b>Breaking:</b> "
        );

        html.append(
                breaking
        );

        html.append(
                "&nbsp;&nbsp;&nbsp;"
        );

        html.append(
                "<b>Other:</b> "
        );

        html.append(
                other
        );

        html.append(
                "</p>"
        );

        html.append(
                "<hr>"
        );

        for (ApiChange change :
                changes) {

            appendChange(
                    html,
                    change
            );
        }

        html.append(
                "</body></html>"
        );

        return html.toString();
    }

    private void appendChange(
            StringBuilder html,
            ApiChange change
    ) {

        ApiEndpoint endpoint =
                change.getAfter() != null
                        ? change.getAfter()
                        : change.getBefore();

        String link =
                endpoint != null
                        ? buildEndpointLink(
                        endpoint
                )
                        : "#";

        String endpointText =
                endpoint == null
                        ? "Unknown endpoint"
                        : formatEndpoint(
                        endpoint
                );

        String heading;

        if (
                change.getType() ==
                        ApiChange.Type.ADDED
        ) {

            heading =
                    "🟢 ADDED";

        } else if (
                change.isBreaking()
        ) {

            heading =
                    "🔴 BREAKING";

        } else {

            heading =
                    "🟡 CONTRACT CHANGE";
        }

        html.append(
                "<div style='margin-bottom:18px;'>"
        );

        html.append(
                "<b>"
        );

        html.append(
                heading
        );

        html.append(
                "</b><br>"
        );

        html.append(
                "<a href='"
        );

        html.append(
                escapeHtml(
                        link
                )
        );

        html.append(
                "'>"
        );

        html.append(
                escapeHtml(
                        endpointText
                )
        );

        html.append(
                "</a><br>"
        );

        if (
                change.getType() ==
                        ApiChange.Type.PATH_CHANGED
                        ||
                        change.getType() ==
                                ApiChange.Type.METHOD_CHANGED
        ) {

            ApiEndpoint before =
                    change.getBefore();

            ApiEndpoint after =
                    change.getAfter();

            html.append(
                    "<span>"
            );

            html.append(
                    escapeHtml(
                            formatEndpoint(
                                    before
                            )
                    )
            );

            html.append(
                    " &rarr; "
            );

            html.append(
                    escapeHtml(
                            formatEndpoint(
                                    after
                            )
                    )
            );

            html.append(
                    "</span><br>"
            );
        }

        html.append(
                escapeHtml(
                        change.getMessage()
                )
        );

        html.append(
                "</div>"
        );
    }

    private String buildEndpointLink(
            ApiEndpoint endpoint
    ) {

        return "endpoint:"
                + endpoint.getController()
                + "|"
                + endpoint.getMethod();
    }

    private void navigateToEndpoint(
            String link
    ) {

        if (!link.startsWith(
                "endpoint:"
        )) {
            return;
        }

        String value =
                link.substring(
                        "endpoint:".length()
                );

        int separator =
                value.lastIndexOf('|');

        if (separator <= 0) {
            return;
        }

        String controller =
                value.substring(
                        0,
                        separator
                );

        String method =
                value.substring(
                        separator + 1
                );

        NavigationRequest request =
                new NavigationRequest(
                        controller,
                        method
                );

        ApplicationManager
                .getApplication()
                .executeOnPooledThread(
                        () -> {

                            SpringApiScanner
                                    .NavigationTarget target =
                                    ApplicationManager
                                            .getApplication()
                                            .runReadAction(
                                                    (Computable<
                                                            SpringApiScanner.NavigationTarget
                                                            >)
                                                            () ->
                                                                    scanner.findNavigationTarget(
                                                                            request.controller,
                                                                            request.method
                                                                    )
                                            );

                            if (target == null) {
                                return;
                            }

                            ApplicationManager
                                    .getApplication()
                                    .invokeLater(
                                            () -> {

                                                new OpenFileDescriptor(
                                                        project,
                                                        target.getFile(),
                                                        target.getOffset()
                                                ).navigate(true);
                                            },
                                            ModalityState.any()
                                    );
                        }
                );
    }

    private String formatEndpoint(
            ApiEndpoint endpoint
    ) {

        return endpoint.getHttpMethod()
                + " "
                + endpoint.getPath();
    }

    private String escapeHtml(
            String text
    ) {

        if (text == null) {
            return "";
        }

        return text
                .replace(
                        "&",
                        "&amp;"
                )
                .replace(
                        "<",
                        "&lt;"
                )
                .replace(
                        ">",
                        "&gt;"
                )
                .replace(
                        "\"",
                        "&quot;"
                );
    }

    private String html(
            String body
    ) {

        return """
                <html>
                <body style="font-family:sans-serif;">
                %s
                </body>
                </html>
                """.formatted(
                body
        );
    }

    private static final class NavigationRequest {

        private final String controller;
        private final String method;

        private NavigationRequest(
                String controller,
                String method
        ) {
            this.controller = controller;
            this.method = method;
        }
    }
}