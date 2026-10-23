package eu.isygoit.ui.common.profile;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.api.ProfileServiceApi;
import eu.isygoit.dto.data.ConnectionPageDto;
import eu.isygoit.dto.data.ConnectionTrackingDto;
import eu.isygoit.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

/**
 * Activity tab: the login history as a chronological timeline (newest first), grouped by day, loaded one page at
 * a time from the server instead of pulling the whole history into the page.
 */
@Slf4j
class ProfileActivityPanel extends VerticalLayout {

    static final int PAGE_SIZE = 10;

    private final transient ProfileServiceApi profileService;
    private final Span totalLabel = new Span();
    private final Div body = new Div();

    ProfileActivityPanel(ProfileServiceApi profileService) {
        this.profileService = profileService;

        setPadding(true);
        setSpacing(true);
        setWidthFull();
        addClassName("profile-tab-panel");

        Div card = new Div();
        card.addClassName("profile-section-card");

        H3 title = new H3(I18n.t("profile.section.activity"));
        title.addClassName("section-title");
        totalLabel.addClassName("profile-section-count");

        Div header = new Div(title, totalLabel);
        header.addClassName("profile-section-header");

        card.add(header, body, pager);
        add(card);
        loadPage(0);
    }

    private void loadPage(int page) {
        ConnectionPageDto result = fetch(page);
        body.removeAll();
        if (result == null) {
            totalLabel.setText("");
            pager.update(0, 0, 0);
            body.add(errorState(page));
            return;
        }

        totalLabel.setText(I18n.t("profile.activity.total", result.getTotalElements()));
        pager.update(result.getPage(), result.getTotalPages(), result.getTotalElements());
        if (result.getContent() == null || result.getContent().isEmpty()) {
            body.add(new ProfileEmptyState(I18n.t("profile.history.empty")));
            return;
        }

        Div timeline = new Div();
        timeline.addClassName("profile-timeline");
        timeline.getElement().setAttribute("aria-label", I18n.t("profile.section.activity"));
        LocalDate currentDay = null;
        for (ConnectionTrackingDto connection : result.getContent()) {
            LocalDate day = ProfileFormatUtils.toLocalDate(connection.getLoginDate());
            if (currentDay == null || !currentDay.equals(day)) {
                currentDay = day;
                Div dayHeader = new Div(new Span(ProfileFormatUtils.formatDayLabel(day)));
                dayHeader.addClassName("profile-timeline-day");
                timeline.add(dayHeader);
            }
            timeline.add(new ConnectionTimelineItem(connection));
        }
        body.add(timeline);
    }    private final ProfilePagerBar pager = new ProfilePagerBar(this::loadPage);

    private ConnectionPageDto fetch(int page) {
        try {
            ResponseEntity<ConnectionPageDto> response = profileService.getConnections(page, PAGE_SIZE);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.warn("Failed to load login history page {}", page, e);
        }
        return null;
    }

    private Div errorState(int page) {
        Button retry = new Button(I18n.t("profile.load.retry"));
        retry.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        retry.addClickListener(e -> loadPage(page));
        Div error = new Div(new Span(I18n.t("profile.activity.load.error")), retry);
        error.addClassName("profile-load-error");
        return error;
    }




}
