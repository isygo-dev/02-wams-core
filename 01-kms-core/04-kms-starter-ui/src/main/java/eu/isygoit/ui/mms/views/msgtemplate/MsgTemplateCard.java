package eu.isygoit.ui.mms.views.msgtemplate;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.MsgTemplateDto;
import eu.isygoit.enums.IEnumLanguage;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.MsgTemplateService;
import eu.isygoit.ui.common.card.BaseCard;
import eu.isygoit.ui.mms.views.msgtemplate.dialog.EditTemplateDialog;
import eu.isygoit.ui.mms.views.msgtemplate.dialog.ViewTemplateDialog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Card component for displaying and managing a Message Template.
 */
@Slf4j
public class MsgTemplateCard extends BaseCard<MsgTemplateManagementView, MsgTemplateService> {

    private final MsgTemplateDto template;
    private final Runnable onRefresh;

    // UI Components
    private Span titleSpan;
    private Span statusChip;
    private Span languageBadge;
    private Span nameSpan;
    private Span codeSpan;
    private Span descriptionSpan;
    private Span fileSpan;
    private Button viewButton;
    private Button editButton;
    private Button deleteButton;

    private final VerticalLayout bodyContainer = new VerticalLayout();

    public MsgTemplateCard(MsgTemplateManagementView parentView,
                           MsgTemplateService templateService,
                           MsgTemplateDto template,
                           Runnable onRefresh) {
        super(parentView, templateService);
        this.template = template;
        this.onRefresh = onRefresh;

        // Setup body container
        bodyContainer.setPadding(false);
        bodyContainer.setSpacing(true);
        bodyContainer.setWidthFull();
        bodyContainer.addClassName("template-card-body");
        add(bodyContainer);

        initCard();
    }

    // ─── Public Accessors ─────────────────────────────────────────────────────

    public MsgTemplateDto getTemplate() {
        return template;
    }

    public Long getTemplateId() {
        return template.getId();
    }

    public String getName() {
        return template.getName();
    }

    public String getCode() {
        return template.getCode();
    }

    public boolean hasFile() {
        return template.getFileName() != null && !template.getFileName().isEmpty();
    }

    // ─── Refresh ──────────────────────────────────────────────────────────────

    public void refresh() {
        getUI().ifPresent(ui -> ui.access(() -> {
            try {
                ResponseEntity<MsgTemplateDto> response = objectService.findById(template.getId());
                if (response.getBody() != null) {
                    MsgTemplateDto updatedTemplate = response.getBody();
                    // Update fields
                    template.setName(updatedTemplate.getName());
                    template.setCode(updatedTemplate.getCode());
                    template.setDescription(updatedTemplate.getDescription());
                    template.setLanguage(updatedTemplate.getLanguage());
                    template.setFileName(updatedTemplate.getFileName());
                    template.setOriginalFileName(updatedTemplate.getOriginalFileName());
                    template.setPath(updatedTemplate.getPath());

                    // Update UI
                    updateDisplay();
                }
            } catch (Exception e) {
                log.error("Failed to refresh template card for {}", template.getId(), e);
            }
        }));
    }

    private void updateDisplay() {
        // Update title
        String displayName = template.getName() != null ? template.getName() : "Template " + template.getId();
        titleSpan.setText(displayName);
        titleSpan.getElement().setAttribute("title", displayName);

        // Update name
        nameSpan.setText(template.getName() != null ? template.getName() : "N/A");

        // Update code
        codeSpan.setText(template.getCode() != null ? template.getCode() : "N/A");

        // Update description
        descriptionSpan.setText(template.getDescription() != null ? template.getDescription() : I18n.t("template.card.no.description"));

        // Update file indicator
        if (hasFile()) {
            fileSpan.setText("✓ " + template.getFileName());
            fileSpan.getStyle().set("color", "var(--lumo-success-color)");
        } else {
            fileSpan.setText("✗ " + I18n.t("template.card.no.file"));
            fileSpan.getStyle().set("color", "var(--lumo-error-color)");
        }

        // Update language badge
        String lang = template.getLanguage() != null ? template.getLanguage().name() : "EN";
        languageBadge.setText(lang);
    }

    // ─── BaseCard Implementation ─────────────────────────────────────────────

    @Override
    protected String cardCssClassName() {
        return "template-card";
    }

    @Override
    protected Component buildTitle() {
        HorizontalLayout left = new HorizontalLayout();
        left.setAlignItems(Alignment.CENTER);
        left.setSpacing(true);
        left.getStyle().set("flex-wrap", "wrap");
        left.getStyle().set("gap", "var(--lumo-space-xs)");

        // Tenant badge
        if (template.getTenant() != null) {
            Span tenantBadge = new Span(template.getTenant());
            tenantBadge.addClassName(LumoUtility.Background.CONTRAST_5);
            tenantBadge.addClassName(LumoUtility.Padding.XSMALL);
            tenantBadge.addClassName(LumoUtility.BorderRadius.SMALL);
            tenantBadge.addClassName(LumoUtility.FontSize.XSMALL);
            tenantBadge.getStyle().set("font-family", "monospace");
            left.add(tenantBadge);
        }

        // Title
        String displayName = template.getName() != null ? template.getName() : "Template " + template.getId();
        titleSpan = buildTitleSpan(displayName, displayName);
        left.add(titleSpan);

        // Language badge
        String lang = template.getLanguage() != null ? template.getLanguage().name() : "EN";
        languageBadge = new Span(lang);
        languageBadge.addClassName(LumoUtility.Background.PRIMARY_10);
        languageBadge.addClassName(LumoUtility.TextColor.PRIMARY);
        languageBadge.addClassName(LumoUtility.Padding.XSMALL);
        languageBadge.addClassName(LumoUtility.BorderRadius.SMALL);
        languageBadge.addClassName(LumoUtility.FontSize.XSMALL);
        languageBadge.getStyle().set("font-weight", "600");
        left.add(languageBadge);

        // Has file indicator
        if (hasFile()) {
            Icon fileIcon = VaadinIcon.FILE.create();
            fileIcon.setSize("14px");
            fileIcon.getStyle().set("color", "var(--lumo-success-color)");
            left.add(fileIcon);
        }

        // ID badge
        Span idBadge = new Span("ID: " + template.getId());
        idBadge.addClassName(LumoUtility.FontSize.XSMALL);
        idBadge.addClassName(LumoUtility.TextColor.TERTIARY);
        idBadge.getStyle().set("font-family", "monospace");
        left.add(idBadge);

        return left;
    }

    @Override
    protected List<Button> buildActionButtons() {
        List<Button> buttons = new ArrayList<>();

        // View Button
        viewButton = createIconButton(VaadinIcon.EYE, I18n.t("template.card.view.tooltip"));
        viewButton.addClickListener(e -> openViewDialog());
        buttons.add(viewButton);

        // Edit Button
        editButton = createIconButton(VaadinIcon.EDIT, I18n.t("template.card.edit.tooltip"));
        editButton.addClickListener(e -> openEditDialog());
        buttons.add(editButton);

        // Delete Button
        deleteButton = createIconButton(VaadinIcon.TRASH, I18n.t("template.card.delete.tooltip"));
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteButton.addClickListener(e -> confirmDelete());
        buttons.add(deleteButton);

        return buttons;
    }

    @Override
    protected void buildBodyRows() {
        bodyContainer.removeAll();

        // Name
        nameSpan = new Span();
        bodyContainer.add(createDetailRow(
                VaadinIcon.FILE_TEXT,
                I18n.t("template.card.name"),
                template.getName() != null ? template.getName() : "N/A"
        ));

        // Code with copy
        bodyContainer.add(createDetailRowWithCopy(
                VaadinIcon.CODE,
                I18n.t("template.card.code"),
                template.getCode() != null ? template.getCode() : "N/A",
                template.getCode() != null ? template.getCode() : ""
        ));

        // Description
        descriptionSpan = new Span();
        bodyContainer.add(createDetailRow(
                VaadinIcon.EDIT,
                I18n.t("template.card.description"),
                template.getDescription() != null ? template.getDescription() : I18n.t("template.card.no.description")
        ));

        // Language
        bodyContainer.add(createDetailRow(
                VaadinIcon.GLOBE,
                I18n.t("template.card.language"),
                template.getLanguage() != null ? template.getLanguage().name() : "EN"
        ));

        // File
        fileSpan = new Span();
        bodyContainer.add(createDetailRow(
                VaadinIcon.FILE,
                I18n.t("template.card.file"),
                hasFile() ? template.getFileName() : I18n.t("template.card.no.file")
        ));

        // Tenant
        bodyContainer.add(createDetailRow(
                VaadinIcon.GROUP,
                I18n.t("template.card.tenant"),
                template.getTenant() != null ? template.getTenant() : "N/A"
        ));
    }

    // ─── Helper Methods ──────────────────────────────────────────────────────

    private HorizontalLayout createDetailRow(VaadinIcon icon, String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.getStyle().set("padding", "var(--lumo-space-xs) var(--lumo-space-s)");
        row.addClassName("detail-row");

        Icon iconComponent = icon.create();
        iconComponent.setSize("16px");
        iconComponent.getStyle().set("color", "var(--lumo-primary-color)");
        iconComponent.getStyle().set("min-width", "20px");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.XSMALL);
        labelSpan.getStyle().set("min-width", "120px");
        labelSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Span valueSpan = new Span(value);
        valueSpan.addClassName(LumoUtility.FontSize.SMALL);
        valueSpan.getStyle().set("word-break", "break-all");
        valueSpan.getStyle().set("flex", "1");

        // Store references
        if (label.equals(I18n.t("template.card.name"))) {
            nameSpan = valueSpan;
        } else if (label.equals(I18n.t("template.card.code"))) {
            codeSpan = valueSpan;
        } else if (label.equals(I18n.t("template.card.description"))) {
            descriptionSpan = valueSpan;
        } else if (label.equals(I18n.t("template.card.file"))) {
            fileSpan = valueSpan;
        }

        row.add(iconComponent, labelSpan, valueSpan);
        row.expand(valueSpan);

        return row;
    }

    private HorizontalLayout createDetailRowWithCopy(VaadinIcon icon, String label, String value, String copyValue) {
        HorizontalLayout row = createDetailRow(icon, label, value);

        Button copyBtn = new Button(new Icon(VaadinIcon.COPY));
        copyBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY_INLINE);
        copyBtn.setTooltipText(I18n.t("template.card.copy.tooltip"));
        copyBtn.addClickListener(e -> {
            getUI().ifPresent(ui -> ui.getPage().executeJs(
                    "navigator.clipboard.writeText($0)",
                    copyValue
            ));
        });
        copyBtn.getStyle().set("padding", "var(--lumo-space-xs)");

        row.add(copyBtn);
        return row;
    }

    // ─── Action Methods ─────────────────────────────────────────────────────

    private void openViewDialog() {
        ViewTemplateDialog dialog = new ViewTemplateDialog(template);
        dialog.open();
    }

    private void openEditDialog() {
        EditTemplateDialog dialog = new EditTemplateDialog(
                objectService,
                template,
                () -> {
                    refresh();
                    if (onRefresh != null) {
                        onRefresh.run();
                    }
                }
        );
        dialog.open();
    }

    private void confirmDelete() {
        // Simplified delete - in real implementation use a confirmation dialog
        try {
            objectService.delete(template.getId());
            if (onRefresh != null) {
                onRefresh.run();
            }
            // Remove card from parent
            getParent().ifPresent(parent -> {
                if (parent instanceof VerticalLayout layout) {
                    layout.remove(this);
                }
            });
        } catch (Exception e) {
            log.error("Failed to delete template {}", template.getId(), e);
        }
    }

    // ─── Lifecycle ──────────────────────────────────────────────────────────

    @Override
    protected void onCardAttach(AttachEvent event) {
        updateDisplay();
    }

    // ─── Extra Styles ──────────────────────────────────────────────────────

    @Override
    protected String buildExtraStyles() {
        return """
                .template-card .detail-row {
                    border-bottom: 1px solid var(--lumo-contrast-5pct);
                }
                .template-card .detail-row:last-child {
                    border-bottom: none;
                }
                .template-card .detail-row:hover {
                    background: var(--lumo-primary-color-5pct);
                }
                .template-card .file-indicator {
                    font-weight: 600;
                }
                @media (max-width: 640px) {
                    .template-card .detail-row {
                        flex-wrap: wrap;
                    }
                    .template-card .detail-row > :not(:first-child) {
                        margin-left: 28px;
                    }
                }
                """;
    }
}