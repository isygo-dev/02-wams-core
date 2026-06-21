# 🚀 i18n Batch Migration Strategy - Complete Implementation

## Executive Summary

Applying i18n to all 150+ views requires:
1. **Automated tools** to speed up migration
2. **Consistent patterns** for all view types
3. **Batch processing** to handle multiple files
4. **Quality assurance** to verify translations

This document provides the complete strategy.

---

## Phase 1: Pre-Migration Preparation

### Step 1: Scan All Files for Hardcoded Strings

**PowerShell Script to Find Hardcoded Strings:**

```powershell
# Find all Java files with hardcoded strings
$pattern = '"[A-Z][a-zA-Z\s\.]+[a-zA-Z0-9]*"'
Get-ChildItem -Recurse -Filter "*.java" | ForEach-Object {
    $content = Get-Content $_.FullName
    $matches = [regex]::Matches($content, $pattern)
    if ($matches.Count -gt 0) {
        Write-Host "File: $($_.Name)"
        $matches | Select-Object -Unique -ExpandProperty Value | Sort-Object
    }
}
```

### Step 2: Group Files by Type

- **Dialogs**: 50+ files → pattern is consistent
- **Views**: 40+ files → pattern is consistent
- **Cards**: 20+ files → pattern is similar
- **Panels**: 15+ files → pattern is similar
- **Other**: 25+ files → varied patterns

---

## Phase 2: Migration Patterns by View Type

### Pattern 1: Dialog Files

**Original:**
```java
public class CreateTokenConfigDialog extends BaseActionDialog {
    public CreateTokenConfigDialog(Service service) {
        super("Create Configuration",
              "Enter token configuration details",
              onSuccess);
        setOkButtonText("Create");
    }
}
```

**Migrated:**
```java
import eu.isygoit.i18n.I18n;

public class CreateTokenConfigDialog extends BaseActionDialog {
    public CreateTokenConfigDialog(Service service) {
        super(I18n.t("dialog.create.title"),
              I18n.t("token.config.create.description"),
              onSuccess);
        setOkButtonText(I18n.t("common.button.create"));
    }
}
```

**Translation Keys to Add:**
```properties
token.config.create.description=Enter token configuration details
dialog.create.title=Create Configuration (already exists)
common.button.create=Create (already exists)
```

### Pattern 2: View Files

**Original:**
```java
public class TokenManagementView extends VerticalLayout {
    private void buildUI() {
        H1 title = new H1("Token Management");
        Button createBtn = new Button("Create Token");
        searchField.setPlaceholder("Search tokens...");
    }
}
```

**Migrated:**
```java
import eu.isygoit.i18n.I18n;

public class TokenManagementView extends VerticalLayout {
    private void buildUI() {
        H1 title = new H1(I18n.t("title.tokenManagement"));
        Button createBtn = new Button(I18n.t("common.button.create"));
        searchField.setPlaceholder(I18n.t("token.search.placeholder"));
    }
}
```

### Pattern 3: Card Files

**Original:**
```java
public class TokenCard extends BaseCard {
    protected void buildBodyRows() {
        add(createRow("Status", token.getStatus()));
        add(createRow("Created", token.getCreatedDate()));
    }
}
```

**Migrated:**
```java
import eu.isygoit.i18n.I18n;

public class TokenCard extends BaseCard {
    protected void buildBodyRows() {
        add(createRow(I18n.t("table.column.status"), token.getStatus()));
        add(createRow(I18n.t("table.column.createdDate"), token.getCreatedDate()));
    }
}
```

---

## Phase 3: Automated Migration Script

### Python Script for Batch Migration

Save as `migrate_i18n.py`:

```python
#!/usr/bin/env python3
import os
import re
import sys

def find_hardcoded_strings(java_file):
    """Extract hardcoded strings from Java file"""
    with open(java_file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Pattern to find quoted strings
    pattern = r'"([A-Z][a-zA-Z\s\.]+[a-zA-Z0-9]*)"'
    matches = re.findall(pattern, content)
    return list(set(matches))  # Unique strings

def generate_translation_key(string):
    """Generate i18n key from string"""
    # Convert to lowercase and replace spaces with dots
    key = string.lower().replace(' ', '.')
    # Remove special characters
    key = re.sub(r'[^a-z0-9\.]', '', key)
    # Prefix with context
    return f"custom.{key}"

def add_i18n_import(java_file):
    """Add I18n import if not present"""
    with open(java_file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    if 'import eu.isygoit.i18n.I18n;' not in content:
        # Add import after other imports
        import_pattern = r'(import .*?;)'
        match = re.search(import_pattern, content)
        if match:
            insert_pos = match.end()
            new_import = '\nimport eu.isygoit.i18n.I18n;'
            content = content[:insert_pos] + new_import + content[insert_pos:]
            with open(java_file, 'w', encoding='utf-8') as f:
                f.write(content)
            return True
    return False

def batch_migrate(directory, file_filter="Dialog.java"):
    """Migrate multiple files"""
    for root, dirs, files in os.walk(directory):
        for file in files:
            if file_filter in file and file.endswith('.java'):
                filepath = os.path.join(root, file)
                print(f"Processing: {filepath}")
                
                strings = find_hardcoded_strings(filepath)
                if strings:
                    print(f"  Found {len(strings)} hardcoded strings")
                    add_i18n_import(filepath)
                    
                    for string in strings[:5]:  # Show first 5
                        key = generate_translation_key(string)
                        print(f"  - '{string}' → {key}")

if __name__ == "__main__":
    # Batch migrate all Dialog.java files
    batch_migrate("src/main/java", "*Dialog.java")
```

### Usage:

```bash
python3 migrate_i18n.py
```

---

## Phase 4: Translation Keys Repository

### Common Translation Keys (Already Defined)

These don't need to be added again:

```properties
common.button.* (all buttons)
common.button.create, delete, save, etc.
title.* (all page titles)
notification.* (all notifications)
error.* (all errors)
success.* (all success messages)
validation.* (all validation messages)
dialog.* (all dialogs)
table.column.* (all table columns)
tooltip.* (all tooltips)
```

### New Translation Keys to Add (By Module)

#### Token Configuration Module
```properties
token.config.issuer=Issuer
token.config.audience=Audience
token.config.algorithm=Algorithm
token.config.lifetime=Lifetime
token.config.delete.confirmation=Delete configuration '%s'? This action is irreversible.
```

#### Password Configuration Module
```properties
password.config.minLength=Minimum Length
password.config.maxLength=Maximum Length
password.config.uppercase=Uppercase Required
password.config.lowercase=Lowercase Required
password.config.digits=Digits Required
password.config.specialChars=Special Characters
```

(Similar for PEB, Digest, and other modules)

---

## Phase 5: Parallel Migration Strategy

### Divide Work by Team

**Developer 1: Dialogs (24 files)**
- All *Dialog.java files
- Focus: Replace constructor parameters with I18n.t()
- Time: ~3 hours

**Developer 2: Configuration Views (15 files)**
- PasswordConfigView, PEBConfigView, DigestConfigView, etc.
- Focus: Replace titles, buttons, labels
- Time: ~4 hours

**Developer 3: Card Components (12 files)**
- *Card.java files
- Focus: Replace labels in buildBodyRows()
- Time: ~3 hours

**Developer 4: Other Views (40 files)**
- Dashboard, Statistics, Advanced Views
- Focus: Replace all UI text
- Time: ~8 hours

**Developer 5: QA & Testing (Continuous)**
- Test each migrated view in all 4 languages
- Verify no missing translations
- Time: ~6 hours

---

## Phase 6: Batch File Processing

### Template for Developers

1. **Read view file**
2. **Identify all hardcoded strings** (use IDE Find & Replace)
3. **Create i18n keys** following naming convention
4. **Update Java file** with I18n.t() calls
5. **Add keys to ALL 4 resource files**
6. **Test in all 4 languages**
7. **Commit with message**: `i18n: migrate [ViewName]`

### Checklist for Each File

- [ ] Added `import eu.isygoit.i18n.I18n;`
- [ ] Replaced all hardcoded strings with `I18n.t()`
- [ ] Added keys to messages.properties
- [ ] Added keys to messages_en_US.properties
- [ ] Added keys to messages_fr_FR.properties
- [ ] Added keys to messages_es_ES.properties
- [ ] Added keys to messages_de_DE.properties
- [ ] Tested in English
- [ ] Tested in French
- [ ] Tested in Spanish
- [ ] Tested in German
- [ ] No missing key warnings
- [ ] UI layout looks good in all languages

---

## Phase 7: Quality Assurance

### Automated Testing

```java
// Test that all keys are defined
@Test
public void testAllKeysTranslated() {
    for (Locale locale : I18n.getSupportedLocales()) {
        assertNotNull(I18n.t("token.config.issuer", locale));
        assertNotNull(I18n.t("password.config.minLength", locale));
        // ... for all keys
    }
}

// Test that no keys return "!key!"
@Test
public void testNoMissingKeys() {
    List<String> allKeys = extractAllI18nKeys();
    for (String key : allKeys) {
        String value = I18n.t(key);
        assertFalse(value.startsWith("!"), "Missing key: " + key);
    }
}
```

### Manual Testing

1. **Language Switching**: Change language, verify all text updates
2. **Layout Testing**: Check UI doesn't break with longer translations
3. **Translation Quality**: Review translations for accuracy
4. **Special Characters**: Verify special chars display correctly

---

## Phase 8: Progress Tracking

### Spreadsheet Template

| View Name | Type | Dev | Status | Tested | Notes |
|-----------|------|-----|--------|--------|-------|
| TokenConfigCard | Card | Dev1 | Done | ✅ | All languages OK |
| DeleteTokenDialog | Dialog | Dev1 | Done | ✅ | Needs confirmation |
| PasswordConfigView | View | Dev2 | In Progress | ⏳ | WIP |
| ... | ... | ... | ... | ... | ... |

---

## Phase 9: Master Script (Combine All)

### Complete Automation Script

```bash
#!/bin/bash
# Master i18n migration script

echo "=== i18n Batch Migration Script ==="

# Step 1: Find all Java files needing migration
echo "Scanning for hardcoded strings..."
find . -name "*.java" -type f | while read file; do
    if grep -q '"[A-Z][a-zA-Z ]*"' "$file"; then
        echo "Found: $file"
    fi
done

# Step 2: Add i18n imports
echo "Adding I18n imports..."
find . -name "*.java" -type f | while read file; do
    if ! grep -q "import eu.isygoit.i18n.I18n;" "$file"; then
        sed -i '/^import.*Dialog;/a import eu.isygoit.i18n.I18n;' "$file"
    fi
done

# Step 3: Verify build
echo "Building project..."
mvn clean compile -DskipTests

echo "Migration complete!"
```

---

## Step-by-Step Execution Plan

### Week 1: Preparation
- [ ] Day 1: Create all translation keys
- [ ] Day 2: Set up automation scripts
- [ ] Day 3: Train team on pattern
- [ ] Day 4-5: Start with high-priority dialogs

### Week 2: High-Priority Views
- [ ] Day 1-2: All 24 Dialogs
- [ ] Day 3-4: 15 Configuration Views
- [ ] Day 5: Quality assurance

### Week 3: Remaining Views
- [ ] Day 1-2: 12 Card Components
- [ ] Day 3-5: 40+ Other Views

### Week 4: Testing & Finalization
- [ ] Day 1-4: Comprehensive testing in all languages
- [ ] Day 5: Final fixes and deployment

---

## Expected Outcomes

✅ **All 150+ views translated**  
✅ **4 languages fully supported**  
✅ **No missing translation keys**  
✅ **UI tested in all languages**  
✅ **Production ready**  

---

## Resources

- **Documentation**: I18N_GUIDE.md
- **Quick Reference**: I18N_QUICK_REFERENCE.md
- **Migration Plan**: I18N_MIGRATION_PLAN.md
- **Example**: TokenConfigView.java (fully migrated)

---

**Status**: ✅ Strategy Complete - Ready for Execution


