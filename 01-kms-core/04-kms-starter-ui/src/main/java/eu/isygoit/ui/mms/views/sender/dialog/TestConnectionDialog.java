package eu.isygoit.ui.mms.views.sender.dialog;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.SenderConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.SenderConfigService;
import eu.isygoit.ui.common.dialog.NoActionDialog;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestConnectionDialog extends NoActionDialog {

    private final SenderConfigService senderConfigService;
    private final SenderConfigDto config;
    private final Runnable onComplete;

    private final Div statusArea = new Div();
    private final ProgressBar progressBar = new ProgressBar();

    public TestConnectionDialog(SenderConfigService senderConfigService,
                                SenderConfigDto config,
                                Runnable onComplete) {
        super(I18n.t("mms.sender.dialog.test.title", config.getHost()));
        this.senderConfigService = senderConfigService;
        this.config = config;
        this.onComplete = onComplete;

        setWidth("500px");
        setMaxWidth("95vw");
        setModal(true);
        setDraggable(true);
        // Prevent dismissing the dialog while the test is still running –
        // re-enabled once a result (success/failure) is available.
        setCloseOnEsc(false);
        setCloseOnOutsideClick(false);

        addOpenedChangeListener(e -> {
            if (!e.isOpened() && onComplete != null) {
                onComplete.run();
            }
        });

        buildLayout();
        runTest();
    }

    private void buildLayout() {
        // Host info
        HorizontalLayout infoRow = new HorizontalLayout();
        infoRow.setAlignItems(FlexComponent.Alignment.CENTER);
        infoRow.setSpacing(true);

        Icon serverIcon = VaadinIcon.SERVER.create();
        serverIcon.setColor("var(--lumo-primary-color)");
        Span hostLabel = new Span(I18n.t("mms.sender.dialog.test.host") + ":");
        hostLabel.addClassName(LumoUtility.FontWeight.BOLD);
        Span hostValue = new Span(config.getHost() + ":" + config.getPort());
        hostValue.addClassName("wams-dialog-host-value");

        infoRow.add(serverIcon, hostLabel, hostValue);
        add(infoRow);

        // Progress bar
        progressBar.setIndeterminate(true);
        progressBar.setWidthFull();
        add(progressBar);

        // Status area
        statusArea.addClassName("wams-dialog-status-box");
        add(statusArea);
    }

    private void runTest() {
        // Simulate async test - in real implementation, call actual test endpoint
        // For now, simulate a successful connection test
        statusArea.removeAll();
        statusArea.removeClassName("wams-dialog-status-box--success");
        statusArea.removeClassName("wams-dialog-status-box--error");
        statusArea.addClassName("wams-dialog-status-box--info");

        VerticalLayout statusContent = new VerticalLayout();
        statusContent.setAlignItems(FlexComponent.Alignment.CENTER);
        statusContent.setSpacing(true);

        Icon spinner = VaadinIcon.SPINNER.create();
        spinner.addClassName("wams-dialog-spinner");
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
        setCloseOnEsc(true);
        setCloseOnOutsideClick(true);

        statusArea.removeAll();
        statusArea.removeClassName("wams-dialog-status-box--info");
        if (success) {
            statusArea.addClassName("wams-dialog-status-box--success");

            VerticalLayout resultContent = new VerticalLayout();
            resultContent.setAlignItems(FlexComponent.Alignment.CENTER);
            resultContent.setSpacing(true);
            resultContent.addClassName("wams-dialog-status-content");

            Icon successIcon = VaadinIcon.CHECK_CIRCLE.create();
            successIcon.setColor("var(--lumo-success-color)");
            successIcon.setSize("48px");

            resultContent.add(successIcon);
            resultContent.add(new Span(message));
            resultContent.add(new Span(I18n.t("mms.sender.dialog.test.connection.established")));

            statusArea.add(resultContent);
        } else {
            statusArea.addClassName("wams-dialog-status-box--error");

            VerticalLayout resultContent = new VerticalLayout();
            resultContent.setAlignItems(FlexComponent.Alignment.CENTER);
            resultContent.setSpacing(true);
            resultContent.addClassName("wams-dialog-status-content");

            Icon errorIcon = VaadinIcon.EXCLAMATION_CIRCLE.create();
            errorIcon.setColor("var(--lumo-error-color)");
            errorIcon.setSize("48px");

            resultContent.add(errorIcon);
            resultContent.add(new Span(message));
            resultContent.add(new Span(I18n.t("mms.sender.dialog.test.check.configuration")));

            statusArea.add(resultContent);
        }
    }
}