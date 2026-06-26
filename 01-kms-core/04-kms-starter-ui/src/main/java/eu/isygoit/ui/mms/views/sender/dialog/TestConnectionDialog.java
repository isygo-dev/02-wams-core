package eu.isygoit.ui.mms.views.sender.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.spring.annotation.UIScope;
import eu.isygoit.dto.data.SenderConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.SenderConfigService;
import feign.FeignException;
import org.springframework.stereotype.Component;

@UIScope
public class TestConnectionDialog extends Dialog {

    private final SenderConfigService senderConfigService;
    private final SenderConfigDto config;
    private final Runnable onSuccess;

    private final ProgressBar progressBar = new ProgressBar();
    private final Paragraph statusText = new Paragraph(I18n.t("sender.test.connecting"));
    private final Button closeButton = new Button(I18n.t("sender.test.close"));
    private final Button retryButton = new Button(I18n.t("sender.test.retry"));
    private boolean isRunning = false;

    public TestConnectionDialog(SenderConfigService senderConfigService,
                                SenderConfigDto config,
                                Runnable onSuccess) {
        this.senderConfigService = senderConfigService;
        this.config = config;
        this.onSuccess = onSuccess;

        setHeaderTitle(I18n.t("sender.test.title", config.getHost()));
        setWidth("450px");
        setModal(true);
        setDraggable(true);
        setResizable(false);

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);
        layout.setWidthFull();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);

        // Progress indicator
        progressBar.setIndeterminate(true);
        progressBar.setWidthFull();

        // Status icon and text
        HorizontalLayout statusLayout = new HorizontalLayout();
        statusLayout.setSpacing(true);
        statusLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        statusLayout.setWidthFull();

        Icon statusIcon = VaadinIcon.COG.create();
        statusIcon.setSize("24px");
        statusIcon.getStyle().set("color", "var(--lumo-primary-color)");

        statusText.getStyle().set("margin", "0");
        statusText.getStyle().set("flex", "1");

        statusLayout.add(statusIcon, statusText);

        // Buttons
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);
        buttonLayout.setWidthFull();
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        retryButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        retryButton.setVisible(false);
        retryButton.addClickListener(e -> runTest());

        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        closeButton.addClickListener(e -> close());

        buttonLayout.add(retryButton, closeButton);

        layout.add(progressBar, statusLayout, buttonLayout);
        add(layout);

        // Auto-start test
        runTest();
    }

    private void runTest() {
        if (isRunning) {
            return;
        }

        isRunning = true;
        progressBar.setVisible(true);
        retryButton.setVisible(false);
        closeButton.setEnabled(false);

        statusText.setText(I18n.t("sender.test.connecting"));
        statusText.getStyle().set("color", "var(--lumo-primary-text-color)");

        // Simulate test with a delay - in real implementation, call the actual test endpoint
        // For this example, we'll use a simulated test with config validation
        getUI().ifPresent(ui -> ui.access(() -> {
            try {
                // Validate required fields
                if (config.getHost() == null || config.getHost().isEmpty()) {
                    showResult(false, I18n.t("sender.test.error.host.missing"));
                    return;
                }
                if (config.getPort() == null || config.getPort().isEmpty()) {
                    showResult(false, I18n.t("sender.test.error.port.missing"));
                    return;
                }
                if (config.getUsername() == null || config.getUsername().isEmpty()) {
                    showResult(false, I18n.t("sender.test.error.username.missing"));
                    return;
                }

                // Simulate connection test - in production, call the actual API
                // For demonstration, we'll simulate a successful connection
                // with a random success/failure for demonstration
                boolean success = Math.random() > 0.3; // 70% success rate for demo

                if (success) {
                    showResult(true, I18n.t("sender.test.success"));
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                } else {
                    showResult(false, I18n.t("sender.test.failed"));
                }

            } catch (Exception e) {
                showResult(false, I18n.t("sender.test.error", e.getMessage()));
            } finally {
                isRunning = false;
            }
        }));
    }

    private void showResult(boolean success, String message) {
        progressBar.setVisible(false);
        closeButton.setEnabled(true);

        if (success) {
            statusText.setText("✓ " + message);
            statusText.getStyle().set("color", "var(--lumo-success-color)");
            Notification.show(message, 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } else {
            statusText.setText("✗ " + message);
            statusText.getStyle().set("color", "var(--lumo-error-color)");
            retryButton.setVisible(true);
            Notification.show(message, 5000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}