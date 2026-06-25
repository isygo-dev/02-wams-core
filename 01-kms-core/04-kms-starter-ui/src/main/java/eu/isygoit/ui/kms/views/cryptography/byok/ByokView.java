package eu.isygoit.ui.kms.views.cryptography.byok;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.enums.IEnumKeyExpirationModel;
import eu.isygoit.enums.IEnumKeyOrigin;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.kms.layout.KmsMainLayout;
import eu.isygoit.util.RsaEncryptionUtil;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@VaadinSessionScope
@Route(value = "kms/byok", layout = KmsMainLayout.class)
@PageTitle("BYOK - Bring Your Own Key")
@PermitAll
public class ByokView extends ManagementVerticalView {

    private final KmsApiService kmsApiService;
    private final ComboBox<KeyOption> keyCombo = new ComboBox<>(I18n.t("byok.view.select.key"));
    private final Button refreshKeysButton = new Button(new Icon(VaadinIcon.REFRESH));
    private final Button getParamsButton = new Button(I18n.t("byok.view.generate.params.button"), new Icon(VaadinIcon.DOWNLOAD));
    private final TextArea wrappedKeyDisplay = new TextArea();
    private final TextArea importTokenDisplay = new TextArea();
    private final TextArea plainKeyMaterialField = new TextArea();
    private final Button encryptNowButton = new Button(I18n.t("byok.view.encrypt.now.button"), new Icon(VaadinIcon.LOCK));
    private final TextArea encryptedMaterialField = new TextArea();
    private final DatePicker expirationDatePicker = new DatePicker(I18n.t("byok.view.expiration.date"));
    private final Button importButton = new Button(I18n.t("byok.view.import.button"), new Icon(VaadinIcon.UPLOAD));
    private final Button deleteMaterialButton = new Button(I18n.t("byok.view.delete.material.button"), new Icon(VaadinIcon.TRASH));
    private final ProgressBar loadingBar = new ProgressBar();
    private final Span paramsExpiryInfo = new Span();
    private final Span keyStatusInfo = new Span();

    private String selectedKeyId = null;
    private String currentImportToken = null;
    private LocalDateTime parametersValidUntil = null;
    private String currentPublicKey = null;
    private List<KeyOption> keyOptions = new ArrayList<>();
    private boolean hasImportedMaterial = false;

    @Autowired
    public ByokView(KmsApiService kmsApiService) {
        this.kmsApiService = kmsApiService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("kms-byok-view");
        setAlignItems(FlexComponent.Alignment.STRETCH);

        // Header
        H2 header = new H2(I18n.t("byok.view.title"));
        header.addClassName(LumoUtility.FontSize.XXLARGE);
        header.addClassName(LumoUtility.Margin.Bottom.SMALL);
        header.addClassName(LumoUtility.Margin.Top.NONE);
        add(header);

        // Step 1: Key selection + status + delete button
        add(createStepCard(I18n.t("byok.view.step1.title"),
                I18n.t("byok.view.step1.hint"),
                buildKeySelectionSection(),
                buildStatusAndDeleteRow()));

        // Step 2: Generate parameters
        VerticalLayout step2Content = new VerticalLayout();
        step2Content.setSpacing(false);
        step2Content.setPadding(false);
        step2Content.add(buildParamButtons());
        step2Content.add(createFieldRow(I18n.t("byok.view.wrapping.key.label"), wrappedKeyDisplay, I18n.t("byok.view.wrapping.key.copy.tooltip")));
        step2Content.add(createFieldRow(I18n.t("byok.view.import.token.label"), importTokenDisplay, I18n.t("byok.view.import.token.copy.tooltip")));
        step2Content.add(paramsExpiryInfo);
        add(createStepCard(I18n.t("byok.view.step2.title"),
                I18n.t("byok.view.step2.hint"),
                step2Content));

        // Step 3: Encrypt key material
        VerticalLayout step3Content = new VerticalLayout();
        step3Content.setSpacing(false);
        step3Content.setPadding(false);
        step3Content.add(createEncryptionSection());
        step3Content.add(createFieldRow(I18n.t("byok.view.encrypted.material.label"), encryptedMaterialField, I18n.t("byok.view.encrypted.material.copy.tooltip")));
        add(createStepCard(I18n.t("byok.view.step3.title"),
                I18n.t("byok.view.step3.hint"),
                step3Content));

        // Step 4: Import
        VerticalLayout step4Content = new VerticalLayout();
        step4Content.setSpacing(false);
        step4Content.setPadding(false);
        step4Content.add(expirationDatePicker);
        step4Content.add(buildImportSection());
        add(createStepCard(I18n.t("byok.view.step4.title"),
                I18n.t("byok.view.step4.hint"),
                step4Content));

        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        add(loadingBar);

        // Event handlers
        getParamsButton.addClickListener(e -> generateParameters());
        encryptNowButton.addClickListener(e -> encryptWithPublicKey());
        importButton.addClickListener(e -> importKeyMaterial());
        deleteMaterialButton.addClickListener(e -> deleteImportedMaterial());
        refreshKeysButton.addClickListener(e -> loadKeyOptions());
        keyCombo.addValueChangeListener(e -> {
            selectedKeyId = e.getValue() != null ? e.getValue().getKeyId() : null;
            clearParameters();
            updateKeyStatus();
        });

        configureComponents();
        injectResponsiveStyles();
        loadKeyOptions();
    }

    // ---------- UI Helpers ----------
    private VerticalLayout createStepCard(String title, String hint, Component... components) {
        VerticalLayout card = new VerticalLayout();
        card.setWidthFull();
        card.addClassName(LumoUtility.Border.ALL);
        card.addClassName(LumoUtility.BorderRadius.LARGE);
        card.addClassName(LumoUtility.Padding.MEDIUM);
        card.getStyle().set("margin-bottom", "var(--lumo-space-s)");
        card.getStyle().set("background-color", "var(--lumo-base-color)");

        H3 stepTitle = new H3(title);
        stepTitle.addClassName(LumoUtility.FontSize.MEDIUM);
        stepTitle.addClassName(LumoUtility.Margin.Top.NONE);
        stepTitle.addClassName(LumoUtility.Margin.Bottom.XSMALL);
        card.add(stepTitle);

        if (StringUtils.hasText(hint)) {
            Span hintSpan = new Span(hint);
            hintSpan.addClassName(LumoUtility.FontSize.XSMALL);
            hintSpan.addClassName(LumoUtility.TextColor.TERTIARY);
            hintSpan.getStyle().set("margin-bottom", "var(--lumo-space-s)");
            card.add(hintSpan);
        }

        for (Component comp : components) {
            card.add(comp);
        }
        return card;
    }

    private HorizontalLayout buildKeySelectionSection() {
        HorizontalLayout keyLayout = new HorizontalLayout();
        keyLayout.setWidthFull();
        keyLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        keyLayout.setSpacing(true);
        keyLayout.getStyle().set("flex-wrap", "wrap");

        keyCombo.setPlaceholder(I18n.t("byok.view.select.key"));
        keyCombo.setItemLabelGenerator(KeyOption::getDisplayName);
        keyCombo.setWidth("400px");
        keyCombo.setTooltipText(I18n.t("byok.view.select.key.tooltip"));

        refreshKeysButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshKeysButton.setTooltipText(I18n.t("byok.view.refresh.tooltip"));

        keyLayout.add(keyCombo, refreshKeysButton);
        return keyLayout;
    }

    private HorizontalLayout buildStatusAndDeleteRow() {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.getStyle().set("flex-wrap", "wrap");

        keyStatusInfo.addClassName(LumoUtility.FontSize.SMALL);
        keyStatusInfo.getStyle().set("margin-top", "var(--lumo-space-xs)");

        deleteMaterialButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteMaterialButton.setTooltipText(I18n.t("byok.view.delete.material.tooltip"));
        deleteMaterialButton.setVisible(false);

        row.add(keyStatusInfo, deleteMaterialButton);
        return row;
    }

    private HorizontalLayout buildParamButtons() {
        HorizontalLayout paramLayout = new HorizontalLayout(getParamsButton);
        paramLayout.setSpacing(true);
        getParamsButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getParamsButton.setTooltipText(I18n.t("byok.view.generate.params.tooltip"));
        return paramLayout;
    }

    private HorizontalLayout buildImportSection() {
        HorizontalLayout importLayout = new HorizontalLayout(importButton);
        importLayout.setWidthFull();
        importLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        importButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        importButton.setTooltipText(I18n.t("byok.view.import.tooltip"));
        return importLayout;
    }

    private VerticalLayout createEncryptionSection() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(false);

        layout.add(createPlainMaterialRow());

        HorizontalLayout buttonRow = new HorizontalLayout(encryptNowButton);
        buttonRow.setWidthFull();
        buttonRow.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        encryptNowButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        encryptNowButton.setTooltipText(I18n.t("byok.view.encrypt.now.tooltip"));
        layout.add(buttonRow);

        return layout;
    }

    private VerticalLayout createPlainMaterialRow() {
        VerticalLayout row = new VerticalLayout();
        row.setWidthFull();
        row.setSpacing(false);
        row.setPadding(false);

        HorizontalLayout headerRow = new HorizontalLayout();
        headerRow.setWidthFull();
        headerRow.setAlignItems(FlexComponent.Alignment.CENTER);
        headerRow.setSpacing(true);
        Span labelSpan = new Span(I18n.t("byok.view.plain.material.label"));
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.SMALL);
        headerRow.add(labelSpan);
        headerRow.getStyle().set("flex-wrap", "wrap");

        plainKeyMaterialField.setWidthFull();
        plainKeyMaterialField.setPlaceholder(I18n.t("byok.view.plain.material.placeholder"));
        plainKeyMaterialField.setHelperText(I18n.t("byok.view.plain.material.helper"));

        row.add(headerRow, plainKeyMaterialField);
        return row;
    }

    private VerticalLayout createFieldRow(String label, TextArea textArea, String copyTooltip) {
        VerticalLayout row = new VerticalLayout();
        row.setWidthFull();
        row.setSpacing(false);
        row.setPadding(false);

        HorizontalLayout headerRow = new HorizontalLayout();
        headerRow.setWidthFull();
        headerRow.setAlignItems(FlexComponent.Alignment.CENTER);
        headerRow.setSpacing(true);

        Span labelSpan = new Span(label);
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.SMALL);

        Button copyBtn = new Button(new Icon(VaadinIcon.COPY));
        copyBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        copyBtn.setTooltipText(copyTooltip);
        copyBtn.setWidth("32px");
        copyBtn.addClickListener(e -> {
            String value = textArea.getValue();
            if (StringUtils.hasText(value)) {
                copyToClipboard(value);
                showSuccessNotification(I18n.t("byok.view.copied"));
            } else {
                showWarningNotification(I18n.t("byok.view.nothing.to.copy"));
            }
        });

        headerRow.add(labelSpan, copyBtn);
        headerRow.getStyle().set("flex-wrap", "wrap");

        textArea.setWidthFull();
        textArea.setHeight("100px");

        row.add(headerRow, textArea);
        return row;
    }

    private void copyToClipboard(String text) {
        UI.getCurrent().getPage().executeJs(
                "navigator.clipboard.writeText($0).then(() => { " +
                        "  const notification = document.createElement('div'); " +
                        "  notification.textContent = $1; " +
                        "  notification.style.position = 'fixed'; " +
                        "  notification.style.bottom = '20px'; " +
                        "  notification.style.right = '20px'; " +
                        "  notification.style.backgroundColor = '#4caf50'; " +
                        "  notification.style.color = 'white'; " +
                        "  notification.style.padding = '10px 20px'; " +
                        "  notification.style.borderRadius = '4px'; " +
                        "  notification.style.zIndex = '1000'; " +
                        "  document.body.appendChild(notification); " +
                        "  setTimeout(() => notification.remove(), 2000); " +
                        "}).catch(() => { " +
                        "  const notification = document.createElement('div'); " +
                        "  notification.textContent = $2; " +
                        "  notification.style.position = 'fixed'; " +
                        "  notification.style.bottom = '20px'; " +
                        "  notification.style.right = '20px'; " +
                        "  notification.style.backgroundColor = '#f44336'; " +
                        "  notification.style.color = 'white'; " +
                        "  notification.style.padding = '10px 20px'; " +
                        "  notification.style.borderRadius = '4px'; " +
                        "  notification.style.zIndex = '1000'; " +
                        "  document.body.appendChild(notification); " +
                        "  setTimeout(() => notification.remove(), 3000); " +
                        "});", text, I18n.t("byok.view.copied"), I18n.t("byok.view.copy.failed"));
    }

    private void configureComponents() {
        int textAreaHeight = 100;
        wrappedKeyDisplay.setHeight(textAreaHeight + "px");
        wrappedKeyDisplay.setReadOnly(true);
        wrappedKeyDisplay.setPlaceholder(I18n.t("byok.view.wrapping.key.placeholder"));

        importTokenDisplay.setHeight(textAreaHeight + "px");
        importTokenDisplay.setReadOnly(true);
        importTokenDisplay.setPlaceholder(I18n.t("byok.view.import.token.placeholder"));

        encryptedMaterialField.setHeight(textAreaHeight + "px");
        encryptedMaterialField.setPlaceholder(I18n.t("byok.view.encrypted.material.placeholder"));
        encryptedMaterialField.setHelperText(I18n.t("byok.view.encrypted.material.helper"));

        expirationDatePicker.setWidth("250px");
        expirationDatePicker.setPlaceholder(I18n.t("byok.view.expiration.date.placeholder"));
        expirationDatePicker.setHelperText(I18n.t("byok.view.expiration.date.helper"));
    }

    // ---------- Data & API ----------
    private void loadKeyOptions() {
        showLoading(true);
        try {
            ResponseEntity<KmsDtos.ListKeysResponse> response = kmsApiService.listKeys(100, null);
            KmsDtos.ListKeysResponse keys = response.getBody();
            if (keys != null && keys.getKeys() != null) {
                keyOptions = keys.getKeys().stream()
                        .map(entry -> fetchAliasAndOrigin(entry.getKeyId()))
                        .filter(KeyOption::isExternal)
                        .collect(Collectors.toList());
                keyCombo.setItems(keyOptions);
            } else {
                keyOptions.clear();
                keyCombo.setItems(keyOptions);
            }
            if (selectedKeyId != null && keyOptions.stream().noneMatch(k -> k.getKeyId().equals(selectedKeyId))) {
                selectedKeyId = null;
                keyCombo.clear();
                clearParameters();
            }
            updateKeyStatus();
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            showErrorNotification(I18n.t("byok.view.load.keys.error", errorMsg));
            log.error("Failed to load keys: {}", errorMsg);
        } catch (Exception e) {
            showErrorNotification(I18n.t("byok.view.load.keys.error", e.getMessage()));
            log.error("Failed to load keys: {}", e.getMessage());
        } finally {
            showLoading(false);
        }
    }

    private KeyOption fetchAliasAndOrigin(String keyId) {
        try {
            ResponseEntity<KmsDtos.DescribeKeyResponse> response = kmsApiService.describeKey(keyId);
            KmsDtos.DescribeKeyResponse desc = response.getBody();
            if (desc != null && desc.getKeyMetadata() != null) {
                String alias = desc.getKeyMetadata().getKeyAlias();
                IEnumKeyOrigin.Types origin = desc.getKeyMetadata().getOrigin();
                boolean isExternal = origin == IEnumKeyOrigin.Types.EXTERNAL;
                return new KeyOption(keyId, alias, isExternal);
            }
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            showErrorNotification(I18n.t("byok.view.fetch.key.error", errorMsg));
            log.error("Error fetching key details for {}: {}", keyId, errorMsg);
        } catch (Exception e) {
            showErrorNotification(I18n.t("byok.view.fetch.key.details.error", keyId, e.getMessage()));
            log.error("Error fetching key details for {}: {}", keyId, e.getMessage());
        }
        return new KeyOption(keyId, keyId, false);
    }

    private void updateKeyStatus() {
        if (selectedKeyId == null) {
            keyStatusInfo.setText("");
            deleteMaterialButton.setVisible(false);
            hasImportedMaterial = false;
            return;
        }
        try {
            ResponseEntity<KmsDtos.DescribeKeyResponse> response = kmsApiService.describeKey(selectedKeyId);
            KmsDtos.DescribeKeyResponse desc = response.getBody();
            if (desc != null && desc.getKeyMetadata() != null) {
                hasImportedMaterial = desc.getKeyMetadata().getOrigin() == IEnumKeyOrigin.Types.EXTERNAL &&
                        desc.getKeyMetadata().getKeyStatus() != IEnumKeyStatus.Types.PENDING_IMPORT;
                String statusText = hasImportedMaterial ?
                        "✅ " + I18n.t("byok.view.key.status.imported") :
                        "⚠️ " + I18n.t("byok.view.key.status.pending");
                keyStatusInfo.setText(statusText);
                deleteMaterialButton.setVisible(hasImportedMaterial);
            }
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            showErrorNotification(I18n.t("byok.view.fetch.key.error", errorMsg));
            log.error("Error fetching key details for {}: {}", selectedKeyId, ex.getMessage());
        } catch (Exception e) {
            showErrorNotification(I18n.t("byok.view.fetch.key.details.error", selectedKeyId, e.getMessage()));
            log.error("Error fetching key details for {}: {}", selectedKeyId, e.getMessage());
            keyStatusInfo.setText("");
            deleteMaterialButton.setVisible(false);
            hasImportedMaterial = false;
        }
    }

    private void generateParameters() {
        if (selectedKeyId == null) {
            showWarningNotification(I18n.t("byok.view.params.select.key.first"));
            return;
        }
        if (hasImportedMaterial) {
            showWarningNotification(I18n.t("byok.view.params.has.material"));
            return;
        }
        showLoading(true);
        try {
            KmsDtos.GetParametersForImportRequest request = KmsDtos.GetParametersForImportRequest.builder()
                    .wrappingAlgorithm("RSAES_OAEP_SHA_256")
                    .wrappingKeySpec("RSA_2048")
                    .build();
            ResponseEntity<KmsDtos.GetParametersForImportResponse> response =
                    kmsApiService.getParametersForImport(selectedKeyId, request);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                KmsDtos.GetParametersForImportResponse params = response.getBody();
                currentPublicKey = params.getPublicKey();
                wrappedKeyDisplay.setValue(currentPublicKey);
                importTokenDisplay.setValue(params.getImportToken());
                currentImportToken = params.getImportToken();
                parametersValidUntil = params.getValidTo();

                String expiryMsg = parametersValidUntil != null ?
                        I18n.t("byok.view.params.expiry.info", DateHelper.formatToHumanReadable(parametersValidUntil)) :
                        I18n.t("byok.view.params.expiry.no.expiry");
                paramsExpiryInfo.setText(expiryMsg);
                paramsExpiryInfo.addClassName(LumoUtility.FontSize.XSMALL);
                paramsExpiryInfo.addClassName(LumoUtility.TextColor.TERTIARY);

                showSuccessNotification(I18n.t("byok.view.params.generated"));
            } else {
                showErrorNotification(I18n.t("byok.view.params.generate.failed"));
            }
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            showErrorNotification(I18n.t("byok.view.params.generate.error", errorMsg));
            log.error("Failed to generate import parameters for key {}: {}", selectedKeyId, errorMsg);
        } catch (Exception e) {
            showErrorNotification(I18n.t("byok.view.params.generate.error", e.getMessage()));
            log.error("Failed to generate import parameters for key {}: {}", selectedKeyId, e.getMessage());
        } finally {
            showLoading(false);
        }
    }

    private void encryptWithPublicKey() {
        String publicKey = wrappedKeyDisplay.getValue();
        if (!StringUtils.hasText(publicKey)) {
            showWarningNotification(I18n.t("byok.view.encrypt.generate.first"));
            return;
        }
        String plainBase64 = plainKeyMaterialField.getValue();
        if (!StringUtils.hasText(plainBase64)) {
            showWarningNotification(I18n.t("byok.view.encrypt.enter.material"));
            return;
        }
        if (!isValidBase64(plainBase64)) {
            showErrorNotification(I18n.t("byok.view.encrypt.invalid.base64"));
            return;
        }

        showLoading(true);
        try {
            String encryptedBase64 = RsaEncryptionUtil.encryptWithPublicKey(publicKey, plainBase64);
            encryptedMaterialField.setValue(encryptedBase64);
            showSuccessNotification(I18n.t("byok.view.encrypt.success"));
        } catch (Exception e) {
            showErrorNotification(I18n.t("byok.view.encrypt.failed", e.getMessage()));
        } finally {
            showLoading(false);
        }
    }

    private void importKeyMaterial() {
        if (selectedKeyId == null) {
            showWarningNotification(I18n.t("byok.view.import.select.key"));
            return;
        }
        if (hasImportedMaterial) {
            showWarningNotification(I18n.t("byok.view.import.has.material"));
            return;
        }
        String encrypted = encryptedMaterialField.getValue();
        if (!StringUtils.hasText(encrypted)) {
            showWarningNotification(I18n.t("byok.view.import.encrypted.required"));
            return;
        }
        if (!isValidBase64(encrypted)) {
            showErrorNotification(I18n.t("byok.view.import.invalid.base64"));
            return;
        }
        if (currentImportToken == null) {
            showWarningNotification(I18n.t("byok.view.import.generate.first"));
            return;
        }
        if (parametersValidUntil != null && parametersValidUntil.isBefore(LocalDateTime.now())) {
            showErrorNotification(I18n.t("byok.view.import.token.expired"));
            return;
        }

        showLoading(true);
        try {
            KmsDtos.ImportKeyMaterialRequest request = KmsDtos.ImportKeyMaterialRequest.builder()
                    .keyId(selectedKeyId)
                    .importToken(currentImportToken)
                    .encryptedKeyMaterial(encrypted)
                    .build();
            if (expirationDatePicker.getValue() != null) {
                LocalDateTime validTo = expirationDatePicker.getValue().atTime(LocalTime.MAX);
                request.setValidTo(validTo);
                request.setExpirationModel(IEnumKeyExpirationModel.Types.KEY_MATERIAL_EXPIRES);
            } else {
                request.setExpirationModel(IEnumKeyExpirationModel.Types.KEY_MATERIAL_DOES_NOT_EXPIRE);
            }
            ResponseEntity<KmsDtos.ImportKeyMaterialResponse> response = kmsApiService.importKeyMaterial(selectedKeyId, request);
            if (response.getStatusCode().is2xxSuccessful()) {
                showSuccessNotification(I18n.t("byok.view.import.success"));
                clearParameters();
                updateKeyStatus();
            } else {
                showErrorNotification(I18n.t("byok.view.import.failed"));
            }
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            showErrorNotification(I18n.t("byok.view.import.error", errorMsg));
            log.error("Import key material failed for {}: {}", selectedKeyId, errorMsg);
        } catch (Exception e) {
            showErrorNotification(I18n.t("byok.view.import.error", e.getMessage()));
            log.error("Import key material failed for {}: {}", selectedKeyId, e.getMessage());
        } finally {
            showLoading(false);
        }
    }

    private void deleteImportedMaterial() {
        if (selectedKeyId == null) {
            showWarningNotification(I18n.t("byok.view.delete.select.key"));
            return;
        }
        if (!hasImportedMaterial) {
            showWarningNotification(I18n.t("byok.view.delete.no.material"));
            return;
        }
        showLoading(true);
        try {
            ResponseEntity<KmsDtos.DeleteImportedKeyMaterialResponse> response =
                    kmsApiService.deleteImportedKeyMaterial(selectedKeyId);
            if (response.getStatusCode().is2xxSuccessful()) {
                showSuccessNotification(I18n.t("byok.view.delete.success"));
                clearParameters();
                updateKeyStatus();
            } else {
                showErrorNotification(I18n.t("byok.view.delete.failed"));
            }
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            showErrorNotification(I18n.t("byok.view.delete.error", errorMsg));
            log.error("Failed to delete imported key material for {}: {}", selectedKeyId, errorMsg);
        } catch (Exception e) {
            showErrorNotification(I18n.t("byok.view.delete.error", e.getMessage()));
            log.error("Failed to delete imported key material for {}: {}", selectedKeyId, e.getMessage());
        } finally {
            showLoading(false);
        }
    }

    private boolean isValidBase64(String str) {
        try {
            Base64.getDecoder().decode(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void clearParameters() {
        wrappedKeyDisplay.clear();
        importTokenDisplay.clear();
        plainKeyMaterialField.clear();
        encryptedMaterialField.clear();
        expirationDatePicker.clear();
        paramsExpiryInfo.setText("");
        currentImportToken = null;
        parametersValidUntil = null;
        currentPublicKey = null;
    }

    private void showLoading(boolean show) {
        loadingBar.setVisible(show);
        keyCombo.setEnabled(!show);
        refreshKeysButton.setEnabled(!show);
        getParamsButton.setEnabled(!show);
        encryptNowButton.setEnabled(!show);
        importButton.setEnabled(!show);
        deleteMaterialButton.setEnabled(!show);
        encryptedMaterialField.setEnabled(!show);
        plainKeyMaterialField.setEnabled(!show);
        expirationDatePicker.setEnabled(!show);
    }

    private void showSuccessNotification(String msg) {
        Notification.show(msg, 6000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showErrorNotification(String msg) {
        Notification.show(msg, 6000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void showWarningNotification(String msg) {
        Notification.show(msg, 6000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_WARNING);
    }

    private void injectResponsiveStyles() {
        String css = """
                .kms-byok-view {
                    background: linear-gradient(145deg, var(--lumo-primary-color-10pct), var(--lumo-base-color) 70%);
                    min-height: 100vh;
                    animation: fadeIn 0.5s ease-out;
                }
                @keyframes fadeIn {
                    from { opacity: 0; transform: translateY(20px); }
                    to { opacity: 1; transform: translateY(0); }
                }
                .kms-byok-view {
                    max-width: 1200px;
                    margin: 0 auto;
                }
                @media (max-width: 768px) {
                    .kms-byok-view .byok-field-row,
                    .kms-byok-view .byok-header-row {
                        flex-direction: column;
                        align-items: flex-start;
                    }
                }
                """;
        UI.getCurrent().getPage().executeJs(
                "const style = document.createElement('style'); style.textContent = $0; document.head.appendChild(style);",
                css
        );
    }

    private static class KeyOption {
        private final String keyId;
        private final String displayName;
        private final boolean isExternal;

        KeyOption(String keyId, String aliasOrId, boolean isExternal) {
            this.keyId = keyId;
            this.displayName = (aliasOrId != null && !aliasOrId.equals(keyId)) ? aliasOrId + " (" + keyId + ")" : keyId;
            this.isExternal = isExternal;
        }

        String getKeyId() {
            return keyId;
        }

        String getDisplayName() {
            return displayName;
        }

        boolean isExternal() {
            return isExternal;
        }
    }
}