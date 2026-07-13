package eu.isygoit.ui.ims.views.profile;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.common.ChangePasswordRequestDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.ProfileService;
import org.springframework.http.ResponseEntity;

/**
 * Settings tab: change-password form with a live strength indicator.
 */
class ProfileSettingsPanel extends VerticalLayout {

    private final transient ProfileService profileService;

    ProfileSettingsPanel(ProfileService profileService) {
        this.profileService = profileService;

        setPadding(true);
        setSpacing(true);
        setWidthFull();
        addClassName("profile-tab-panel");

        Div card = new Div();
        card.addClassName("profile-section-card");

        H3 passwordTitle = new H3(I18n.t("profile.section.change.password"));
        passwordTitle.addClassName("section-title");

        Paragraph passwordDesc = new Paragraph(I18n.t("profile.password.description"));
        passwordDesc.addClassName(LumoUtility.TextColor.SECONDARY);

        FormLayout form = new FormLayout();
        form.addClassName("password-form");

        PasswordField currentPasswordField = new PasswordField(I18n.t("profile.field.current.password"));
        currentPasswordField.setWidthFull();
        currentPasswordField.setRequiredIndicatorVisible(true);

        PasswordField newPasswordField = new PasswordField(I18n.t("profile.field.new.password"));
        newPasswordField.setWidthFull();
        newPasswordField.setRequiredIndicatorVisible(true);

        PasswordField confirmPasswordField = new PasswordField(I18n.t("profile.field.confirm.password"));
        confirmPasswordField.setWidthFull();
        confirmPasswordField.setRequiredIndicatorVisible(true);

        ProgressBar strengthBar = new ProgressBar();
        strengthBar.setWidthFull();
        strengthBar.setValue(0);
        strengthBar.addClassName("password-strength-bar");

        Span strengthLabel = new Span(I18n.t("profile.password.strength.weak"));
        strengthLabel.addClassName("password-strength-label");

        newPasswordField.addValueChangeListener(e -> {
            int strength = calculatePasswordStrength(e.getValue());
            strengthBar.setValue(strength / 100.0);
            strengthLabel.setText(getStrengthText(strength));
            strengthBar.getStyle().set("--strength-color", getStrengthColor(strength));
        });

        HorizontalLayout strengthRow = new HorizontalLayout(strengthBar, strengthLabel);
        strengthRow.setAlignItems(FlexComponent.Alignment.CENTER);
        strengthRow.setWidthFull();
        strengthRow.setSpacing(true);

        Button savePasswordButton = new Button(I18n.t("profile.password.change"), VaadinIcon.LOCK.create());
        savePasswordButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        savePasswordButton.addClassName("profile-save-btn");
        savePasswordButton.addClickListener(e -> changePassword(currentPasswordField, newPasswordField, confirmPasswordField));

        form.add(currentPasswordField, newPasswordField, confirmPasswordField, strengthRow, savePasswordButton);
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2));

        card.add(passwordTitle, passwordDesc, form);
        add(card);
    }

    private void changePassword(PasswordField currentPasswordField, PasswordField newPasswordField, PasswordField confirmPasswordField) {
        String currentPwd = currentPasswordField.getValue();
        String newPwd = newPasswordField.getValue();
        String confirmPwd = confirmPasswordField.getValue();

        if (newPwd.isEmpty() || confirmPwd.isEmpty() || currentPwd.isEmpty()) {
            Notification.show(I18n.t("profile.password.fields.required"), 3000, Notification.Position.MIDDLE);
            return;
        }
        if (!newPwd.equals(confirmPwd)) {
            Notification.show(I18n.t("profile.password.mismatch"), 3000, Notification.Position.MIDDLE);
            return;
        }
        if (newPwd.length() < 8) {
            Notification.show(I18n.t("profile.password.min.length"), 3000, Notification.Position.MIDDLE);
            return;
        }

        try {
            ChangePasswordRequestDto request = ChangePasswordRequestDto.builder()
                    .currentPassword(currentPwd)
                    .newPassword(newPwd)
                    .confirmPassword(confirmPwd)
                    .build();

            ResponseEntity<Void> response = profileService.changePassword(request);

            if (response.getStatusCode().is2xxSuccessful()) {
                Notification.show(I18n.t("profile.password.success"), 3000, Notification.Position.MIDDLE);
                currentPasswordField.clear();
                newPasswordField.clear();
                confirmPasswordField.clear();
            }
        } catch (Exception e) {
            Notification.show(I18n.t("profile.password.error"), 3000, Notification.Position.MIDDLE);
        }
    }

    private int calculatePasswordStrength(String password) {
        int score = 0;
        if (password.length() >= 8) score += 25;
        if (password.length() >= 12) score += 15;
        if (password.matches(".*[A-Z].*")) score += 20;
        if (password.matches(".*[a-z].*")) score += 20;
        if (password.matches(".*\\d.*")) score += 10;
        if (password.matches(".*[!@#$%^&*()].*")) score += 10;
        return Math.min(score, 100);
    }

    private String getStrengthText(int strength) {
        if (strength < 30) return I18n.t("profile.password.strength.weak");
        if (strength < 60) return I18n.t("profile.password.strength.medium");
        if (strength < 80) return I18n.t("profile.password.strength.strong");
        return I18n.t("profile.password.strength.very.strong");
    }

    private String getStrengthColor(int strength) {
        if (strength < 30) return "var(--lumo-error-color)";
        if (strength < 60) return "var(--lumo-warning-color, var(--wams-warning-color))";
        return "var(--lumo-success-color)";
    }
}
