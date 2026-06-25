package eu.isygoit.ui.ims.views.application;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.ApplicationDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.ApplicationImageService;
import eu.isygoit.remote.ims.ApplicationService;
import eu.isygoit.ui.common.card.BaseCard;
import eu.isygoit.ui.ims.views.application.dialog.ApplicationDetailsDialog;
import eu.isygoit.ui.ims.views.application.dialog.DeleteApplicationDialog;
import eu.isygoit.ui.ims.views.application.dialog.ToggleApplicationStatusDialog;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

@Slf4j
public class ApplicationCard extends BaseCard<ApplicationManagementView, ApplicationService> {

    private final ApplicationDto application;
    private final ApplicationImageService applicationImageService;
    private final Runnable onRefresh;

    private Image appImage;
    private Span adminStatusChip;
    private Button toggleStatusBtn;

    public ApplicationCard(ApplicationManagementView parentView,
                           ApplicationService applicationService,
                           ApplicationImageService applicationImageService,
                           ApplicationDto application,
                           Runnable onRefresh) {
        super(parentView, applicationService);
        this.application = application;
        this.applicationImageService = applicationImageService;
        this.onRefresh = onRefresh;
        initCard();
    }

    @Override
    protected Component buildTitle() {
        return new Div();
    }

    @Override
    protected String cardCssClassName() {
        return "application-card";
    }

    @Override
    protected void buildHeader() {
        HorizontalLayout row1 = new HorizontalLayout();
        row1.setWidthFull();
        row1.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        row1.setAlignItems(FlexComponent.Alignment.CENTER);
        row1.setSpacing(true);
        row1.getStyle().set("flex-wrap", "wrap");

        appImage = new Image();
        appImage.setWidth("48px");
        appImage.setHeight("48px");
        appImage.getStyle()
                .set("border-radius", "50%")
                .set("object-fit", "cover")
                .set("background", "var(--lumo-contrast-10pct)")
                .set("border", "2px solid var(--lumo-contrast-20pct)");
        appImage.setSrc(getSvgPlaceholder());

        buttonBar = new HorizontalLayout();
        buttonBar.setSpacing(true);
        buttonBar.setPadding(false);
        buttonBar.getStyle().set("flex-wrap", "wrap");
        buttonBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonBar.addClassName(cardCssClassName() + "__button-bar");

        List<Button> buttons = buildActionButtons();
        buttons.forEach(buttonBar::add);

        row1.add(appImage, buttonBar);
        row1.expand(buttonBar);

        HorizontalLayout row2 = new HorizontalLayout();
        row2.setAlignItems(FlexComponent.Alignment.CENTER);
        row2.setSpacing(true);
        row2.getStyle().set("flex-wrap", "wrap");
        row2.getStyle().set("margin-top", "var(--lumo-space-s)");

        Span titleSpan = buildTitleSpan(application.getName(), application.getTitle());
        adminStatusChip = buildStatusChip(
                application.getAdminStatus() != null ? application.getAdminStatus().name() : I18n.t("app.card.status.unknown"),
                application.getAdminStatus() != null ? application.getAdminStatus().name() : I18n.t("app.card.status.unknown")
        );
        row2.add(titleSpan, adminStatusChip);

        add(row1, row2);
    }

    @Override
    protected List<Button> buildActionButtons() {
        Button detailsBtn = createIconButton(VaadinIcon.INFO_CIRCLE, I18n.t("app.card.details.tooltip"));
        detailsBtn.addClickListener(e -> new ApplicationDetailsDialog(parentView, objectService, application.getId()).open());

        Button editBtn = createIconButton(VaadinIcon.EDIT, I18n.t("app.card.edit.tooltip"));
        editBtn.addClickListener(e -> parentView.openUpdateApplicationDialog(application, () -> {
            if (onRefresh != null) onRefresh.run();
        }));

        toggleStatusBtn = createIconButton(
                application.getAdminStatus() == IEnumEnabledBinaryStatus.Types.ENABLED ? VaadinIcon.LOCK : VaadinIcon.UNLOCK,
                application.getAdminStatus() == IEnumEnabledBinaryStatus.Types.ENABLED ? I18n.t("app.card.disable.tooltip") : I18n.t("app.card.enable.tooltip")
        );
        toggleStatusBtn.addClickListener(e -> openToggleStatusDialog());

        Button deleteBtn = createIconButton(VaadinIcon.TRASH, I18n.t("app.card.delete.tooltip"));
        deleteBtn.addClickListener(e -> new DeleteApplicationDialog(parentView, objectService, application.getId(), () -> {
            if (onRefresh != null) onRefresh.run();
        }).open());

        return List.of(detailsBtn, editBtn, toggleStatusBtn, deleteBtn);
    }

    @Override
    protected void buildBodyRows() {
        VerticalLayout body = new VerticalLayout();
        body.setSpacing(false);
        body.setPadding(false);
        body.getStyle().set("margin-top", "var(--lumo-space-s)");

        body.add(createIconRow(VaadinIcon.DESKTOP, I18n.t("app.card.category"), application.getCategory()));
        body.add(createIconRow(VaadinIcon.GLOBE, I18n.t("app.card.url"), application.getUrl()));
        if (application.getOrder() != null) {
            body.add(createIconRow(VaadinIcon.SORT, I18n.t("app.card.order"), String.valueOf(application.getOrder())));
        }
        if (application.getDescription() != null && !application.getDescription().isBlank()) {
            body.add(createIconRow(VaadinIcon.FILE_TEXT, I18n.t("app.card.description"), application.getDescription()));
        }
        add(body);
    }

    private HorizontalLayout createIconRow(VaadinIcon icon, String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.addClassName("meta-row");

        com.vaadin.flow.component.icon.Icon iconComponent = icon.create();
        iconComponent.setSize("14px");
        iconComponent.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.XXSMALL);
        labelSpan.getStyle().set("min-width", "80px");

        Span valueSpan = new Span(value != null ? value : "—");
        valueSpan.addClassName(LumoUtility.FontSize.XXSMALL);
        valueSpan.getStyle().set("word-break", "break-all");
        valueSpan.getStyle().set("flex", "1");

        row.add(iconComponent, labelSpan, valueSpan);
        row.expand(valueSpan);
        return row;
    }

    private void openToggleStatusDialog() {
        new ToggleApplicationStatusDialog(parentView, objectService, application.getId(), application.getAdminStatus(), () -> {
            refreshApplicationData();
            if (onRefresh != null) onRefresh.run();
        }).open();
    }

    private void refreshApplicationData() {
        try {
            ResponseEntity<ApplicationDto> response = objectService.findById(application.getId());
            if (response.getBody() != null) {
                application.setAdminStatus(response.getBody().getAdminStatus());
                updateStatusChip();
                updateToggleButtonIcon();
            }
        } catch (Exception e) {
            log.error("Failed to refresh application data", e);
        }
    }

    private void updateStatusChip() {
        if (adminStatusChip != null) {
            String status = application.getAdminStatus() != null ? application.getAdminStatus().name() : I18n.t("app.card.status.unknown");
            adminStatusChip.setText(status);
            adminStatusChip.getElement().setAttribute("title", status);
            ChipColor color = ChipColor.fromStatus(status);
            adminStatusChip.getStyle()
                    .set("background-color", color.background())
                    .set("color", color.foreground());
        }
    }

    private void updateToggleButtonIcon() {
        if (toggleStatusBtn != null) {
            boolean enabled = application.getAdminStatus() == IEnumEnabledBinaryStatus.Types.ENABLED;
            toggleStatusBtn.setIcon(enabled ? VaadinIcon.LOCK.create() : VaadinIcon.UNLOCK.create());
            toggleStatusBtn.setTooltipText(enabled ? I18n.t("app.card.disable.tooltip") : I18n.t("app.card.enable.tooltip"));
        }
    }

    @Override
    protected void onCardAttach(AttachEvent event) {
        updateStatusChip();
        loadApplicationImage();
    }

    /**
     * Loads the application image synchronously on the UI thread.
     * This method is called from onCardAttach and uses getUI().ifPresent(ui -> ui.access(() -> { ... }))
     * to ensure the UI update is performed on the correct thread.
     */
    private void loadApplicationImage() {
        getUI().ifPresent(ui -> ui.access(() -> {
            try {
                ResponseEntity<Resource> response = applicationImageService.downloadImage(application.getId());
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    byte[] imageBytes = response.getBody().getContentAsByteArray();
                    if (imageBytes != null && imageBytes.length > 0) {
                        StreamResource resource = new StreamResource("app_" + application.getId() + ".jpg",
                                () -> new ByteArrayInputStream(imageBytes));
                        appImage.setSrc(resource);
                        return;
                    }
                }
            } catch (FeignException ex) {
                if (ex.status() == 404) {
                    log.debug("No image found for application {}", application.getId());
                } else {
                    log.warn("Failed to load image for application {}: {}", application.getId(), ex.getMessage());
                }
            } catch (IOException e) {
                log.error("Error reading image stream for application {}", application.getId(), e);
            }
            // Fallback to placeholder
            appImage.setSrc(getSvgPlaceholder());
        }));
    }

    private String getSvgPlaceholder() {
        return "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='48' height='48' viewBox='0 0 24 24' fill='none' stroke='%23888' stroke-width='1' stroke-linecap='round' stroke-linejoin='round'%3E%3Crect x='2' y='2' width='20' height='20' rx='2.18' ry='2.18'%3E%3C/rect%3E%3Cpath d='M7 2v20M17 2v20M2 12h20M2 7h5M2 17h5M17 17h5M17 7h5'%3E%3C/path%3E%3C/svg%3E";
    }

    @Override
    protected String buildExtraStyles() {
        return """
                .application-card {
                    padding: var(--lumo-space-s) var(--lumo-space-m);
                }
                .application-card .meta-row {
                    border-bottom: 1px solid var(--lumo-contrast-10pct);
                    padding-bottom: var(--lumo-space-xs);
                    margin-bottom: var(--lumo-space-xs);
                }
                .application-card .meta-row:last-child {
                    border-bottom: none;
                    margin-bottom: 0;
                }
                @media (max-width: 640px) {
                    .application-card .meta-row {
                        flex-wrap: wrap;
                    }
                    .application-card .meta-row > :not(:first-child) {
                        margin-left: 28px;
                    }
                    .application-card .application-card__button-bar {
                        width: 100%;
                        justify-content: flex-start;
                    }
                }
                """;
    }
}