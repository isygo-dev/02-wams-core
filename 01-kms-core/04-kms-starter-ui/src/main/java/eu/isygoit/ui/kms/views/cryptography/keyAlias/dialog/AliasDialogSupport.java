package eu.isygoit.ui.kms.views.cryptography.keyAlias.dialog;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import eu.isygoit.dto.KmsDtos.DescribeKeyResponse;
import eu.isygoit.dto.KmsDtos.ListKeysResponse;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Shared pieces of the alias create/update dialogs: the key id lookup and the
 * target-key combo box (previously duplicated in both dialogs).
 */
@Slf4j
final class AliasDialogSupport {

    private AliasDialogSupport() {
    }

    /** Loads the ids of the first 100 keys; shows an error notification on failure. */
    static List<String> fetchKeyIds(KmsApiService kmsApiService) {
        List<String> keyIds = new ArrayList<>();
        try {
            ResponseEntity<ListKeysResponse> response = kmsApiService.listKeys(100, null);
            ListKeysResponse keys = response.getBody();
            if (keys != null && keys.getKeys() != null) {
                keyIds = keys.getKeys().stream()
                        .map(ListKeysResponse.KeyEntry::getKeyId)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            Notification.show(I18n.t("kms.aliases.view.load.keys.error", e.getMessage()), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
        return keyIds;
    }

    /** Required target-key combo whose labels read "alias (keyId)" when the key has an alias. */
    static ComboBox<String> createTargetKeyCombo(KmsApiService kmsApiService, List<String> keyIds) {
        ComboBox<String> combo = new ComboBox<>(I18n.t("kms.alias.dialog.field.target.key"));
        combo.setRequiredIndicatorVisible(true);
        combo.setWidthFull();
        combo.setPlaceholder(I18n.t("kms.alias.dialog.field.target.key.placeholder"));
        combo.setItems(keyIds);
        combo.setItemLabelGenerator(keyId -> {
            try {
                ResponseEntity<DescribeKeyResponse> desc = kmsApiService.describeKey(keyId);
                DescribeKeyResponse descBody = desc.getBody();
                if (descBody != null && descBody.getKeyMetadata() != null) {
                    String alias = descBody.getKeyMetadata().getKeyAlias();
                    if (alias != null && !alias.isEmpty()) return alias + " (" + keyId + ")";
                }
            } catch (Exception e) {
                log.error("Failed to fetch key metadata for keyId: {}", keyId, e);
            }
            return keyId;
        });
        return combo;
    }
}
