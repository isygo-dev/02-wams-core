#!/usr/bin/env powershell
# i18n Batch Migration Script - Apply translations to all views, dialogs, and cards
# This script automates the migration of hardcoded strings to i18n translations

param(
    [string]$BasePath = "src/main/java",
    [string]$FilePattern = "*",
    [switch]$DryRun = $false
)

# Function to find hardcoded strings in a file
function Find-HardcodedStrings {
    param([string]$FilePath)

    $content = Get-Content -Path $FilePath -Raw
    $pattern = '"([A-Z][a-zA-Z\s\.]+[a-zA-Z0-9]*)"'
    $matches = [regex]::Matches($content, $pattern)

    $strings = @()
    foreach ($match in $matches) {
        $strings += $match.Groups[1].Value
    }

    return $strings | Select-Object -Unique
}

# Function to generate i18n key from string
function Generate-I18nKey {
    param([string]$String)

    $key = $String.ToLower()
    $key = $key -replace '\s+', '.'
    $key = $key -replace '[^a-z0-9\.]', ''

    return "custom.$key"
}

# Function to add i18n import
function Add-I18nImport {
    param([string]$FilePath)

    $content = Get-Content -Path $FilePath -Raw

    if ($content -notmatch 'import eu\.isygoit\.i18n\.I18n;') {
        # Find the last import statement
        $importMatch = [regex]::Match($content, '(import [^;]+;)', [System.Text.RegexOptions]::RightToLeft)
        if ($importMatch.Success) {
            $insertPos = $importMatch.Index + $importMatch.Length
            $newImport = "`nimport eu.isygoit.i18n.I18n;"
            $content = $content.Insert($insertPos, $newImport)

            if (-not $DryRun) {
                Set-Content -Path $FilePath -Value $content
            }
            return $true
        }
    }

    return $false
}

# Function to replace hardcoded strings with i18n calls
function Replace-WithI18n {
    param(
        [string]$FilePath,
        [hashtable]$StringMap
    )

    $content = Get-Content -Path $FilePath -Raw
    $modified = $false

    foreach ($kvp in $StringMap.GetEnumerator()) {
        $original = [regex]::Escape($kvp.Key)
        $pattern = '"' + $original + '"'

        if ($content -match $pattern) {
            $replacement = 'I18n.t("' + $kvp.Value + '")'
            $content = $content -replace $pattern, $replacement
            $modified = $true
        }
    }

    if ($modified -and -not $DryRun) {
        Set-Content -Path $FilePath -Value $content
    }

    return $modified
}

# Main migration logic
Write-Host "Starting i18n batch migration..."
Write-Host "Base Path: $BasePath"
Write-Host "File Pattern: $FilePattern"
Write-Host "Dry Run: $DryRun"
Write-Host ""

$javaFiles = Get-ChildItem -Path $BasePath -Recurse -Filter "*.java" -Include $FilePattern

$totalFiles = $javaFiles.Count
$processedFiles = 0
$modifiedFiles = 0

foreach ($file in $javaFiles) {
    $processedFiles++
    $filePath = $file.FullName
    $relativePath = $filePath.Substring((Get-Location).Path.Length + 1)

    Write-Host "[$processedFiles/$totalFiles] Processing: $relativePath"

    try {
        $strings = Find-HardcodedStrings -FilePath $filePath

        if ($strings.Count -gt 0) {
            Write-Host "  Found $($strings.Count) hardcoded strings"

            Add-I18nImport -FilePath $filePath | Out-Null

            $stringMap = @{}
            foreach ($str in $strings) {
                $key = Generate-I18nKey -String $str
                $stringMap[$str] = $key
            }

            $modified = Replace-WithI18n -FilePath $filePath -StringMap $stringMap

            if ($modified) {
                $modifiedFiles++
                Write-Host "  ✓ Updated"
            }
        }
    }
    catch {
        Write-Host "  ✗ Error: $_"
    }
}

Write-Host ""
Write-Host "Migration Summary:"
Write-Host "  Total Files Processed: $processedFiles"
Write-Host "  Files Modified: $modifiedFiles"
Write-Host "  Dry Run: $DryRun"
Write-Host ""
Write-Host "Next Steps:"
Write-Host "  1. Review the generated i18n keys"
Write-Host "  2. Add translations to all 4 resource files"
Write-Host "  3. Test all views in all 4 languages"
Write-Host "  4. Commit changes"


