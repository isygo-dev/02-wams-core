package eu.isygoit.ui.mms.views.sender;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.spring.annotation.UIScope;
import eu.isygoit.dto.data.SenderConfigDto;
import eu.isygoit.remote.mms.SenderConfigService;

import java.util.ArrayList;
import java.util.List;

/**
 * Container component that holds and manages SenderConfigCard instances.
 */
@UIScope
public class SenderConfigCardGrid extends VerticalLayout {

    private final SenderConfigService senderConfigService;
    private final List<SenderConfigCard> cards = new ArrayList<>();
    private final Runnable refreshCallback;

    public SenderConfigCardGrid(SenderConfigService senderConfigService) {
        this.senderConfigService = senderConfigService;
        this.refreshCallback = this::refreshAll;

        setPadding(false);
        setSpacing(true);
        setWidthFull();
        addClassName("sender-card-grid");
    }

    public void setConfigs(List<SenderConfigDto> configs) {
        removeAll();
        cards.clear();

        if (configs == null || configs.isEmpty()) {
            // Show empty state
            return;
        }

        for (SenderConfigDto config : configs) {
            SenderConfigCard card = new SenderConfigCard(
                    null, // parent view - pass if needed
                    senderConfigService,
                    config,
                    refreshCallback
            );
            cards.add(card);
            add(card);
        }
    }

    public void refreshAll() {
        for (SenderConfigCard card : cards) {
            card.refresh();
        }
    }

    public void addConfig(SenderConfigDto config) {
        SenderConfigCard card = new SenderConfigCard(
                null,
                senderConfigService,
                config,
                refreshCallback
        );
        cards.add(card);
        add(card);
    }

    public void removeConfig(Long id) {
        SenderConfigCard toRemove = cards.stream()
                .filter(card -> card.getId().equals(id))
                .findFirst()
                .orElse(null);
        if (toRemove != null) {
            cards.remove(toRemove);
            remove(toRemove);
        }
    }

    public List<SenderConfigCard> getCards() {
        return new ArrayList<>(cards);
    }
}