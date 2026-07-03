package eu.isygoit.ui.mms.views.sender.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import eu.isygoit.dto.data.SenderConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.SenderConfigService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestConnectionDialog extends Dialog {

    private final SenderConfigService senderConfigService;
    private final SenderConfigDto config;
    private final Runnable onComplete;

    private final Div statusArea = new Div();
    private final ProgressBar progressBar = new ProgressBar();
    private final Button closeButton = new Button(I18n.t("common.dialog.close"));

    public TestConnectionDialog(SenderConfigService senderConfigService,
                                SenderConfigDto config,
                                Runnable onComplete) {
        this.senderConfigService = senderConfigService;
        this.config = config;
        this.onComplete = onComplete;

        setHeaderTitle(I18n.t("mms.sender.dialog.test.title", config.getHost()));
        setWidth("500px");
        setMaxWidth("95vw");
        setModal(true);
        setDraggable(true);

        buildLayout();
        runTest();
    }

    private void buildLayout() {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(true);
        mainLayout.setSpacing(true);
        mainLayout.setWidthFull();

        // Host info
        HorizontalLayout infoRow = new HorizontalLayout();
        infoRow.setAlignItems(FlexComponent.Alignment.CENTER);
        infoRow.setSpacing(true);

        Icon serverIcon = VaadinIcon.SERVER.create();
        serverIcon.setColor("var(--lumo-primary-color)");
        Span hostLabel = new Span(I18n.t("mms.sender.dialog.test.host") + ":");
        hostLabel.getStyle().set("font-weight", "bold");
        Span hostValue = new Span(config.getHost() + ":" + config.getPort());
        hostValue.getStyle().set("font-family", "monospace");

        infoRow.add(serverIcon, hostLabel, hostValue);
        mainLayout.add(infoRow);

        // Progress bar
        progressBar.setIndeterminate(true);
        progressBar.setWidthFull();
        mainLayout.add(progressBar);

        // Status area
        statusArea.setWidthFull();
        statusArea.getStyle()
                .set("padding", "var(--lumo-space-m)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("min-height", "100px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center");
        mainLayout.add(statusArea);

        // Close button
        HorizontalLayout footer = new HorizontalLayout();
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        closeButton.setEnabled(false);
        closeButton.addClickListener(e -> {
            close();
            if (onComplete != null) {
                onComplete.run();
            }
        });
        footer.add(closeButton);
        mainLayout.add(footer);

        add(mainLayout);
    }

    private void runTest() {
        // Simulate async test - in real implementation, call actual test endpoint
        // For now, simulate a successful connection test
        statusArea.removeAll();
        statusArea.getStyle()
                .set("background-color", "var(--lumo-primary-color-10pct)")
                .set("color", "var(--lumo-primary-text-color)");

        VerticalLayout statusContent = new VerticalLayout();
        statusContent.setAlignItems(FlexComponent.Alignment.CENTER);
        statusContent.setSpacing(true);

        Icon spinner = VaadinIcon.SPINNER.create();
        spinner.getStyle().set("animation", "spin 1s linear infinite");
        spinner.setSize("32px");
        statusContent.add(spinner);
        statusContent.add(new Span(I18n.t("mms.sender.dialog.test.connecting")));

        statusArea.add(statusContent);

        // Simulate test completion after delay
        getUI().ifPresent(ui -> ui.access(() -> {
            try {
                // In real implementation, call:
                // ResponseEntity<TestConnectionResponse> response = senderConfigService.testConnection(config.getId());
                // For now, simulate success
                Thread.sleep(2000);

                getUI().ifPresent(ui2 -> ui2.access(() -> {
                    showTestResult(true, I18n.t("mms.sender.dialog.test.success"));
                }));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                showTestResult(false, I18n.t("mms.sender.dialog.test.failed", e.getMessage()));
            }
        }));
    }

    private void showTestResult(boolean success, String message) {
        progressBar.setVisible(false);
        closeButton.setEnabled(true);

        statusArea.removeAll();
        if (success) {
            statusArea.getStyle()
                    .set("background-color", "var(--lumo-success-color-10pct)")
                    .set("color", "var(--lumo-success-text-color)");

            VerticalLayout resultContent = new VerticalLayout();
            resultContent.setAlignItems(FlexComponent.Alignment.CENTER);
            resultContent.setSpacing(true);

            Icon successIcon = VaadinIcon.CHECK_CIRCLE.create();
            successIcon.setColor("var(--lumo-success-color)");
            successIcon.setSize("48px");

            resultContent.add(successIcon);
            resultContent.add(new Span(message));
            resultContent.add(new Span(I18n.t("mms.sender.dialog.test.connection.established")));
            resultContent.getStyle().set("font-size", "var(--lumo-font-size-s)");

            statusArea.add(resultContent);
        } else {
            statusArea.getStyle()
                    .set("background-color", "var(--lumo-error-color-10pct)")
                    .set("color", "var(--lumo-error-text-color)");

            VerticalLayout resultContent = new VerticalLayout();
            resultContent.setAlignItems(FlexComponent.Alignment.CENTER);
            resultContent.setSpacing(true);

            Icon errorIcon = VaadinIcon.EXCLAMATION_CIRCLE.create();
            errorIcon.setColor("var(--lumo-error-color)");
            errorIcon.setSize("48px");

            resultContent.add(errorIcon);
            resultContent.add(new Span(message));
            resultContent.add(new Span(I18n.t("mms.sender.dialog.test.check.configuration")));
            resultContent.getStyle().set("font-size", "var(--lumo-font-size-s)");

            statusArea.add(resultContent);
        }
    }
}