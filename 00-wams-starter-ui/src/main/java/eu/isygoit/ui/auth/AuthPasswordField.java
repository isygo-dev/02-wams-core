package eu.isygoit.ui.auth;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.PasswordField;
import elemental.json.JsonObject;
import eu.isygoit.i18n.I18n;

@Tag("auth-password-field")
public class AuthPasswordField extends Component implements HasComponents {

    private final PasswordField field = new PasswordField(I18n.t("auth.password.field.password.label"));
    private final Span capsLockHint = new Span(I18n.t("auth.password.capsLock"));

    public AuthPasswordField() {
        field.setRequiredIndicatorVisible(true);
        field.setRevealButtonVisible(true);
        field.setPlaceholder(I18n.t("auth.password.field.password.placeholder"));
        field.getElement().setAttribute("autocomplete", "current-password");
        capsLockHint.setVisible(false);
        capsLockHint.getElement().setAttribute("role", "status");
        field.getElement().addEventListener("auth-caps-lock", event -> {
            JsonObject eventData = event.getEventData();
            capsLockHint.setVisible(eventData.getBoolean("event.detail"));
        }).addEventData("event.detail");
        field.getElement().executeJs(
                "this.inputElement.addEventListener('keyup', event => this.dispatchEvent(" +
                        "new CustomEvent('auth-caps-lock', {detail: event.getModifierState('CapsLock')})))");
        field.addValueChangeListener(event -> capsLockHint.setVisible(false));
        add(field, capsLockHint);
    }

    public PasswordField getField() {
        return field;
    }

    public void setEnabled(boolean enabled) {
        field.setEnabled(enabled);
    }

    @Override
    public void add(Component... components) {
        for (Component component : components) {
            getElement().appendChild(component.getElement());
        }
    }
}
