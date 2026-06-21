# i18n Quick Reference Card

## 🚀 Quick Start

### 1. Basic Translation
```java
import eu.isygoit.i18n.I18n;

String text = I18n.t("common.button.create");  // "Create"
```

### 2. With Parameters
```java
String message = I18n.t("token.config.page.info", 1, 10);  // "Page 1/10"
```

### 3. Create Translated Button
```java
Button btn = new Button(I18n.t("common.button.save"));
```

## 📦 Using I18nUIHelper

```java
import eu.isygoit.util.I18nUIHelper;

// Create button
Button btn = I18nUIHelper.createButton("common.button.create");

// Create heading
H1 title = I18nUIHelper.createH1("title.keyManagement");

// Create text field
TextField field = I18nUIHelper.createTextField("search.placeholder");

// Set tooltip
component.setTitle(I18n.t("tooltip.refresh"));
```

## 🌍 Change Language at Runtime

```java
// In your LanguageSelectorComponent or similar
I18n.setLocale(new Locale("fr", "FR"));
UI.getCurrent().getPage().reload();
```

## 📝 Key Naming Convention

```
<module>.<feature>.<component>.<property>

Examples:
common.button.create
token.config.title
token.config.page.info
notification.success
error.validation
```

## 📚 Available Key Patterns

| Pattern | Example | Use Case |
|---------|---------|----------|
| `common.*` | `common.button.create` | Reusable UI elements |
| `title.*` | `title.dashboard` | Page titles |
| `notification.*` | `notification.error` | Status messages |
| `dialog.*` | `dialog.delete.confirmation` | Dialog content |
| `validation.*` | `validation.required` | Form validation |
| `tooltip.*` | `tooltip.refresh` | Hover hints |
| `<feature>.*` | `token.config.title` | Feature-specific |

## 🔧 Add New Translation Key

### Step 1: Add to messages.properties
```properties
mykey.title=My Title
mykey.button.action=Click Me
```

### Step 2: Add to messages_fr_FR.properties
```properties
mykey.title=Mon Titre
mykey.button.action=Cliquez-moi
```

### Step 3: Add to messages_es_ES.properties
```properties
mykey.title=Mi Título
mykey.button.action=Hágame clic
```

### Step 4: Add to messages_de_DE.properties
```properties
mykey.title=Mein Titel
mykey.button.action=Klick mich
```

### Step 5: Use in Code
```java
String title = I18n.t("mykey.title");
Button btn = new Button(I18n.t("mykey.button.action"));
```

## 🌐 Supported Languages

- **English (en_US)** 🇺🇸
- **French (fr_FR)** 🇫🇷
- **Spanish (es_ES)** 🇪🇸
- **German (de_DE)** 🇩🇪

## 💡 Pro Tips

### 1. Always Provide 4 Translations
Don't forget any language - check all files!

### 2. Use Consistent Keys
Same text → same key everywhere

### 3. Parameter Formatting
- String: `%s`
- Number: `%d`
- Example: `"Page %d of %d"` for pagination

### 4. Test With Each Language
Some languages are longer (German) or shorter (Chinese)

### 5. Use I18nUIHelper for Components
Reduces code and ensures consistency

## 🚨 Common Mistakes

❌ **Don't hardcode text**
```java
// WRONG
Button btn = new Button("Create");
```

✅ **Do use i18n**
```java
// RIGHT
Button btn = new Button(I18n.t("common.button.create"));
```

---

❌ **Don't forget a language**
```properties
# WRONG - missing German
# messages.properties
# messages_en_US.properties
# messages_fr_FR.properties
# Missing messages_de_DE.properties
```

✅ **Do translate everywhere**
```properties
# RIGHT - all 4 languages
# messages.properties
# messages_en_US.properties
# messages_fr_FR.properties
# messages_de_DE.properties
```

---

❌ **Don't mix keys**
```java
// WRONG - inconsistent
String msg = I18n.t("button.create");
String msg2 = I18n.t("common.button.create");
```

✅ **Do be consistent**
```java
// RIGHT - same key everywhere
String msg = I18n.t("common.button.create");
String msg2 = I18n.t("common.button.create");
```

## 🎯 Migration Checklist

For each view that needs i18n:

- [ ] Import `I18n` class
- [ ] Identify all hardcoded strings
- [ ] Create keys in all 4 resource files
- [ ] Replace strings with `I18n.t("key")`
- [ ] Test with all 4 languages
- [ ] Verify UI layout doesn't break

## 📂 File Locations

```
src/main/resources/
├── messages.properties              # Default
├── messages_en_US.properties        # English
├── messages_fr_FR.properties        # French
├── messages_es_ES.properties        # Spanish
└── messages_de_DE.properties        # German

src/main/java/eu/isygoit/i18n/
├── I18nProvider.java                # Core provider
└── I18n.java                        # Helper class

src/main/java/eu/isygoit/util/
└── I18nUIHelper.java                # UI component helpers

src/main/java/eu/isygoit/ui/common/component/
└── LanguageSelectorComponent.java   # Language switcher

src/main/java/eu/isygoit/config/
└── VaadinConfig.java                # Spring configuration
```

## 🎓 Learn More

- See `I18N_GUIDE.md` for comprehensive documentation
- Check `TokenConfigView.java` for full implementation example
- Review `I18N_MIGRATION_PLAN.md` for detailed migration steps

## 📞 Questions?

1. Check existing translation in resource files
2. Look at `TokenConfigView` for example
3. Review the key naming convention above
4. Consult `I18N_GUIDE.md` for detailed information

---

**Version**: 1.0  
**Last Updated**: 2026-06-21  
**Status**: ✅ Ready to Use

