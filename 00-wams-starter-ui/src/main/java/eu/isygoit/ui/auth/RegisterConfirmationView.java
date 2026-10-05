package eu.isygoit.ui.auth;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import eu.isygoit.i18n.I18n;
import jakarta.annotation.security.PermitAll;

@org.springframework.stereotype.Component
@UIScope
@Route(value = AuthRoutes.REGISTRATION_CONFIRMATION)
@PermitAll
public class RegisterConfirmationView extends BaseLoginView {

    public RegisterConfirmationView() {
        super("auth.page.title.registrationConfirmation");

        Button loginButton = new Button(
                I18n.t("auth.confirmation.loginButton"), VaadinIcon.ARROW_RIGHT.create());
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.addClickListener(event -> UI.getCurrent().navigate(AuthRoutes.LOGIN));

        AuthConfirmationMark mark = new AuthConfirmationMark();
        AuthConfirmationContent content = new AuthConfirmationContent();
        content.add(
                new H2(I18n.t("auth.confirmation.title")),
                new Paragraph(I18n.t("auth.confirmation.description")),
                new Paragraph(I18n.t("auth.confirmation.instructions")),
                loginButton
        );
        addToCard(new BrandHeader(I18n.t("auth.confirmation.brand"), null), mark, content,
                new AuthFooter());
    }

    @Override
    protected void onBeforeEnter(BeforeEnterEvent event) {
        clearError();
    }

    @Tag("auth-confirmation-mark")
    private static class AuthConfirmationMark extends Component implements HasComponents {

        private AuthConfirmationMark() {
            Icon icon = VaadinIcon.CHECK_CIRCLE_O.create();
            icon.getElement().setAttribute("aria-hidden", "true");
            add(icon);
        }

        @Override
        public void add(Component... components) {
            for (Component component : components) {
                getElement().appendChild(component.getElement());
            }
        }
    }

    @Tag("auth-confirmation-content")
    private static class AuthConfirmationContent extends Component implements HasComponents {

        @Override
        public void add(Component... components) {
            for (Component component : components) {
                getElement().appendChild(component.getElement());
            }
        }
    }
}
