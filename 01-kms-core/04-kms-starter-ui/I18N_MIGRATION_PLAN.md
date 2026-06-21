# i18n Migration Plan - KMS Starter UI

## Overview
Ce document fournit un plan de migration pour implémenter l'i18n dans toutes les vues de l'application KMS Starter UI.

## Implementation Status

### Infrastructure (✅ COMPLETED)
- [x] I18nProvider - Fournisseur de traductions Vaadin
- [x] I18n Helper Class - Classe utilitaire pour accéder aux traductions
- [x] I18nUIHelper - Helper pour créer des composants avec i18n
- [x] LanguageSelectorComponent - Composant de sélection de langue
- [x] Resource Files (en, fr, es, de)
- [x] VaadinConfig - Configuration Vaadin
- [x] I18N_GUIDE.md - Documentation complète

### Views to Migrate

#### KMS Module Views

##### Token Configuration Views
- [x] **TokenConfigView** - DONE
  - Header, search, buttons, pagination, notifications, empty state

- [ ] **TokenConfigCard**
  - Title, labels, action buttons, tooltips
  - **Location**: `eu.isygoit.ui.kms.views.tokenizer.config.TokenConfigCard`

- [ ] **CreateTokenConfigDialog**
  - Dialog title, form labels, buttons, messages
  - **Location**: `eu.isygoit.ui.kms.views.tokenizer.config.dialog.CreateTokenConfigDialog`

- [ ] **UpdateTokenConfigDialog**
  - Dialog title, form labels, buttons, messages

- [ ] **DeleteTokenConfigDialog**
  - Confirmation message, buttons

- [ ] **TokenConfigDialogBase**
  - Base dialog components and labels

##### Password Configuration Views
- [ ] **PasswordConfigView**
- [ ] **PasswordConfigCard**
- [ ] **CreatePasswordConfigDialog**
- [ ] **UpdatePasswordConfigDialog**
- [ ] **DeletePasswordConfigDialog**

##### PEB Configuration Views
- [ ] **PEBConfigView**
- [ ] **PEBConfigCard**
- [ ] **CreatePEBConfigDialog**
- [ ] **UpdatePEBConfigDialog**
- [ ] **DeletePEBConfigDialog**

##### Digest Configuration Views
- [ ] **DigestConfigView**
- [ ] **DigestConfigCard**
- [ ] **CreateDigestConfigDialog**
- [ ] **UpdateDigestConfigDialog**
- [ ] **DeleteDigestConfigDialog**

##### Token Builder Views
- [ ] **TokenBuilderView**
- [ ] **ClaimsBuilderDialog**
- [ ] **DecodeJwtDialog**

##### Cryptography Views
- [ ] **CryptoOperationsView**
- [ ] **EncryptDecryptPanel**
- [ ] **SignVerifyPanel**
- [ ] **MacPanel**
- [ ] **DataKeyPanel**
- [ ] **CryptoPanelUtils**

##### Key Management Views
- [ ] **KeyManagementView**
- [ ] **KeyCard**
- [ ] **CreateKeyDialog**
- [ ] **UpdateKeyDialog**
- [ ] **DescribeKeyDialog**
- [ ] **RotateKeyConfirmDialog**
- [ ] **ScheduleKeyDeletionDialog**
- [ ] **CancelKeyDeletionDialog**
- [ ] **PermanentDeleteKeyDialog**
- [ ] **ToggleKeyStatusDialog**
- [ ] **ToggleRotationDialog**
- [ ] **EnableKeyVersionDialog**
- [ ] **DisableKeyVersionDialog**
- [ ] **ShowKeyVersionsDialog**
- [ ] **KeyDialogBase**

##### Key Alias Views
- [ ] **AliasesView**
- [ ] **AliasCard**
- [ ] **CreateAliasDialog**
- [ ] **UpdateAliasDialog**
- [ ] **DeleteAliasDialog**

##### Key Grants Views
- [ ] **GrantsView**
- [ ] **CreateGrantDialog**
- [ ] **GrantDetailsDialog**
- [ ] **RetireGrantDialog**
- [ ] **RevokeGrantDialog**

##### Key Policies Views
- [ ] **PoliciesView**
- [ ] **PolicyBuilderDialog**
- [ ] **PolicyStatementEditorDialog**

##### Key Store Views
- [ ] **CustomKeyStoresView**
- [ ] **StoreCard**
- [ ] **CreateCustomKeyStoreDialog**
- [ ] **UpdateCustomKeyStoreDialog**
- [ ] **DeleteCustomKeyStoreDialog**

##### Key Tags Views
- [ ] **TagsView**
- [ ] **AddTagDialog**

##### Random Key Views
- [ ] **RandomKeyView**
- [ ] **RandomKeyCard**
- [ ] **CreateRandomKeyDialog**
- [ ] **DeleteRandomKeyDialog**
- [ ] **RenewRandomKeyDialog**

##### Incremental Key Views
- [ ] **IncrementalKeyView**
- [ ] **NextCodeCard**
- [ ] **SubscribeDialog**
- [ ] **DeleteNextCodeDialog**

##### BYOK Views
- [ ] **ByokView**

##### Dashboard Views
- [ ] **KeyStatisticsPanel**
- [ ] **TokenStatisticsPanel**
- [ ] **KeyUsageStatsPanel**
- [ ] **AuditLogPanel**
- [ ] **StatCard**

#### IMS Module Views (Identity Management)
- [ ] **AccountManagementView**
- [ ] **AccountCard**
- [ ] **CreateAccountDialog**
- [ ] **UpdateAccountDialog**
- [ ] **DeleteAccountDialog**
- [ ] **AccountDetailsDialog**
- [ ] **EnableDisableAccountDialog**
- [ ] **ResetPasswordDialog**

- [ ] **CustomerManagementView**
- [ ] **CustomerCard**
- [ ] **CreateCustomerDialog**
- [ ] **UpdateCustomerDialog**
- [ ] **DeleteCustomerDialog**
- [ ] **CustomerDetailsDialog**
- [ ] **LinkCustomerAccountDialog**
- [ ] **ToggleCustomerStatusDialog**

- [ ] **TenantManagementView**
- [ ] **TenantCard**
- [ ] **CreateTenantDialog**
- [ ] **UpdateTenantDialog**
- [ ] **DeleteTenantDialog**
- [ ] **TenantDetailsDialog**
- [ ] **ToggleTenantStatusDialog**

- [ ] **RoleManagementView**
- [ ] **RoleCard**
- [ ] **CreateRoleDialog**
- [ ] **UpdateRoleDialog**
- [ ] **DeleteRoleDialog**
- [ ] **RoleDetailsDialog**

- [ ] **ApplicationManagementView**
- [ ] **ApplicationCard**
- [ ] **CreateApplicationDialog**
- [ ] **UpdateApplicationDialog**
- [ ] **DeleteApplicationDialog**
- [ ] **ApplicationDetailsDialog**
- [ ] **ToggleApplicationStatusDialog**

- [ ] **ParameterManagementView**
- [ ] **ParameterCard**
- [ ] **CreateParameterDialog**
- [ ] **UpdateParameterDialog**
- [ ] **DeleteParameterDialog**
- [ ] **ParameterDetailsDialog**

- [ ] **AnnexManagementView**
- [ ] **AnnexCard**
- [ ] **CreateAnnexDialog**
- [ ] **UpdateAnnexDialog**
- [ ] **DeleteAnnexDialog**
- [ ] **AnnexDetailsDialog**

#### Authentication Views
- [ ] **LoginView**
- [ ] **RegisterView**
- [ ] **PasswordView**
- [ ] **OtpLoginView**
- [ ] **QrCodeLoginView**

#### Common Components & Layouts
- [ ] **BaseMainLayout**
- [ ] **KmsMainLayout**
- [ ] **ImsMainLayout**
- [ ] **BaseCard**
- [ ] **BaseActionDialog**
- [ ] **NoActionDialog**
- [ ] **PinBaseActionDialog**
- [ ] **ImageCropperDialog**
- [ ] **ImageCropper**

#### Entry Points
- [ ] **IndexView**
- [ ] **KmsMainView**
- [ ] **ImsMainView**

## Migration Process

### For Each View

1. **Identify all user-facing strings**
   - Button labels
   - Page titles
   - Placeholders
   - Labels
   - Tooltips
   - Error messages
   - Success messages
   - Empty state messages

2. **Create i18n keys**
   - Follow naming conventions
   - Add keys to all resource files (en_US, fr_FR, es_ES, de_DE)

3. **Update the view**
   - Add import: `import eu.isygoit.i18n.I18n;`
   - Replace hardcoded strings with `I18n.t("key")`
   - Or use `I18nUIHelper` methods for components

4. **Test**
   - Verify all text appears correctly
   - Test with each language
   - Check for missing keys

### Example Migration

**Before:**
```java
public class MyDialog extends Dialog {
    public MyDialog() {
        setHeaderTitle("Create New Item");
        add(new TextField()); 
        // placeholders, etc.
    }
}
```

**After:**
```java
import eu.isygoit.i18n.I18n;

public class MyDialog extends Dialog {
    public MyDialog() {
        setHeaderTitle(I18n.t("dialog.create.title"));
        add(new TextField(I18n.t("dialog.form.name")));
        // placeholders, etc.
    }
}
```

## Translation Keys to Add

### View-Specific Keys Template

For each view, add these types of keys to all resource files:

```properties
# Page Title
title.myview=...

# Buttons
myview.button.create=...
myview.button.edit=...
myview.button.delete=...
myview.button.save=...

# Labels & Placeholders
myview.label.name=...
myview.placeholder.search=...

# Messages
myview.message.created=...
myview.message.updated=...
myview.message.deleted=...
myview.message.error=...

# Dialog
myview.dialog.create.title=...
myview.dialog.delete.confirmation=...

# Empty State
myview.empty.title=...
myview.empty.description=...

# Tooltips
myview.tooltip.refresh=...
myview.tooltip.create=...
```

## Priority Order

### Phase 1 (High Priority)
- All dialog components
- Authentication views
- Main layouts (add language selector)
- Error/Success notifications
- Pagination controls

### Phase 2 (Medium Priority)
- All configuration management views
- Card components
- Search and filter components

### Phase 3 (Lower Priority)
- Advanced cryptography views
- Statistics/Dashboard views
- Utility components

## Translation Checklist

- [ ] All English (en_US) translations complete
- [ ] All French (fr_FR) translations complete
- [ ] All Spanish (es_ES) translations complete
- [ ] All German (de_DE) translations complete
- [ ] No missing key warnings in application
- [ ] Language selector working correctly
- [ ] Page reloads correctly when language changes
- [ ] All views tested with multiple languages
- [ ] Documentation updated for new views

## Testing Checklist

- [ ] Switch between all languages
- [ ] Verify all text changes correctly
- [ ] Check UI layout is not broken by longer translations
- [ ] Test with German (longest translations)
- [ ] Test with special characters in non-English languages
- [ ] Verify page reload preserves application state
- [ ] Check that new keys have fallback values

## Notes

- Resource files are located in `src/main/resources/`
- I18n helper class is in `eu.isygoit.i18n.I18n`
- Use `I18n.t(key)` for simple translations
- Use `I18n.t(key, param1, param2)` for parameterized translations
- Always provide translations in all 4 languages

## Additional Resources

- See `I18N_GUIDE.md` for detailed usage instructions
- Reference `I18nUIHelper` for UI component helpers
- Check `TokenConfigView` for a complete implementation example

---

**Last Updated**: 2026-06-21
**Status**: Infrastructure Complete, Views Migration In Progress

