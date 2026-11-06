package eu.isygoit.ui.kms.views.cryptography.random;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.common.RandomKeyDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.RandomKeyService;
import eu.isygoit.ui.common.view.AbstractPaginatedView;
import eu.isygoit.ui.kms.layout.KmsMainLayout;
import eu.isygoit.ui.kms.views.cryptography.random.dialog.CreateRandomKeyDialog;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SpringComponent
@RouteScope
@Route(value = "kms/random-keys", layout = KmsMainLayout.class)
@PermitAll
public class RandomKeyView extends AbstractPaginatedView<RandomKeyDto> {
    private final RandomKeyService keyService;
    private final Span totalCountLabel = new Span();

    @Autowired
    public RandomKeyView(RandomKeyService keyService) {
        this.keyService = keyService;
        this.pageSize = 10;

        totalCountLabel.addClassName("management-total-count");

        initPaginatedView();
    }

    @Override
    public String getPageTitle() {
        return I18n.t(getTitleKey());
    }

    // ─── Data ──────────────────────────────────────────────────────

    @Override
    protected PaginatedResponseDto<RandomKeyDto> fetchPage(int page, int size) {
        String search = searchField.getValue();
        if (search == null || search.isBlank()) {
            ResponseEntity<PaginatedResponseDto<RandomKeyDto>> response = keyService.listRandomKeys(page, size);
            return response.getBody();
        }

        // Search is applied over the whole list (same behaviour as before), then paged locally.
        ResponseEntity<PaginatedResponseDto<RandomKeyDto>> response = keyService.listRandomKeys(0, 5000);
        PaginatedResponseDto<RandomKeyDto> body = response.getBody();
        List<RandomKeyDto> fullList = (body != null && body.getContent() != null)
                ? body.getContent() : new ArrayList<>();
        String searchLower = search.toLowerCase();
        List<RandomKeyDto> filtered = fullList.stream()
                .filter(k -> k.getName() != null && k.getName().toLowerCase().contains(searchLower))
                .collect(Collectors.toList());

        long total = filtered.size();
        int start = page * size;
        int end = (int) Math.min((long) start + size, total);
        List<RandomKeyDto> pageItems = (start < total) ? new ArrayList<>(filtered.subList(start, end)) : new ArrayList<>();
        return PaginatedResponseDto.<RandomKeyDto>builder()
                .content(pageItems)
                .totalElements(total)
                .totalPages((int) Math.ceil((double) total / size))
                .pageNumber(page)
                .pageSize(size)
                .build();
    }

    @Override
    protected Component renderItem(RandomKeyDto dto) {
        return new RandomKeyCard(this, keyService, dto);
    }

    @Override
    protected boolean matchesSearch(RandomKeyDto dto, String search) {
        // The search is already applied server-side in fetchPage.
        return true;
    }

    @Override
    protected void onSearchChanged() {
        loadPage(0);
    }

    @Override
    protected void onPageLoaded() {
        totalCountLabel.setText(I18n.t("kms.random.key.total.count", totalElements));
    }

    @Override
    protected void openCreateDialog() {
        new CreateRandomKeyDialog(keyService, this::refresh).open();
    }

    @Override
    protected boolean configureGrid(Grid<RandomKeyDto> grid) {
        grid.addColumn(RandomKeyDto::getName)
                .setHeader(I18n.t("kms.random.key.table.name")).setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(dto -> dto.getValue() == null ? 0 : dto.getValue().length())
                .setHeader(I18n.t("kms.random.key.card.key.value")).setAutoWidth(true);
        grid.addColumn(dto -> dto.getCreateDate() != null ? eu.isygoit.helper.DateHelper.formatToHumanReadable(dto.getCreateDate()) : "")
                .setHeader(I18n.t("kms.random.key.card.created")).setAutoWidth(true);
        addCardActionsColumn(grid);
        return true;
    }

    @Override
    protected List<Component> buildToolbarActions() {
        return List.of(totalCountLabel);
    }

    public void refreshCard(RandomKeyCard card) {
        refresh();
    }

    public void removeCard(RandomKeyCard card) {
        refresh();
    }

    // ─── Base-class contract ───────────────────────────────────────

    @Override
    protected String[] getRootCssClasses() {
        return new String[]{"random-keys-view"};
    }

    @Override
    protected String getTitleKey() {
        return "kms.random.key.title";
    }

    @Override
    protected String getSubtitleKey() {
        return "kms.random.key.subtitle";
    }

    @Override
    protected String getCreateButtonKey() {
        return "kms.random.key.create.button";
    }

    @Override
    protected String getSearchPlaceholderKey() {
        return "kms.random.key.search.placeholder";
    }

    @Override
    protected String getEmptyTitleKey() {
        return "kms.random.key.empty.title";
    }

    @Override
    protected String getEmptyDescriptionKey() {
        return "kms.random.key.empty.description";
    }

    @Override
    protected String getLoadErrorKey() {
        return "kms.random.key.load.error";
    }

    @Override
    protected VaadinIcon getEmptyIcon() {
        return VaadinIcon.KEY;
    }
}
