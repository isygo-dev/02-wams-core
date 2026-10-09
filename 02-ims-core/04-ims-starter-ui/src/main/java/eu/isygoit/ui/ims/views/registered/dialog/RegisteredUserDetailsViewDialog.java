package eu.isygoit.ui.ims.views.registered.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.dto.request.RegisteredUserDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.RegisteredUserService;
import eu.isygoit.ui.common.dialog.DetailHero;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.ims.views.common.ImsDetailsDialog;
import eu.isygoit.ui.ims.views.common.ImsEnumTag;
import eu.isygoit.ui.ims.views.registered.RegisteredManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only view of a {@link RegisteredUserDto}: every DTO field except {@code id}
 * (never displayed) is shown, grouped in tabs (identity, classification, audit).
 */
public class RegisteredUserDetailsViewDialog extends ImsDetailsDialog {

    private final RegisteredManagementView parentView;
    private final RegisteredUserService registeredUserService;
    private final Long registeredUserId;

    public RegisteredUserDetailsViewDialog(RegisteredManagementView parentView,
                                           RegisteredUserService registeredUserService,
                                           Long registeredUserId) {
        super(I18n.t("ims.registered.details.title"));
        this.parentView = parentView;
        this.registeredUserService = registeredUserService;
        this.registeredUserId = registeredUserId;

        applyWidth(DialogLayout.WIDTH_M);
        setModal(true);
        setDraggable(true);

        loadAndShowDetails();
    }

    private void loadAndShowDetails() {
        parentView.showLoading(true);
        try {
            ResponseEntity<RegisteredUserDto> response = registeredUserService.findById(registeredUserId);
            if (response.getBody() != null) {
                buildContent(response.getBody());
            } else {
                add(new Span(I18n.t("ims.registered.details.not.found")));
            }
        } catch (FeignException ex) {
            add(new Span(I18n.t("ims.registered.details.load.error",
                    RegisteredDialogSupport.extractErrorMessage(ex))));
        } catch (Exception e) {
            add(new Span(I18n.t("ims.registered.details.load.error", e.getMessage())));
        } finally {
            parentView.showLoading(false);
        }
    }

    private void buildContent(RegisteredUserDto user) {
        addIdentityTab(user);
        addClassificationTab(user);
        addAuditTab(user.getCreatedBy(), formatDate(user.getCreateDate()),
                user.getUpdatedBy(), formatDate(user.getUpdateDate()));
    }

    private void addIdentityTab(RegisteredUserDto user) {
        Div grid = createDetailGrid();
        addFieldToGrid(grid, VaadinIcon.USER, I18n.t("ims.registered.details.field.name"),
                dash(fullName(user)));
        addFieldToGrid(grid, VaadinIcon.ENVELOPE, I18n.t("ims.registered.details.field.email"),
                dash(user.getEmail()), true);
        addFieldToGrid(grid, VaadinIcon.PHONE, I18n.t("ims.registered.details.field.phone"),
                dash(user.getPhoneNumber()));
        addFieldToGrid(grid, VaadinIcon.BUILDING, I18n.t("ims.registered.details.field.organisation"),
                dash(user.getOrganisation()));

        VerticalLayout identity = DialogLayout.stack();
        identity.add(buildHero(user),
                createSection(I18n.t("ims.registered.details.section.identity"), grid));
        addTab(I18n.t("ims.registered.details.section.identity"), identity);
    }

    private void addClassificationTab(RegisteredUserDto user) {
        Div grid = createDetailGrid();
        ImsEnumTag.addDetailField(grid, VaadinIcon.SITEMAP, I18n.t("ims.registered.details.field.origin"),
                user.getOrigin(), "ims.enum.origin");
        addFieldToGrid(grid, VaadinIcon.BRIEFCASE, I18n.t("ims.registered.details.field.function.role"),
                dash(user.getFunctionRole()));
        ImsEnumTag.addDetailField(grid, VaadinIcon.SHIELD, I18n.t("ims.registered.details.field.status"),
                user.getStatus(), "ims.registered.card.status");
        addFieldToGrid(grid, VaadinIcon.BUILDING_O, I18n.t("ims.registered.details.field.tenant"),
                dash(user.getTenant()), true);
        addTab(I18n.t("ims.registered.details.section.classification"),
                createSection(I18n.t("ims.registered.details.section.classification"), grid));
    }

    private Component buildHero(RegisteredUserDto user) {
        List<Component> chips = new ArrayList<>();
        if (user.getStatus() != null) {
            chips.add(ImsEnumTag.of(user.getStatus(), "ims.registered.card.status"));
        }
        return new DetailHero(null, dash(fullName(user)), user.getEmail(), chips.toArray(Component[]::new));
    }

    private static String fullName(RegisteredUserDto user) {
        return ((user.getFirstName() != null ? user.getFirstName() : "") + " "
                + (user.getLastName() != null ? user.getLastName() : "")).trim();
    }

    private static String formatDate(LocalDateTime date) {
        return date == null ? null : DateHelper.formatToHumanReadable(date);
    }
}
