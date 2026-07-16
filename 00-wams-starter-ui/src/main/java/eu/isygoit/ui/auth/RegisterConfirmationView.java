package eu.isygoit.ui.auth;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.annotation.UIScope;
import eu.isygoit.i18n.I18n;
import jakarta.annotation.security.PermitAll;

@org.springframework.stereotype.Component
@UIScope
@Route(value = "register-confirmation")
@PageTitle("Registration Confirmation")
@PermitAll
public class RegisterConfirmationView extends BaseLoginView {

    public RegisterConfirmationView() {
        configureAsAuthPage("register-confirmation-view");

        // Create success icon container
        Div iconContainer = createIconContainer();

        // Title
        H2 title = new H2(I18n.t("auth.confirmation.title"));
        title.addClassName("wams-auth-confirmation-title");

        // Description
        Paragraph description = new Paragraph(I18n.t("auth.confirmation.description"));
        description.addClassName("wams-auth-confirmation-description");

        // Instructions with email icon
        Div instructionsContainer = createInstructionsContainer();

        // Divider
        Div divider = createDivider();

        // Action buttons
        Component actionButtons = createActionButtons();

        // Main content
        VerticalLayout content = new VerticalLayout();
        content.setAlignItems(FlexComponent.Alignment.CENTER);
        content.setSpacing(false);
        content.setPadding(false);
        content.add(
                iconContainer,
                title,
                description,
                instructionsContainer,
                divider,
                actionButtons
        );
        content.addClassName("wams-auth-confirmation-content");

        // Card
        var card = createCard();
        card.addClassName("wams-auth-card--confirmation");
        card.add(
                createBrand(I18n.t("auth.confirmation.brand"), null),
                content,
                createFooter()
        );

        add(card);
    }

    private Div createIconContainer() {
        Div container = new Div();
        container.addClassName("wams-confirmation-icon-container");

        Icon successIcon = VaadinIcon.CHECK_CIRCLE_O.create();
        successIcon.addClassName("wams-confirmation-icon");

        container.add(successIcon);
        return container;
    }

    private Div createInstructionsContainer() {
        Div container = new Div();
        container.addClassName("wams-confirmation-instructions-container");

        Icon emailIcon = VaadinIcon.ENVELOPE_O.create();
        emailIcon.addClassName("wams-confirmation-email-icon");

        Paragraph instructions = new Paragraph(I18n.t("auth.confirmation.instructions"));
        instructions.addClassName("wams-auth-confirmation-instructions");

        HorizontalLayout instructionsLayout = new HorizontalLayout(emailIcon, instructions);
        instructionsLayout.setAlignItems(FlexComponent.Alignment.START);
        instructionsLayout.setSpacing(true);
        instructionsLayout.addClassName("wams-confirmation-instructions-layout");

        container.add(instructionsLayout);
        return container;
    }

    private Component createActionButtons() {
        VerticalLayout buttonContainer = new VerticalLayout();
        buttonContainer.setAlignItems(FlexComponent.Alignment.CENTER);
        buttonContainer.setSpacing(true);
        buttonContainer.setPadding(false);
        buttonContainer.setWidthFull();
        buttonContainer.addClassName("wams-confirmation-actions");

        // Primary button - Go to Login
        Button loginButton = new Button(
                I18n.t("auth.confirmation.loginButton"),
                VaadinIcon.ARROW_RIGHT.create()
        );
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.addClassName("wams-auth-primary-btn");
        loginButton.setWidthFull();
        loginButton.addClickListener(e -> UI.getCurrent().navigate("login"));

        // Secondary link - Resend email
        RouterLink resendLink = new RouterLink(
                I18n.t("auth.confirmation.resendLink"),
                RegisterView.class
        );
        resendLink.addClassName("wams-auth-confirmation-resend-link");

        buttonContainer.add(loginButton, resendLink);
        return buttonContainer;
    }

    private Div createDivider() {
        Div divider = new Div();
        divider.addClassName("wams-confirmation-divider");
        return divider;
    }
}