# I18n (Internationalisation) Guide for KMS Starter UI

## Overview

L'application KMS Starter UI implémente un système complet d'internationalisation (i18n) permettant de supporter plusieurs langues. Le système est basé sur le fournisseur i18n de Vaadin et utilise des fichiers de ressources (Properties) pour stocker les traductions.

## Supported Languages

- English (US) - `en_US`
- French (FR) - `fr_FR`
- Spanish (ES) - `es_ES`
- German (DE) - `de_DE`

## Architecture

### Components

1. **I18nProvider** (`eu.isygoit.i18n.I18nProvider`)
   - Implémente l'interface `com.vaadin.flow.i18n.I18NProvider`
   - Charge et gère les fichiers de ressources
   - Fournit les traductions en fonction de la locale

2. **I18n Helper Class** (`eu.isygoit.i18n.I18n`)
   - Classe utilitaire statique pour accéder facilement aux traductions
   - Méthodes: `t(key)`, `t(key, params...)`, `t(key, locale)`

3. **LanguageSelectorComponent** (`eu.isygoit.ui.common.component.LanguageSelectorComponent`)
   - Composant UI pour permettre aux utilisateurs de changer de langue
   - ComboBox avec les langues supportées
   - Met à jour la locale de la session et recharge l'interface

4. **Resource Files**
   - `messages.properties` (par défaut - English)
   - `messages_en_US.properties`
   - `messages_fr_FR.properties`
   - `messages_es_ES.properties`
   - `messages_de_DE.properties`

## Usage in Views

### Basic Translation

Pour utiliser une traduction simple dans vos vues Vaadin :

```java
import eu.isygoit.i18n.I18n;

// Dans une classe de vue
Button button = new Button(I18n.t("common.button.create"));
H2 title = new H2(I18n.t("title.keyManagement"));
```

### Parameterized Translation

Pour utiliser des traductions avec des paramètres :

```java
// Clé de ressource avec paramètres
String message = I18n.t("token.config.page.info", currentPage, totalPages);
// Affichera: "Page 1/10"
```

### Translation Keys Conventions

Les clés de traduction suivent une convention de hiérarchie :

- `common.*` - Composants et éléments communs
- `title.*` - Titres de pages
- `token.config.*` - Spécifique aux Token Configurations
- `password.config.*` - Spécifique aux Password Configurations
- `notification.*` - Messages de notification
- `error.*` - Messages d'erreur
- `success.*` - Messages de succès
- `validation.*` - Messages de validation
- `tooltip.*` - Textes de tooltip
- `kms.*`, `ims.*`, `dms.*` - Spécifique aux modules

## Adding i18n to Existing Views

### Step 1: Identify all String Literals

Parcourez votre vue et identifiez tous les textes affichés à l'utilisateur.

### Step 2: Create Translation Keys

Pour chaque texte, créez une clé mnémonique dans les fichiers de ressources :

```properties
# Dans messages.properties et autres fichiers localisés
myview.title=My View Title
myview.button.submit=Submit
myview.placeholder=Enter text here
```

### Step 3: Update the View

Remplacez les textes hardcodés par les appels à `I18n.t()` :

```java
import eu.isygoit.i18n.I18n;

public class MyView extends VerticalLayout {
    
    private void buildUI() {
        // Avant
        // H2 title = new H2("My View Title");
        
        // Après
        H2 title = new H2(I18n.t("myview.title"));
        
        Button button = new Button(I18n.t("myview.button.submit"));
        TextField textField = new TextField(I18n.t("myview.placeholder"));
    }
}
```

## Adding a New Language

### Step 1: Create Resource File

Créez un nouveau fichier dans `src/main/resources/` :

```
messages_it_IT.properties
```

### Step 2: Add Language to I18nProvider

Modifiez la classe `I18nProvider` pour ajouter la nouvelle locale :

```java
private static final Locale[] SUPPORTED_LOCALES = {
    new Locale("en", "US"),
    new Locale("fr", "FR"),
    new Locale("es", "ES"),
    new Locale("de", "DE"),
    new Locale("it", "IT")  // Nouvelle langue
};
```

### Step 3: Translate All Keys

Copiez toutes les clés du fichier `messages.properties` et traduisez-les dans la nouvelle langue.

## Language Selector Component

### How to Add to Your Layout

```java
import eu.isygoit.ui.common.component.LanguageSelectorComponent;

public class MyLayout extends VerticalLayout {
    public MyLayout() {
        // Ajouter le sélecteur de langue
        LanguageSelectorComponent languageSelector = new LanguageSelectorComponent();
        
        // Dans une barre d'outils ou header
        HorizontalLayout header = new HorizontalLayout();
        header.add(languageSelector);
    }
}
```

## Resource Files Organization

### File Naming Convention

```
src/main/resources/
├── messages.properties           # Default (English fallback)
├── messages_en_US.properties     # English
├── messages_fr_FR.properties     # French
├── messages_es_ES.properties     # Spanish
└── messages_de_DE.properties     # German
```

## Translation Keys Reference

### Common Buttons
```
common.button.create
common.button.edit
common.button.delete
common.button.save
common.button.cancel
common.button.close
common.button.refresh
```

### Page Titles
```
title.dashboard
title.keyManagement
title.tokenConfigurations
title.passwordConfigurations
```

### Token Configuration
```
token.config.header
token.config.create.button
token.config.empty.title
token.config.search.placeholder
```

## Best Practices

1. **Always use i18n for user-facing text**
   - Buttons, labels, messages, tooltips
   - Placeholder text, empty states

2. **Don't translate**
   - CSS class names
   - Technical field names
   - Internal variable names

3. **Use meaningful keys**
   - Évitez les clés génériques comme `label1`, `label2`
   - Utilisez une hiérarchie logique

4. **Format strings consistently**
   - Utilisez `%s` pour les paramètres de chaîne
   - Utilisez `%d` pour les nombres
   - Exemple: `"Page %d of %d"` → clé: `"pagination.page"`

5. **Keep translations up-to-date**
   - Lorsque vous ajoutez une nouvelle clé, traduisez-la dans TOUTES les langues
   - Testez avec chaque langue pour éviter les incohérences

## Testing i18n

### Manual Testing

1. Ouvrez l'application
2. Cliquez sur le sélecteur de langue
3. Changez de langue
4. Vérifiez que tous les textes sont mis à jour correctement

### Runtime Locale Change

```java
// Dans un composant
I18n.setLocale(new Locale("fr", "FR"));
UI.getCurrent().getPage().reload();
```

## Debugging

### Finding Missing Translations

Les clés manquantes affichent: `!clé.manquante!`

Si vous voyez ce format, ajoutez la clé manquante aux fichiers de ressources.

### Logger Output

Le I18nProvider affiche les avertissements lors du chargement :

```
Failed to load resource bundle for locale: ...
```

## Examples

### Complete View Example

```java
import eu.isygoit.i18n.I18n;

@Route(value = "example", layout = MainLayout.class)
@PageTitle("Example View")
public class ExampleView extends VerticalLayout {
    
    public ExampleView() {
        buildUI();
    }
    
    private void buildUI() {
        H1 title = new H1(I18n.t("example.title"));
        
        Button createBtn = new Button(I18n.t("common.button.create"));
        Button deleteBtn = new Button(I18n.t("common.button.delete"));
        
        TextField searchField = new TextField();
        searchField.setPlaceholder(I18n.t("search.placeholder"));
        
        add(title, searchField, createBtn, deleteBtn);
    }
    
    private void showMessage(String type) {
        String message = I18n.t("notification." + type);
        Notification.show(message);
    }
}
```

### Resource File Example

```properties
# messages_fr_FR.properties
example.title=Exemple de Vue
common.button.create=Créer
common.button.delete=Supprimer
search.placeholder=Rechercher...
notification.success=Succès
notification.error=Erreur
```

## Performance Considerations

- Les fichiers de ressources sont chargés au démarrage
- Les traductions sont mises en cache
- Les changements de locale rechargent l'interface (page reload)

## Internationalization Features

Le système i18n supporte :

- ✅ Plusieurs langues
- ✅ Changement de langue à runtime
- ✅ Paramètres dans les traductions
- ✅ Fallback automatique (en cas de clé manquante)
- ✅ Cache des bundles de ressources
- ✅ Intégration Vaadin standard

## Références

- [Vaadin I18n Documentation](https://vaadin.com/docs/latest/topics/i18n/overview)
- [Java ResourceBundle](https://docs.oracle.com/javase/8/docs/api/java/util/ResourceBundle.html)
- [ISO 639-1 Language Codes](https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes)

## Support

Pour ajouter une nouvelle langue, modifier des traductions, ou intégrer i18n dans de nouvelles vues, consultez ce guide et les fichiers de configuration existants.

