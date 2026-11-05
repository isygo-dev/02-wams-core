package eu.isygoit.ui.kms.views.cryptography.incremental;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import eu.isygoit.dto.common.NextCodeDto;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.exception.GenerationFailedException;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsAppNextCodeService;
import eu.isygoit.remote.kms.KmsIncrementalKeyService;
import eu.isygoit.ui.common.view.AbstractPaginatedView;
import eu.isygoit.ui.kms.layout.KmsMainLayout;
import eu.isygoit.ui.kms.views.cryptography.incremental.dialog.DeleteNextCodeConfigDialog;
import eu.isygoit.ui.kms.views.cryptography.incremental.dialog.NextCodeDetailsViewDialog;
import eu.isygoit.ui.kms.views.cryptography.incremental.dialog.SubscribeNextCodeConfigDialog;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@VaadinSessionScope
@Route(value = "kms/incremental-key", layout = KmsMainLayout.class)
@PermitAll
public class IncrementalKeyView extends AbstractPaginatedView<NextCodeDto> {

    @Getter
    private final KmsIncrementalKeyService incrementalKeyService;
    @Getter
    private final KmsAppNextCodeService nextCodeService;
    private final Button refreshButton = new Button(VaadinIcon.REFRESH.create());
    private final ComboBox<Integer> pageSizeSelect = new ComboBox<>();
    private final com.vaadin.flow.component.html.Span totalCountLabel =
            new com.vaadin.flow.component.html.Span();

    @Autowired
    public IncrementalKeyView(KmsIncrementalKeyService incrementalKeyService,
                              KmsAppNextCodeService nextCodeService) {
        this.incrementalKeyService = incrementalKeyService;
        this.nextCodeService = nextCodeService;
        pageSize = 10;
        initPaginatedView();
        refreshButton.setTooltipText(I18n.t("kms.incremental.key.refresh.tooltip"));
    }

    @Override
    protected PaginatedResponseDto<NextCodeDto> fetchPage(int page, int size) {
        String criteria = searchCriteria(searchField.getValue());
        var response = criteria.isBlank()
                ? nextCodeService.findAll(page, size)
                : nextCodeService.findAllFilteredByCriteria(criteria, page, size);
        return response != null ? response.getBody() : null;
    }

    @Override
    protected PaginatedResponseDto<? extends NextCodeDto> fetchFilteredPage(
            String criteria, int page, int size) {
        String searchCriteria = searchCriteria(searchField.getValue());
        String mergedCriteria = searchCriteria.isBlank()
                ? criteria
                : criteria == null || criteria.isBlank()
                    ? searchCriteria
                    : "(" + criteria + ") & " + searchCriteria;
        var response = nextCodeService.findAllFilteredByCriteria(mergedCriteria, page, size);
        return response != null ? response.getBody() : null;
    }

    @Override
    protected Map<String, String> fetchCriteriaMetadata() {
        var response = nextCodeService.getAnnotatedCriteria();
        return response != null && response.getBody() != null
                ? response.getBody().stream().collect(Collectors.toMap(
                field -> field.name(), field -> field.type(), (first, second) -> second,
                java.util.LinkedHashMap::new))
                : Map.of();
    }

    @Override
    protected boolean configureGrid(Grid<NextCodeDto> grid) {
        grid.addColumn(NextCodeDto::getEntity).setHeader("Entity").setAutoWidth(true);
        grid.addColumn(NextCodeDto::getAttribute).setHeader("Attribute").setAutoWidth(true);
        grid.addColumn(NextCodeDto::getPrefix).setHeader("Prefix").setAutoWidth(true);
        grid.addColumn(NextCodeDto::getSuffix).setHeader("Suffix").setAutoWidth(true);
        grid.addColumn(NextCodeDto::getIncrement).setHeader("Increment").setAutoWidth(true);
        addCardActionsColumn(grid);
        return true;
    }

    @Override
    protected Component renderItem(NextCodeDto item) {
        return new NextCodeCard(this, nextCodeService, item,
                () -> deleteConfig(item.getId()), this::generateCodeForConfig);
    }

    @Override
    protected boolean matchesSearch(NextCodeDto item, String search) {
        return true;
    }

    @Override
    protected void openCreateDialog() {
        new SubscribeNextCodeConfigDialog(nextCodeService, this::loadPageZero).open();
    }

    @Override
    protected void onSearchChanged() {
        loadPageZero();
    }

    @Override
    protected List<Component> buildToolbarActions() {
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.addClickListener(event -> loadPageZero());
        pageSizeSelect.setItems(5, 10, 20, 50);
        pageSizeSelect.setValue(pageSize);
        pageSizeSelect.setWidth("100px");
        pageSizeSelect.addValueChangeListener(event -> {
            if (event.getValue() != null && event.getValue() != pageSize) {
                pageSize = event.getValue();
                loadPageZero();
            }
        });
        totalCountLabel.addClassName("management-total-count");
        return List.of(refreshButton, totalCountLabel, pageSizeSelect);
    }

    @Override
    protected void onPageLoaded() {
        totalCountLabel.setText(I18n.t("kms.incremental.key.total.count", totalElements));
        if (totalPages > 0 && currentPage >= totalPages) {
            loadPage(totalPages - 1);
        }
    }

    @Override
    protected String[] getRootCssClasses() {
        return new String[]{"incremental-keys-view"};
    }

    @Override
    protected String extractErrorMessage(Exception exception) {
        if (exception instanceof FeignException feignException
                && feignException.contentUTF8() != null && !feignException.contentUTF8().isBlank()) {
            return feignException.contentUTF8();
        }
        return exception.getMessage();
    }

    @Override
    protected String getTitleKey() {
        return "kms.incremental.key.title";
    }

    @Override
    protected String getSubtitleKey() {
        return null;
    }

    @Override
    protected String getCreateButtonKey() {
        return "kms.incremental.key.subscribe.button";
    }

    @Override
    protected String getSearchPlaceholderKey() {
        return "kms.incremental.key.search.placeholder";
    }

    @Override
    protected String getEmptyTitleKey() {
        return "kms.incremental.key.empty.title";
    }

    @Override
    protected String getEmptyDescriptionKey() {
        return "kms.incremental.key.empty.description";
    }

    @Override
    protected VaadinIcon getEmptyIcon() {
        return VaadinIcon.KEY;
    }

    @Override
    protected String getLoadErrorKey() {
        return "kms.incremental.key.load.error";
    }

    @Override
    public String getPageTitle() {
        return I18n.t(getTitleKey());
    }

    public void loadNextCodes() {
        loadPage(currentPage);
    }

    public void loadPageZero() {
        loadPage(0);
    }

    public void refreshCard(NextCodeCard card) {
        try {
            var response = nextCodeService.findById(card.getDto().getId());
            NextCodeDto updated = response.getBody();
            if (updated == null) {
                loadNextCodes();
                return;
            }
            card.updateDto(updated);
            for (int i = 0; i < currentItems.size(); i++) {
                if (currentItems.get(i).getId().equals(updated.getId())) {
                    currentItems.set(i, updated);
                    applyLocalFilter();
                    return;
                }
            }
        } catch (Exception exception) {
            eu.isygoit.ui.common.notification.AppNotifications.error(
                    I18n.t("kms.incremental.key.refresh.card.error", exception.getMessage()));
        }
    }

    private void deleteConfig(Long id) {
        try {
            nextCodeService.delete(id);
            eu.isygoit.ui.common.notification.AppNotifications.success(
                    I18n.t("kms.incremental.key.delete.success"));
            loadNextCodes();
        } catch (Exception exception) {
            eu.isygoit.ui.common.notification.AppNotifications.error(
                    I18n.t("kms.incremental.key.delete.failed", exception.getMessage()));
        }
    }

    private String generateCodeForConfig(String entity, String attribute) {
        var response = incrementalKeyService.generateNextCode(entity, attribute);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        }
        throw new GenerationFailedException("Generation failed with status: " + response.getStatusCode());
    }

    private String searchCriteria(String search) {
        if (search == null || search.isBlank()) {
            return "";
        }
        String escaped = search.replace("'", "''");
        return "(entity ~ '" + escaped + "' | attribute ~ '" + escaped + "')";
    }
}
