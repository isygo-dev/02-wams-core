package eu.isygoit.ui.ims.views.application;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.data.ApplicationDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.ApplicationImageService;
import eu.isygoit.remote.ims.ApplicationService;
import eu.isygoit.ui.common.view.AbstractPaginatedView;
import eu.isygoit.ui.ims.layout.ImsMainLayout;
import eu.isygoit.ui.ims.views.application.dialog.CreateApplicationDialog;
import eu.isygoit.ui.ims.views.application.dialog.UpdateApplicationDialog;
import eu.isygoit.ui.ims.views.common.ImsEnumTag;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Slf4j
@SpringComponent
@RouteScope
@Route(value = "ims/applications", layout = ImsMainLayout.class)
@PermitAll
public class ApplicationManagementView extends AbstractPaginatedView<ApplicationDto> {

    private final ApplicationService applicationService;
    private final ApplicationImageService applicationImageService;

    private final ComboBox<IEnumEnabledBinaryStatus.Types> statusFilter = new ComboBox<>();

    private IEnumEnabledBinaryStatus.Types currentAdminStatus;

    @Autowired
    public ApplicationManagementView(ApplicationService applicationService,
                                     ApplicationImageService applicationImageService) {
        this.applicationService = applicationService;
        this.applicationImageService = applicationImageService;
        initPaginatedView();
    }

    private static byte[] readBytes(org.springframework.core.io.Resource resource) {
        try {
            return resource.getContentAsByteArray();
        } catch (java.io.IOException e) {
            return null;
        }
    }

    @Override
    protected PaginatedResponseDto<ApplicationDto> fetchPage(int page, int size) {
        var r = applicationService.findAll(page, size);
        return r != null ? r.getBody() : null;
    }

    @Override
    protected boolean configureGrid(com.vaadin.flow.component.grid.Grid<ApplicationDto> grid) {
        grid.addColumn(ApplicationDto::getCode).setHeader("Code").setAutoWidth(true);
        grid.addColumn(ApplicationDto::getName).setHeader("Name").setAutoWidth(true);
        grid.addColumn(ApplicationDto::getTitle).setHeader("Title").setAutoWidth(true);
        grid.addColumn(ApplicationDto::getCategory).setHeader("Category").setAutoWidth(true);
        grid.addColumn(ApplicationDto::getAdminStatus).setHeader("Status").setAutoWidth(true);
        addCardActionsColumn(grid);
        return true;
    }

    @Override
    protected PaginatedResponseDto<? extends ApplicationDto> fetchFilteredPage(String criteria, int page, int size) {
        var r = applicationService.findAllFilteredByCriteria(criteria, page, size);
        return r != null ? r.getBody() : null;
    }

    @Override
    protected java.util.Map<String, String> fetchCriteriaMetadata() {
        var r = applicationService.getAnnotatedCriteria();
        return r != null && r.getBody() != null
                ? r.getBody().stream().collect(java.util.stream.Collectors.toMap(
                field -> field.name(), field -> field.type(), (first, second) -> second,
                java.util.LinkedHashMap::new))
                : java.util.Map.of();
    }

    @Override
    protected Component renderItem(ApplicationDto item) {
        return new ApplicationCard(this, applicationService, applicationImageService,
                item, this::loadPageZero);
    }

    /**
     * Image shown in the first column of the table mode (the item implements IImageUploadDto).
     */
    @Override
    protected byte[] downloadItemImage(ApplicationDto item) {
        if (item == null || item.getId() == null) {
            return null;
        }
        try {
            var response = applicationImageService.downloadImage(item.getId());
            return response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null
                    ? readBytes(response.getBody()) : null;
        } catch (Exception e) {
            // no image: the avatar keeps the initials
            return null;
        }
    }

    @Override
    protected String getItemImageLabel(ApplicationDto item) {
        return item.getName() != null ? item.getName() : item.getCode();
    }

    @Override
    protected boolean matchesSearch(ApplicationDto a, String search) {
        if (search == null || search.isBlank()) return true;
        String s = search.toLowerCase();
        return (a.getName() != null && a.getName().toLowerCase().contains(s))
                || (a.getTitle() != null && a.getTitle().toLowerCase().contains(s));
    }

    @Override
    protected boolean matchesAdditionalFilters(ApplicationDto a) {
        return currentAdminStatus == null || a.getAdminStatus() == currentAdminStatus;
    }

    @Override
    protected void openCreateDialog() {
        new CreateApplicationDialog(this, applicationService, applicationImageService,
                this::loadPageZero).open();
    }

    @Override
    protected void onSearchChanged() {
        loadPageZero();
    }

    @Override
    protected String[] getRootCssClasses() {
        return new String[]{"application-management-view"};
    }

    @Override
    protected List<Component> buildToolbarFilters() {
        statusFilter.setItems(IEnumEnabledBinaryStatus.Types.values());
        statusFilter.setItemLabelGenerator(s -> ImsEnumTag.label(s, null));
        statusFilter.setRenderer(ImsEnumTag.renderer(null));
        statusFilter.setPlaceholder(I18n.t("ims.app.view.status.placeholder"));
        statusFilter.setWidth("160px");
        statusFilter.addValueChangeListener(e -> {
            currentAdminStatus = e.getValue();
            loadPageZero();
        });
        return List.of(statusFilter);
    }


    @Override
    protected String extractErrorMessage(Exception e) {
        if (e instanceof FeignException fex
                && fex.contentUTF8() != null && !fex.contentUTF8().isBlank()) {
            return fex.contentUTF8();
        }
        return e.getMessage();
    }

    @Override
    protected String getTitleKey() {
        return "ims.app.view.title";
    }

    @Override
    protected String getSubtitleKey() {
        return "ims.app.view.subtitle";
    }

    @Override
    protected String getCreateButtonKey() {
        return "ims.app.view.create.button";
    }

    @Override
    protected String getSearchPlaceholderKey() {
        return "ims.app.view.search.placeholder";
    }

    @Override
    protected String getEmptyTitleKey() {
        return "ims.app.view.empty.title";
    }

    @Override
    protected String getEmptyDescriptionKey() {
        return "ims.app.view.empty.description";
    }

    @Override
    protected VaadinIcon getEmptyIcon() {
        return VaadinIcon.ASTERISK;
    }

    @Override
    protected String getLoadErrorKey() {
        return "ims.app.view.load.error";
    }

    @Override
    public String getPageTitle() {
        return I18n.t("ims.app.view.title");
    }

    public void loadPageZero() {
        loadPage(0);
    }

    public void openUpdateApplicationDialog(ApplicationDto app, Runnable onSuccess) {
        new UpdateApplicationDialog(this, applicationService, applicationImageService,
                app, onSuccess).open();
    }
}