package eu.isygoit.ui.common.profile;

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
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.api.ProfileServiceApi;
import eu.isygoit.dto.common.ChangePasswordRequestDto;
import eu.isygoit.dto.request.ChangeAuthTypeRequestDto;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.common.dialog.DialogLayout;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Settings tab: change-password form with a live strength indicator, and the authentication method switch.
 */
class ProfileSettingsPanel extends VerticalLayout {

    private static final List<IEnumAuth.Types> SWITCHABLE =
            List.of(IEnumAuth.Types.PWD, IEnumAuth.Types.OTP, IEnumAuth.Types.QRC);

    private final transient ProfileServiceApi profileService;
    private final transient Consumer<IEnumAuth.Types> onAuthTypeChanged;
    private final Div authMethodHost = new Div();
    private transient IEnumAuth.Types currentAuthType;

    ProfileSettingsPanel(ProfileServiceApi profileService, IEnumAuth.Types currentAuthType,
                         Consumer<IEnumAuth.Types> onAuthTypeChanged) {
        this.profileService = profileService;
        this.currentAuthType = currentAuthType;
        this.onAuthTypeChanged = onAuthTypeChanged;

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
        strengthBar.addClassName("password-strength-bar--weak");

        Span strengthLabel = new Span(I18n.t("profile.password.strength.weak"));
        strengthLabel.addClassName("password-strength-label");

        newPasswordField.addValueChangeListener(e -> {
            int strength = calculatePasswordStrength(e.getValue());
            strengthBar.setValue(strength / 100.0);
            strengthLabel.setText(getStrengthText(strength));
            strengthBar.removeClassNames("password-strength-bar--weak",
                    "password-strength-bar--medium", "password-strength-bar--strong");
            strengthBar.addClassName(getStrengthClass(strength));
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

        authMethodHost.setWidthFull();
        add(authMethodHost);
        renderAuthMethodCard();
    }

    // ------------------------------------------------------------------------
    // Authentication method
    // ------------------------------------------------------------------------

    private void renderAuthMethodCard() {
        authMethodHost.removeAll();

        Div card = new Div();
        card.addClassName("profile-section-card");

        H3 title = new H3(I18n.t("profile.section.auth.method"));
        title.addClassName("section-title");
        Paragraph description = new Paragraph(I18n.t("profile.authmethod.description"));
        description.addClassName(LumoUtility.TextColor.SECONDARY);
        card.add(title, description);

        if (!SWITCHABLE.contains(currentAuthType)) {
            // Unknown or token mode: nothing to switch from here.
            card.add(new Paragraph(I18n.t("profile.authmethod.current", authTypeLabel(currentAuthType))),
                    new Paragraph(I18n.t("profile.authmethod.unsupported")));
            authMethodHost.add(card);
            return;
        }

        Paragraph current = new Paragraph(I18n.t("profile.authmethod.current", authTypeLabel(currentAuthType)));
        current.addClassName("profile-auth-current");

        List<IEnumAuth.Types> targets = SWITCHABLE.stream().filter(type -> type != currentAuthType).toList();
        Select<IEnumAuth.Types> targetSelect = new Select<>();
        targetSelect.setLabel(I18n.t("profile.authmethod.field.target"));
        targetSelect.setItems(targets);
        targetSelect.setItemLabelGenerator(this::authTypeLabel);
        targetSelect.setValue(targets.get(0));
        targetSelect.setWidthFull();

        // The credential is checked against the current method; QR accounts confirm with the account password
        // because a QR code cannot be typed.
        boolean otpMode = currentAuthType == IEnumAuth.Types.OTP;
        PasswordField credentialField = new PasswordField(otpMode
                ? I18n.t("profile.authmethod.field.code")
                : I18n.t("profile.field.current.password"));
        credentialField.setWidthFull();
        credentialField.setRequiredIndicatorVisible(true);
        if (otpMode) {
            credentialField.setRevealButtonVisible(false);
        }
        if (currentAuthType == IEnumAuth.Types.QRC) {
            credentialField.setHelperText(I18n.t("profile.authmethod.qr.reauth.hint"));
        }

        Button changeButton = new Button(I18n.t("profile.authmethod.button.change"), VaadinIcon.SHIELD.create());
        changeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        changeButton.addClassName("profile-save-btn");
        changeButton.addClickListener(e -> changeAuthType(targetSelect.getValue(), credentialField, changeButton));

        FormLayout form = new FormLayout();
        form.addClassName("auth-method-form");
        form.add(targetSelect, credentialField);
        if (otpMode) {
            Button sendCodeButton = new Button(I18n.t("profile.authmethod.button.sendCode"), VaadinIcon.ENVELOPE.create());
            sendCodeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            sendCodeButton.addClickListener(e -> sendCode(sendCodeButton));
            form.add(sendCodeButton);
        }
        form.add(changeButton);
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2));

        card.add(current, form);
        authMethodHost.add(card);
    }

    private void sendCode(Button sendCodeButton) {
        sendCodeButton.setEnabled(false);
        try {
            ResponseEntity<Boolean> response = profileService.sendReauthenticationCode();
            if (response.getStatusCode().is2xxSuccessful() && Boolean.TRUE.equals(response.getBody())) {
                Notification.show(I18n.t("profile.authmethod.code.sent"), 3000, Notification.Position.BOTTOM_END);
            } else {
                Notification.show(I18n.t("profile.authmethod.error.generic"), 3000, Notification.Position.BOTTOM_END);
            }
        } catch (Exception e) {
            Notification.show(I18n.t("profile.authmethod.error.generic"), 3000, Notification.Position.BOTTOM_END);
        } finally {
            sendCodeButton.setEnabled(true);
        }
    }

    private void changeAuthType(IEnumAuth.Types target, PasswordField credentialField, Button changeButton) {
        String credential = credentialField.getValue();
        if (credential.isBlank()) {
            Notification.show(I18n.t("profile.authmethod.credential.required"), 3000, Notification.Position.BOTTOM_END);
            return;
        }
        if (target == null || target == currentAuthType) {
            Notification.show(I18n.t("profile.authmethod.same"), 3000, Notification.Position.BOTTOM_END);
            return;
        }

        if (target == IEnumAuth.Types.QRC) {
            // Switching to QR login can lock the user out when no signed-in device is at hand: confirm first.
            new QrModeConfirmDialog(() -> applyAuthTypeChange(target, credential),
                    () -> authTypeChanged(target)).open();
            return;
        }

        changeButton.setEnabled(false);
        String errorKey = applyAuthTypeChange(target, credential);
        changeButton.setEnabled(true);
        credentialField.clear();
        if (errorKey == null) {
            Notification.show(I18n.t("profile.authmethod.success"), 3000, Notification.Position.BOTTOM_END);
            authTypeChanged(target);
        } else {
            Notification.show(I18n.t(errorKey), 4000, Notification.Position.BOTTOM_END);
        }
    }

    /**
     * Calls the server.
     *
     * @return null on success, otherwise the message key describing the failure
     */
    private String applyAuthTypeChange(IEnumAuth.Types target, String credential) {
        try {
            ResponseEntity<Boolean> response = profileService.changeAuthType(ChangeAuthTypeRequestDto.builder()
                    .authType(target)
                    .currentCredential(credential)
                    .build());
            if (response.getStatusCode().is2xxSuccessful() && Boolean.TRUE.equals(response.getBody())) {
                return null;
            }
            return "profile.authmethod.error.generic";
        } catch (FeignException ex) {
            return switch (ex.status()) {
                case 401 -> "profile.authmethod.error.credential";
                case 403 -> "profile.authmethod.error.forbidden";
                default -> "profile.authmethod.error.generic";
            };
        } catch (Exception ex) {
            return "profile.authmethod.error.generic";
        }
    }

    private void authTypeChanged(IEnumAuth.Types target) {
        currentAuthType = target;
        onAuthTypeChanged.accept(target);
        renderAuthMethodCard();
    }

    private String authTypeLabel(IEnumAuth.Types type) {
        return type == null ? "-" : I18n.t("profile.authmethod.type." + type.name());
    }

    private void changePassword(PasswordField currentPasswordField, PasswordField newPasswordField, PasswordField confirmPasswordField) {
        String currentPwd = currentPasswordField.getValue();
        String newPwd = newPasswordField.getValue();
        String confirmPwd = confirmPasswordField.getValue();

        if (newPwd.isEmpty() || confirmPwd.isEmpty() || currentPwd.isEmpty()) {
            Notification.show(I18n.t("profile.password.fields.required"), 3000, Notification.Position.BOTTOM_END);
            return;
        }
        if (!newPwd.equals(confirmPwd)) {
            Notification.show(I18n.t("profile.password.mismatch"), 3000, Notification.Position.BOTTOM_END);
            return;
        }
        if (newPwd.length() < 8) {
            Notification.show(I18n.t("profile.password.min.length"), 3000, Notification.Position.BOTTOM_END);
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
                Notification.show(I18n.t("profile.password.success"), 3000, Notification.Position.BOTTOM_END);
                currentPasswordField.clear();
                newPasswordField.clear();
                confirmPasswordField.clear();
            }
        } catch (Exception e) {
            Notification.show(I18n.t("profile.password.error"), 3000, Notification.Position.BOTTOM_END);
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

    private String getStrengthClass(int strength) {
        if (strength < 30) return "password-strength-bar--weak";
        if (strength < 60) return "password-strength-bar--medium";
        return "password-strength-bar--strong";
    }

    /**
     * Basic confirmation (no form, no tabs) shown before enabling QR login.
     */
    private static final class QrModeConfirmDialog extends BaseActionDialog {

        private final transient Supplier<String> action;

        QrModeConfirmDialog(Supplier<String> action, Runnable onSuccess) {
            super(I18n.t("profile.authmethod.qr.confirm.title"), onSuccess);
            this.action = action;
            DialogLayout.size(this, DialogLayout.WIDTH_S);
            setOkButtonText(I18n.t("profile.authmethod.qr.confirm.ok"));
            addContent(new Paragraph(I18n.t("profile.authmethod.qr.confirm.message")));
        }

        @Override
        protected boolean onOk() {
            String errorKey = action.get();
            if (errorKey == null) {
                append(I18n.t("profile.authmethod.success"));
                return true;
            }
            append(I18n.t(errorKey));
            return false;
        }
    }
}
