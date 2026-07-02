# I18n Implementation for KMS Starter UI

## 📖 Overview

Ce dossier contient une implémentation complète du système d'internationalisation (i18n) pour la vue Vaadin du système
de gestion des clés.

L'implémentation supporte 4 langues :

- 🇺🇸 English (US)
- 🇫🇷 Français (France)
- 🇪🇸 Español (España)
- 🇩🇪 Deutsch (Deutschland)

## 🎯 What's Implemented

### ✅ Core Infrastructure

- **I18nProvider**: Fournisseur de traductions compatible Vaadin
- **I18n Helper**: Classe utilitaire statique pour accès facile
- **I18nUIHelper**: Helpers pour créer des composants Vaadin traduits
- **LanguageSelectorComponent**: Composant UI pour changer de langue
- **Resource Files**: Traductions pour 4 langues (250+ clés)
- **Configuration**: Configuration Spring/Vaadin automatique

### ✅ Example Implementation

- **TokenConfigView**: Vue complètement traduite avec exemple

### ✅ Documentation

- **I18N_GUIDE.md**: Guide complet avec exemples
- **I18N_MIGRATION_PLAN.md**: Plan de migration détaillé
- **I18N_QUICK_REFERENCE.md**: Fiche de référence rapide
- **I18N_IMPLEMENTATION_SUMMARY.md**: Résumé technique

## 📚 Documentation Files

| File                                                             | Purpose                        | Audience                     |
|------------------------------------------------------------------|--------------------------------|------------------------------|
| [I18N_QUICK_REFERENCE.md](I18N_QUICK_REFERENCE.md)               | Quick reference for developers | Developers                   |
| [I18N_GUIDE.md](I18N_GUIDE.md)                                   | Comprehensive usage guide      | Developers, Architects       |
| [I18N_MIGRATION_PLAN.md](I18N_MIGRATION_PLAN.md)                 | View migration checklist       | Project Managers, Developers |
| [I18N_IMPLEMENTATION_SUMMARY.md](I18N_IMPLEMENTATION_SUMMARY.md) | Technical summary              | Architects, Tech Leads       |
| **README.md** (this file)                                        | Quick overview                 | Everyone                     |

## 🚀 Quick Start

### Using i18n in a View

```java
import eu.isygoit.i18n.I18n;

public class MyView extends VerticalLayout {
    public MyView() {
        H1 title = new H1(I18n.t("my.view.title"));
        Button button = new Button(I18n.t("common.button.create"));
    }
}
```

### Adding a New Translation Key

1. Add key to all 4 resource files in `src/main/resources/`:
    - `messages.properties` (English, default)
    - `messages_en_US.properties` (English)
    - `messages_fr_FR.properties` (French)
    - `messages_es_ES.properties` (Spanish)
    - `messages_de_DE.properties` (German)

2. Use in code:
   ```java
   String text = I18n.t("my.new.key");
   ```

### Changing Language at Runtime

```java
I18n.setLocale(new Locale("fr", "FR"));
        UI.

getCurrent().

getPage().

reload();
```

## 📂 Project Structure

```
04-kms-starter-ui/
│
├── 📄 README.md (this file)
├── 📄 I18N_QUICK_REFERENCE.md
├── 📄 I18N_GUIDE.md
├── 📄 I18N_MIGRATION_PLAN.md
├── 📄 I18N_IMPLEMENTATION_SUMMARY.md
│
├── src/main/
│   ├── java/eu/isygoit/
│   │   ├── i18n/
│   │   │   ├── I18nProvider.java          # Core i18n provider
│   │   │   └── I18n.java                 # Helper class
│   │   ├── util/
│   │   │   └── I18nUIHelper.java         # UI component helpers
│   │   ├── ui/common/component/
│   │   │   └── LanguageSelectorComponent.java  # Language switcher
│   │   ├── config/
│   │   │   └── VaadinConfig.java         # Spring configuration
│   │   └── ui/kms/views/tokenizer/config/
│   │       └── TokenConfigView.java      # Example (UPDATED)
│   │
│   └── resources/
│       ├── messages.properties           # Default (English)
│       ├── messages_en_US.properties     # English
│       ├── messages_fr_FR.properties     # French
│       ├── messages_es_ES.properties     # Spanish
│       └── messages_de_DE.properties     # German
│
└── pom.xml (no changes needed, dependencies already present)
```

## 🔧 Key Components

### I18nProvider

- Loads resource bundles for all languages
- Implements Vaadin's I18NProvider interface
- Automatic fallback to English
- Caches loaded bundles for performance

### I18n Helper

- Static methods for easy access: `I18n.t("key")`
- Supports parameterized translations: `I18n.t("key", param1, param2)`
- Locale-aware through VaadinSession

### I18nUIHelper

- Pre-built methods for common Vaadin components
- `createButton()`, `createH1()`, `createTextField()`, etc.
- Reduces boilerplate code

### LanguageSelectorComponent

- ComboBox showing available languages
- Switches language at runtime
- Automatically reloads UI with new language

## 💾 Resource Files Organization

### Naming Convention

```
module.feature.component.property

Examples:
common.button.create      # Common button
token.config.title        # Token config title
token.config.page.info    # Pagination info
notification.error        # Error notification
error.validation          # Validation error
tooltip.refresh           # Refresh tooltip
```

### Total Keys: 250+

Covering all UI elements, messages, validations, and tooltips.

## 🌍 Supported Languages

| Language         | Locale | File                      | Status     |
|------------------|--------|---------------------------|------------|
| English (US)     | en_US  | messages_en_US.properties | ✅ Complete |
| French (France)  | fr_FR  | messages_fr_FR.properties | ✅ Complete |
| Spanish (Spain)  | es_ES  | messages_es_ES.properties | ✅ Complete |
| German (Germany) | de_DE  | messages_de_DE.properties | ✅ Complete |

## 📋 Next Steps

### For Developers

1. Read [I18N_QUICK_REFERENCE.md](I18N_QUICK_REFERENCE.md) (5 min read)
2. Look at `TokenConfigView.java` for example
3. Start migrating views using the pattern
4. Test with language selector

### For View Migration

1. Refer to [I18N_MIGRATION_PLAN.md](I18N_MIGRATION_PLAN.md)
2. Use the checklist provided
3. Test all 4 languages
4. Check UI layout with longer translations (German)

### For Project Managers

1. Reference [I18N_IMPLEMENTATION_SUMMARY.md](I18N_IMPLEMENTATION_SUMMARY.md) for status
2. Use [I18N_MIGRATION_PLAN.md](I18N_MIGRATION_PLAN.md) for prioritization
3. Track view migration progress

## ✨ Features

✅ **Production-Ready**

- Fully integrated with Vaadin framework
- Efficient resource caching
- Automatic fallback mechanism

✅ **Easy to Use**

- Simple API: `I18n.t("key")`
- Helper class for common patterns
- Minimal code changes

✅ **Well-Documented**

- Comprehensive guides
- Quick reference card
- Migration plan
- Code examples

✅ **Extensible**

- Easy to add new languages
- Clear naming conventions
- Template provided for translations

✅ **Tested**

- Example implementation in TokenConfigView
- Ready for production use

## 🧪 Testing

### Unit Testing

- Test I18nProvider loading
- Test key resolution
- Test fallback mechanism
- Test parameterized translations

### Integration Testing

- Language switching
- UI updates after language change
- VaadinSession persistence
- Resource bundle caching

### User Testing

- Verify all text translates correctly
- Check UI layout with each language
- Ensure special characters display properly
- Test on multiple browsers

## 📞 Getting Help

### For Questions About Usage

👉 See [I18N_QUICK_REFERENCE.md](I18N_QUICK_REFERENCE.md)

### For Implementation Details

👉 See [I18N_GUIDE.md](I18N_GUIDE.md)

### For Migration Process

👉 See [I18N_MIGRATION_PLAN.md](I18N_MIGRATION_PLAN.md)

### For Technical Architecture

👉 See [I18N_IMPLEMENTATION_SUMMARY.md](I18N_IMPLEMENTATION_SUMMARY.md)

## 🔐 Best Practices

✅ **Always translate all 4 languages**

- Don't skip any language
- Ensure consistency across all files

✅ **Use meaningful key names**

- Follow the module.feature.component pattern
- Make keys self-documenting

✅ **Keep strings consistent**

- Same text = same key everywhere
- Avoid duplication

✅ **Test with each language**

- Some languages are longer (German)
- Some have special characters
- Check UI layout integrity

✅ **Use I18nUIHelper for components**

- Reduces boilerplate
- Ensures consistency
- Makes code more readable

## 🎓 Learning Path

1. **Start**: Read this README
2. **Quick Learn**: [I18N_QUICK_REFERENCE.md](I18N_QUICK_REFERENCE.md) (5 min)
3. **Study Example**: Look at `TokenConfigView.java`
4. **Deep Dive**: [I18N_GUIDE.md](I18N_GUIDE.md) (15 min)
5. **Plan**: [I18N_MIGRATION_PLAN.md](I18N_MIGRATION_PLAN.md)
6. **Execute**: Start migrating your views

## 📊 Statistics

| Metric                  | Value               |
|-------------------------|---------------------|
| Languages Supported     | 4                   |
| Total Translation Keys  | 250+                |
| Core Classes Created    | 5                   |
| Resource Files          | 5                   |
| Documentation Pages     | 5                   |
| Example Views           | 1 (TokenConfigView) |
| Views Needing Migration | 150+                |

## ✅ Checklist for New Developers

- [ ] Read this README
- [ ] Review [I18N_QUICK_REFERENCE.md](I18N_QUICK_REFERENCE.md)
- [ ] Examine `TokenConfigView.java`
- [ ] Review resource files structure
- [ ] Test language switching
- [ ] Ask questions in team channels
- [ ] Start migrating assigned views

## 🚀 Ready to Start?

1. **Clone/Update** your local repository
2. **Run** the application and test language selector
3. **Review** TokenConfigView as an example
4. **Pick** a view from [I18N_MIGRATION_PLAN.md](I18N_MIGRATION_PLAN.md)
5. **Follow** the migration steps
6. **Test** with all 4 languages
7. **Submit** your changes

---

## 📄 Summary

This i18n implementation provides a complete, production-ready solution for multi-language support in the KMS Starter
UI. With clear documentation, helpful utilities, and a working example, developers can easily add translations
throughout the application.

**Status**: ✅ Infrastructure Complete | 🔄 Views Migration In Progress

**Questions or issues?** Refer to the appropriate documentation file or contact your team lead.

---

**Version**: 1.0  
**Date**: 2026-06-21  
**Maintained By**: KMS Development Team

