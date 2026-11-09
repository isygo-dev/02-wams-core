package eu.isygoit.ui.plan.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.value.ValueChangeMode;
import eu.isygoit.i18n.I18n;
import lombok.extern.slf4j.Slf4j;

/**
 * Planning JSON input and display with an editable text area, syntax-colored overlay,
 * pretty-print action, and copy-to-clipboard action.
 */
@Slf4j
public class PlanJsonEditor extends VerticalLayout {

    private static final String BIND_HIGHLIGHT =
            "const surface=this;"
                    + "const field=surface.querySelector('.plan-json-editor__field');"
                    + "const output=surface.querySelector('.plan-json-editor__highlight');"
                    + "if(!field||!output)return;"
                    + "const escapeHtml=s=>s.replace(/[&<>\"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','\"':'&quot;',\"'\":'&#39;'}[c]));"
                    + "const bind=()=>{"
                    + "const input=field.shadowRoot&&field.shadowRoot.querySelector('textarea');"
                    + "if(!input)return false;"
                    + "if(surface.__planJsonInput===input){surface.__planJsonPaint();return true;}"
                    + "const paint=()=>{"
                    + "const text=input.value||'';const token=/\"(?:\\\\.|[^\"\\\\])*\"|-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?|\\b(?:true|false|null)\\b|[{}\\[\\],:]/g;"
                    + "let html='',last=0,match;"
                    + "while((match=token.exec(text))!==null){"
                    + "const value=match[0];html+=escapeHtml(text.slice(last,match.index));"
                    + "let kind='literal';"
                    + "if(value.charAt(0)==='\"')kind=/^\\s*:/.test(text.slice(token.lastIndex))?'key':'string';"
                    + "else if(/^-?\\d/.test(value))kind='number';"
                    + "else if(/^[{}\\[\\],:]$/.test(value))kind='punctuation';"
                    + "html+='<span class=\"plan-json-token--'+kind+'\">'+escapeHtml(value)+'</span>';last=token.lastIndex;"
                    + "}"
                    + "html+=escapeHtml(text.slice(last));output.innerHTML=html||' ';"
                    + "output.scrollTop=input.scrollTop;output.scrollLeft=input.scrollLeft;"
                    + "};"
                    + "input.addEventListener('input',paint);"
                    + "input.addEventListener('scroll',()=>{output.scrollTop=input.scrollTop;output.scrollLeft=input.scrollLeft;});"
                    + "surface.__planJsonInput=input;surface.__planJsonPaint=paint;"
                    + "surface.classList.add('plan-json-editor--highlight-ready');paint();return true;"
                    + "};"
                    + "surface.__planJsonBind=bind;"
                    + "let attempts=40;"
                    + "const retry=()=>{if(bind())return;if(attempts-->0)setTimeout(retry,50);else console.warn('Plan JSON syntax highlighting could not bind to the text area.');};"
                    + "const start=()=>retry();"
                    + "if(customElements.get('vaadin-text-area'))start();"
                    + "else customElements.whenDefined('vaadin-text-area').then(start);";
    private static final String COPY_TO_CLIPBOARD =
            "const text=$0;"
                    + "if(navigator.clipboard&&window.isSecureContext){navigator.clipboard.writeText(text);}"
                    + "else{const field=document.createElement('textarea');field.value=text;"
                    + "field.style.position='fixed';field.style.opacity='0';document.body.appendChild(field);"
                    + "field.select();document.execCommand('copy');field.remove();}";

    private final ObjectMapper objectMapper;
    private final TextArea input = new TextArea();
    private final Div surface = new Div();

    public PlanJsonEditor(ObjectMapper objectMapper, String label, String helperText, boolean readOnly) {
        this.objectMapper = objectMapper;
        setPadding(false);
        setSpacing(false);
        setWidthFull();
        addClassName("plan-json-editor");

        Span labelSpan = new Span(label);
        labelSpan.addClassName("plan-json-editor__label");
        add(labelSpan);

        if (helperText != null && !helperText.isBlank()) {
            Span helper = new Span(helperText);
            helper.addClassName("plan-json-editor__helper");
            add(helper);
        }

        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setPadding(false);
        toolbar.setSpacing(true);
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        toolbar.addClassName("plan-json-editor__toolbar");

        if (!readOnly) {
            Button formatButton = new Button(I18n.t("plan.ui.json.format"), VaadinIcon.CODE.create());
            formatButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            formatButton.addClickListener(event -> format());
            toolbar.add(formatButton);
        }

        Button copyButton = new Button(VaadinIcon.COPY.create());
        copyButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        copyButton.setTooltipText(I18n.t("common.action.copy.tooltip"));
        copyButton.setAriaLabel(I18n.t("common.action.copy.tooltip"));
        copyButton.addClickListener(event -> copy());
        toolbar.add(copyButton);
        add(toolbar);

        surface.addClassName("plan-json-editor__surface");
        Div highlight = new Div();
        highlight.addClassName("plan-json-editor__highlight");
        highlight.getElement().setAttribute("aria-hidden", "true");
        input.addClassName("plan-json-editor__field");
        input.setAriaLabel(label);
        input.setWidthFull();
        input.setHeightFull();
        input.setReadOnly(readOnly);
        input.setValueChangeMode(ValueChangeMode.ON_CHANGE);
        surface.add(highlight, input);
        add(surface);
    }

    public String getValue() {
        return input.getValue();
    }

    public void setValue(String value) {
        input.setValue(value == null ? "" : value);
        input.setInvalid(false);
        refreshHighlight();
    }

    public void setRequired(boolean required) {
        input.setRequiredIndicatorVisible(required);
    }

    public void setInvalid(boolean invalid) {
        input.setInvalid(invalid);
    }

    public void setErrorMessage(String message) {
        input.setErrorMessage(message);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        surface.getElement().executeJs(BIND_HIGHLIGHT);
    }

    private void format() {
        try {
            Object json = objectMapper.readValue(getValue(), Object.class);
            setValue(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json));
        } catch (JsonProcessingException exception) {
            log.debug("Unable to pretty-print invalid planning JSON", exception);
            input.setInvalid(true);
            input.setErrorMessage(I18n.t("plan.ui.json.invalid"));
        }
    }

    private void copy() {
        String value = getValue();
        if (value == null || value.isBlank()) {
            return;
        }
        UI.getCurrent().getPage().executeJs(COPY_TO_CLIPBOARD, value);
        Notification.show(I18n.t("common.action.copy.success"), 1500, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void refreshHighlight() {
        surface.getElement().executeJs(
                "if(this.__planJsonBind)this.__planJsonBind();"
                        + "else if(this.__planJsonPaint)this.__planJsonPaint();");
    }
}
