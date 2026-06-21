# i18n Mass Migration Guide

## Strategy for Migrating 150+ Views

Given the large number of views to migrate, here's the most efficient approach:

## Phase 1: Batch Migration Template

### Pattern for View Migration

Every view follows a similar pattern:

1. **Add import**: `import eu.isygoit.i18n.I18n;`
2. **Replace hardcoded strings**: `"text"` → `I18n.t("key")`
3. **Add translation keys** to all 4 resource files

### Common String Patterns

| Pattern | Replacement | Key Example |
|---------|-------------|-------------|
| Button labels | `I18n.t("common.button.create")` | Already defined |
| Page titles | `I18n.t("title.keyManagement")` | Already defined |
| Dialog titles | `I18n.t("dialog.create.title")` | Already defined |
| Error messages | `I18n.t("notification.error")` | Already defined |
| Form labels | `I18n.t("form.label.fieldname")` | New per view |
| Tooltips | `I18n.t("tooltip.action")` | Already defined |

## Phase 2: Priority Order

### Tier 1 (Critical - Affects all users)
- [ ] All Dialog Base Classes
- [ ] Common Layout Components
- [ ] Authentication Views

### Tier 2 (High - Used frequently)
- [ ] All Config Management Views
- [ ] All Card Components
- [ ] Main Management Views

### Tier 3 (Medium - Used less frequently)
- [ ] Advanced Operations Views
- [ ] Statistics/Dashboard Views
- [ ] Utility Panels

## Phase 3: Automated Migration

### Using Find & Replace

For each view, use IDE Find & Replace with regex:

**Find**: `"([A-Z][a-zA-Z ]+)"`  
**Replace**: `I18n.t("view.key")`

Then manually adjust keys.

## Phase 4: Translation Key Management

### Adding New Keys

For each view, identify:
1. Button labels → `viewname.button.*`
2. Field labels → `viewname.label.*`
3. Messages → `viewname.message.*`
4. Dialog text → `dialog.*` (shared)

### Batch Add Keys to Properties

```properties
# Token Configuration
token.config.issuer=Issuer
token.config.audience=Audience
token.config.algorithm=Algorithm
token.config.lifetime=Lifetime
token.config.edit.button=Edit Configuration
token.config.delete.button=Delete Configuration

# Password Configuration
password.config.minLength=Minimum Length
password.config.maxLength=Maximum Length
```

## Phase 5: Testing Strategy

### Quick Testing
1. Start app
2. Switch language
3. Check specific view
4. Verify all text updates

### Comprehensive Testing
1. Test all views in all 4 languages
2. Check UI layout with longest text (German)
3. Verify no missing key warnings
4. Test responsive behavior

## Implementation Steps

### For Each View:

```
1. Read the view file
2. Identify all hardcoded strings
3. Create translation keys
4. Add keys to all 4 resource files
5. Update view code with I18n.t()
6. Test in all languages
7. Commit changes
```

### Tools Needed

- IDE with Find & Replace (IntelliJ recommended)
- Text editor for properties files
- Test browser for multiple languages

## Estimated Effort

- **Per Dialog**: 5-10 minutes
- **Per View**: 10-15 minutes
- **Per Config View**: 15-20 minutes
- **Testing**: 2-3 minutes per view

**Total for 150 views**: ~40-50 hours (can be parallelized)

## Recommended Team Approach

1. **Developer 1**: Dialogs (20+ views)
2. **Developer 2**: Configuration Views (30+ views)
3. **Developer 3**: Card Components (20+ views)
4. **Developer 4**: Other Views (60+ views)
5. **QA**: Testing in all languages

## Automation Opportunities

### Script to Generate Translation Keys

```bash
#!/bin/bash
# Extract quoted strings from Java files
grep -oP '"[^"]{3,}"' *.java | sort -u | while read str; do
  key=$(echo $str | tr ' ' '.' | tr '[:upper:]' '[:lower:]')
  echo "$key=$str"
done
```

### IDE Macro for Quick Migration

1. Find: `new Button\("([^"]+)"\)`
2. Replace: `new Button(I18n.t("key.for.$1"))`

## Next Steps

This guide provides the framework. The actual migration will proceed view-by-view, starting with high-priority components and dialogs.

---

**Status**: Ready to proceed with full migration

