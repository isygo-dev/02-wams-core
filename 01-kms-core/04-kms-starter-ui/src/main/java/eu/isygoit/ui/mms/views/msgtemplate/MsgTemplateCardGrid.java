package eu.isygoit.ui.mms.views.msgtemplate;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.spring.annotation.UIScope;
import eu.isygoit.dto.data.MsgTemplateDto;
import eu.isygoit.remote.mms.MsgTemplateService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Container component that holds and manages MsgTemplateCard instances.
 */
@Component
@UIScope
public class MsgTemplateCardGrid extends VerticalLayout {

    private final MsgTemplateService templateService;
    private final List<MsgTemplateCard> cards = new ArrayList<>();
    private final Runnable refreshCallback;

    public MsgTemplateCardGrid(MsgTemplateService templateService) {
        this.templateService = templateService;
        this.refreshCallback = this::refreshAll;

        setPadding(false);
        setSpacing(true);
        setWidthFull();
        addClassName("template-card-grid");
    }

    public void setTemplates(List<MsgTemplateDto> templates) {
        removeAll();
        cards.clear();

        if (templates == null || templates.isEmpty()) {
            // Show empty state
            return;
        }

        for (MsgTemplateDto template : templates) {
            MsgTemplateCard card = new MsgTemplateCard(
                    null,
                    templateService,
                    template,
                    refreshCallback
            );
            cards.add(card);
            add(card);
        }
    }

    public void refreshAll() {
        for (MsgTemplateCard card : cards) {
            card.refresh();
        }
    }

    public void addTemplate(MsgTemplateDto template) {
        MsgTemplateCard card = new MsgTemplateCard(
                null,
                templateService,
                template,
                refreshCallback
        );
        cards.add(card);
        add(card);
    }

    public void removeTemplate(Long id) {
        MsgTemplateCard toRemove = cards.stream()
                .filter(card -> card.getId().equals(id))
                .findFirst()
                .orElse(null);
        if (toRemove != null) {
            cards.remove(toRemove);
            remove(toRemove);
        }
    }

    public List<MsgTemplateCard> getCards() {
        return new ArrayList<>(cards);
    }
}