package eu.isygoit.ui.kms.views.cryptography.keyStore.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.kms.views.common.KmsActionDialog;
import eu.isygoit.ui.kms.views.cryptography.keyStore.CustomKeyStoresView;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared form of the custom key store create/update dialogs. Every field of
 * {@code CreateCustomKeyStoreRequest} / {@code UpdateCustomKeyStoreRequest} is
 * declared and laid out here once (identity, CloudHSM, XKS, connection settings,
 * metadata); subclasses only add their specifics and send the request.
 *
 * <p>Subclasses must call {@link #buildForm()} at the end of their constructor.
 */
abstract class AbstractCustomKeyStoreFormDialog extends KmsActionDialog {

    final CustomKeyStoresView parentView;
    final KmsApiService kmsApiService;

    /** i18n prefix of the field labels (create and update dialogs use different wording). */
    private final String fieldKeyPrefix;

    TextField nameField;
    TextField cloudHsmClusterId;
    PasswordField keyStorePassword;
    TextArea trustAnchorCert;
    TextField xksProxyUriEndpoint;
    TextField xksProxyUriPath;
    PasswordField xksProxyAuth;
    TextField xksProxyConnectivity;
    IntegerField maxKeysField;
    IntegerField timeoutField;
    IntegerField healthIntervalField;
    Checkbox autoReconnectCheck;
    TextArea metadataField;
    TextArea tagsField;
    TextArea typeSpecificDataField;

    private VerticalLayout cloudHsmSection;
    private VerticalLayout xksSection;

    AbstractCustomKeyStoreFormDialog(String title,
                                     Runnable onSuccess,
                                     CustomKeyStoresView parentView,
                                     KmsApiService kmsApiService,
                                     String fieldKeyPrefix) {
        super(title, onSuccess);
        this.parentView = parentView;
        this.kmsApiService = kmsApiService;
        this.fieldKeyPrefix = fieldKeyPrefix;
    }

    /** Components shown in the identity section after the name field (e.g. the type selector). */
    abstract Component[] additionalIdentityFields();

    /** Sets placeholders, helper texts, initial values and listeners once the fields exist. */
    abstract void decorateFields();

    final void buildForm() {
        DialogLayout.size(this, DialogLayout.WIDTH_M);

        createFields();
        Component[] identityExtras = additionalIdentityFields();
        decorateFields();

        VerticalLayout root = DialogLayout.stack();
        root.add(buildIdentitySection(identityExtras), buildCloudHsmSection(), buildXksSection(),
                buildSettingsSection(), buildMetadataSection());
        add(root);
    }

    /** Shows the CloudHSM or the XKS section (the other one is hidden). */
    final void showTypeSections(boolean cloudHsm) {
        cloudHsmSection.setVisible(cloudHsm);
        xksSection.setVisible(!cloudHsm);
    }

    private String label(String suffix) {
        return I18n.t(fieldKeyPrefix + suffix);
    }

    private void createFields() {
        nameField = new TextField(label("name"));
        nameField.setWidthFull();

        cloudHsmClusterId = new TextField(label("cloudhsm.cluster"));
        keyStorePassword = new PasswordField(label("password"));
        trustAnchorCert = DialogLayout.tall(new TextArea(label("certificate")));

        xksProxyUriEndpoint = new TextField(label("xks.endpoint"));
        xksProxyUriPath = new TextField(label("xks.path"));
        xksProxyAuth = new PasswordField(label("xks.auth"));
        xksProxyConnectivity = new TextField(label("xks.connectivity"));

        maxKeysField = new IntegerField(label("max.keys"));
        maxKeysField.setMin(1);
        maxKeysField.setMax(10000);
        timeoutField = new IntegerField(label("timeout"));
        timeoutField.setMin(1);
        healthIntervalField = new IntegerField(label("health"));
        healthIntervalField.setMin(10);
        autoReconnectCheck = new Checkbox(label("auto.reconnect"));

        metadataField = DialogLayout.tall(new TextArea(label("metadata")));
        tagsField = DialogLayout.tall(new TextArea(label("tags")));
        typeSpecificDataField = DialogLayout.tall(new TextArea(label("type.specific")));
    }

    private VerticalLayout buildIdentitySection(Component[] identityExtras) {
        VerticalLayout section = DialogLayout.section(
                I18n.t("kms.keystore.details.section.identity"), VaadinIcon.DATABASE);
        FormLayout form = DialogLayout.responsiveForm();
        form.add(nameField);
        form.add(identityExtras);
        section.add(form);
        return section;
    }

    private VerticalLayout buildCloudHsmSection() {
        cloudHsmSection = DialogLayout.section(
                I18n.t("kms.keystore.details.section.cloudhsm"), VaadinIcon.CLOUD);
        FormLayout form = DialogLayout.responsiveForm();
        form.add(cloudHsmClusterId, keyStorePassword, trustAnchorCert);
        form.setColspan(trustAnchorCert, 2);
        cloudHsmSection.add(form);
        return cloudHsmSection;
    }

    private VerticalLayout buildXksSection() {
        xksSection = DialogLayout.section(
                I18n.t("kms.keystore.details.section.xks"), VaadinIcon.LINK);
        FormLayout form = DialogLayout.responsiveForm();
        form.add(xksProxyUriEndpoint, xksProxyUriPath, xksProxyAuth, xksProxyConnectivity);
        xksSection.add(form);
        return xksSection;
    }

    private VerticalLayout buildSettingsSection() {
        VerticalLayout section = DialogLayout.section(
                I18n.t("kms.keystore.details.section.settings"), VaadinIcon.COG);
        FormLayout form = DialogLayout.responsiveForm();
        form.add(maxKeysField, timeoutField, healthIntervalField, autoReconnectCheck);
        section.add(form);
        return section;
    }

    private VerticalLayout buildMetadataSection() {
        VerticalLayout section = DialogLayout.section(
                I18n.t("kms.keystore.details.section.metadata"), VaadinIcon.TAGS);
        FormLayout form = DialogLayout.responsiveForm();
        form.add(metadataField, tagsField, typeSpecificDataField);
        form.setColspan(metadataField, 2);
        form.setColspan(tagsField, 2);
        form.setColspan(typeSpecificDataField, 2);
        section.add(form);
        return section;
    }

    /** Parses a flat {@code {"key":"value", ...}} text into a map (null when empty). */
    final Map<String, String> parseJsonToMap(String json) {
        if (!StringUtils.hasText(json)) return null;
        if ("null".equalsIgnoreCase(json.trim())) return null;
        Map<String, String> map = new HashMap<>();
        try {
            String stripped = json.trim();
            if (stripped.startsWith("{") && stripped.endsWith("}")) {
                stripped = stripped.substring(1, stripped.length() - 1);
            }
            if (stripped.isEmpty()) return null;
            for (String pair : stripped.split(",")) {
                if (pair.trim().isEmpty()) continue;
                String[] kv = pair.split(":", 2);
                if (kv.length == 2) {
                    String key = kv[0].trim().replace("\"", "");
                    String value = kv[1].trim().replace("\"", "");
                    if (!key.isEmpty()) map.put(key, value);
                }
            }
        } catch (Exception e) {
            append(I18n.t("kms.keystore.dialog.error.invalid.json", e.getMessage()));
            return null;
        }
        return map.isEmpty() ? null : map;
    }
}
