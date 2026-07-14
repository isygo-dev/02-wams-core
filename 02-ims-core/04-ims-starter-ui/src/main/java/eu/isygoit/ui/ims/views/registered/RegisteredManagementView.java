package eu.isygoit.ui.ims.views.registered;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.request.RegisteredUserDto;
import eu.isygoit.enums.IEnumAccountOrigin;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.RegisteredUserService;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.ims.layout.ImsMainLayout;
import eu.isygoit.ui.ims.views.registered.dialog.CreateRegisteredUserDialog;
import eu.isygoit.ui.ims.views.registered.dialog.UpdateRegisteredUserDialog;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@VaadinSessionScope
@Route(value = "ims/registered-users", layout = ImsMainLayout.class)
@PageTitle("Registered User Management")
@PermitAll
public class RegisteredManagementView extends ManagementVerticalView {

    private final RegisteredUserService registeredUserService;

    private final Div cardsContainer = new Div();
    private final Button createButton = new Button(I18n.t("ims.registered.view.create.button"), new Icon(VaadinIcon.PLUS_CIRCLE));
    private final Button refreshButton = new Button(new Icon(VaadinIcon.REFRESH));
    private final TextField searchField = new TextField();
    private final ComboBox<IEnumAccountOrigin.Types> originFilter = new ComboBox<>();

    private final ProgressBar loadingBar = new ProgressBar();
    private final ComboBox<Integer> pageSizeSelect = new ComboBox<>();
    private final Button prevButton = new Button(new Icon(VaadinIcon.CHEVRON_LEFT));
    private final Button nextButton = new Button(new Icon(VaadinIcon.CHEVRON_RIGHT));
    private final Span pageInfoLabel = new Span();
    private final Span totalCountLabel = new Span();

    private int currentPage = 0;
    private int pageSize = 10;
    private int totalPages = 0;
    private long totalElements = 0;
    private List<RegisteredUserDto> currentPageRegisteredUsers = new ArrayList<>();

    private String currentSearch = "";
    private IEnumAccountOrigin.Types currentOrigin = null;

    @Autowired
    public RegisteredManagementView(RegisteredUserService registeredUserService) {
        this.registeredUserService = registeredUserService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("registered-management-view");

        H2 header = new H2(I18n.t("ims.registered.view.title"));
        header.addClassName(LumoUtility.FontSize.XXLARGE);
        header.addClassName(LumoUtility.Margin.Bottom.NONE);
        add(header);

        HorizontalLayout toolbar = buildToolbar();
        add(toolbar);

        cardsContainer.setWidthFull();
        cardsContainer.addClassName("registered-cards-grid");
        add(cardsContainer);

        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        add(loadingBar);

        initEventHandlers();

        loadPage(0);
    }

    private void initEventHandlers() {
        createButton.addClickListener(e -> openCreateRegisteredUserDialog());
        createButton.setTooltipText(I18n.t("ims.registered.view.create.tooltip"));

        refreshButton.addClickListener(e -> loadPage(0));
        refreshButton.setTooltipText(I18n.t("ims.registered.view.refresh.tooltip"));

        searchField.setPlaceholder(I18n.t("ims.registered.view.search.placeholder"));
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> {
            currentSearch = e.getValue();
            loadPage(0);
        });

        originFilter.setItems(IEnumAccountOrigin.Types.values());
        originFilter.setItemLabelGenerator(Enum::name);
        originFilter.setPlaceholder(I18n.t("ims.registered.view.origin.placeholder"));
        originFilter.addValueChangeListener(e -> {
            currentOrigin = e.getValue();
            loadPage(0);
        });

        pageSizeSelect.setItems(5, 10, 20, 50);
        pageSizeSelect.setValue(10);
        pageSizeSelect.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                pageSize = e.getValue();
                loadPage(0);
            }
        });

        prevButton.addClickListener(e -> {
            if (currentPage > 0) loadPage(currentPage - 1);
        });
        nextButton.addClickListener(e -> {
            if (currentPage + 1 < totalPages) loadPage(currentPage + 1);
        });
    }

    private void loadPage(int page) {
        showLoading(true);
        try {
            ResponseEntity<PaginatedResponseDto<RegisteredUserDto>> response =
                    registeredUserService.findAll(page, pageSize);
            PaginatedResponseDto<RegisteredUserDto> body = response.getBody();
            if (body != null && body.getContent() != null) {
                currentPageRegisteredUsers = body.getContent();
                totalElements = body.getTotalElements();
                totalPages = body.getTotalPages();
                currentPage = body.getPageNumber();
            } else {
                currentPageRegisteredUsers = new ArrayList<>();
                totalElements = 0;
                totalPages = 0;
            }
            updatePaginationDisplay();
            filterAndDisplayCards();
        } catch (FeignException ex) {
            String errorMsg = extractErrorMessage(ex);
            Notification.show(I18n.t("ims.registered.view.load.error", errorMsg), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            log.error("Failed to load registered users", ex);
        } catch (Exception e) {
            Notification.show(I18n.t("ims.registered.view.load.error", e.getMessage()), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            log.error("Failed to load registered users", e);
        } finally {
            showLoading(false);
        }
    }

    private void filterAndDisplayCards() {
        List<RegisteredUserDto> filtered = currentPageRegisteredUsers.stream()
                .filter(registeredUser -> {
                    if (currentOrigin != null &&
                            (registeredUser.getOrigin() == null || !registeredUser.getOrigin().equals(currentOrigin.name()))) {
                        return false;
                    }
                    if (currentSearch != null && !currentSearch.isBlank()) {
                        String searchLower = currentSearch.toLowerCase();
                        return (registeredUser.getFirstName() != null && registeredUser.getFirstName().toLowerCase().contains(searchLower)) ||
                                (registeredUser.getLastName() != null && registeredUser.getLastName().toLowerCase().contains(searchLower)) ||
                                (registeredUser.getEmail() != null && registeredUser.getEmail().toLowerCase().contains(searchLower));
                    }
                    return true;
                })
                .collect(Collectors.toList());

        cardsContainer.removeAll();

        if (filtered.isEmpty()) {
            Div emptyState = new Div();
            emptyState.addClassName(LumoUtility.TextAlignment.CENTER);
            emptyState.addClassName(LumoUtility.Padding.XLARGE);
            Icon emptyIcon = VaadinIcon.CLIPBOARD_USER.create();
            emptyIcon.setSize("48px");
            emptyIcon.addClassName("empty-state-icon");
            H4 emptyTitle = new H4(I18n.t("ims.registered.view.empty.title"));
            Paragraph emptyDesc = new Paragraph(I18n.t("ims.registered.view.empty.description"));
            emptyDesc.addClassName(LumoUtility.TextColor.SECONDARY);
            emptyState.add(emptyIcon, emptyTitle, emptyDesc);
            cardsContainer.add(emptyState);
        } else {
            for (RegisteredUserDto registeredUser : filtered) {
                cardsContainer.add(new RegisteredCard(this, registeredUserService, registeredUser, this::loadPageZero));
            }
        }
    }

    private void updatePaginationDisplay() {
        pageInfoLabel.setText(I18n.t("ims.registered.view.page.info", currentPage + 1, totalPages));
        totalCountLabel.setText(I18n.t("ims.registered.view.total.count", totalElements));
        prevButton.setEnabled(currentPage > 0);
        nextButton.setEnabled(currentPage + 1 < totalPages);
    }

    private HorizontalLayout buildToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setPadding(false);
        toolbar.setSpacing(true);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        toolbar.addClassName("registered-management-toolbar");

        HorizontalLayout leftGroup = new HorizontalLayout();
        leftGroup.setSpacing(true);
        leftGroup.setAlignItems(FlexComponent.Alignment.END);
        searchField.setWidth("200px");
        originFilter.setWidth("160px");
        leftGroup.add(searchField, originFilter);

        HorizontalLayout centerGroup = new HorizontalLayout();
        centerGroup.setSpacing(true);
        centerGroup.setAlignItems(FlexComponent.Alignment.CENTER);
        prevButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        nextButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        pageSizeSelect.setWidth("100px");
        pageInfoLabel.addClassName("page-info-label");
        centerGroup.add(prevButton, pageInfoLabel, nextButton, totalCountLabel, pageSizeSelect);

        HorizontalLayout rightGroup = new HorizontalLayout();
        rightGroup.setSpacing(true);
        rightGroup.setAlignItems(FlexComponent.Alignment.END);
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        rightGroup.add(refreshButton, createButton);

        toolbar.add(leftGroup, centerGroup, rightGroup);
        return toolbar;
    }

    private void openCreateRegisteredUserDialog() {
        new CreateRegisteredUserDialog(this, registeredUserService, this::loadPageZero).open();
    }

    public void openUpdateRegisteredUserDialog(RegisteredUserDto registeredUser, Runnable onSuccess) {
        new UpdateRegisteredUserDialog(this, registeredUserService, registeredUser, onSuccess).open();
    }

    public void loadPageZero() {
        loadPage(0);
    }

    public void showLoading(boolean show) {
        loadingBar.setVisible(show);
        cardsContainer.setVisible(!show);
        refreshButton.setEnabled(!show);
        createButton.setEnabled(!show);
    }

    private String extractErrorMessage(FeignException ex) {
        try {
            if (ex.contentUTF8() != null && !ex.contentUTF8().isBlank()) {
                return ex.contentUTF8();
            }
        } catch (Exception ignored) {
        }
        return ex.getMessage();
    }
}
