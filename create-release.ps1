# GitHub Release Creator Script
# Требует установку gh CLI или использование GitHub API

param(
    [string]$Owner = "Zlogdan",
    [string]$Repo = "clip-keep",
    [string]$Tag = "v1.0.0",
    [string]$JarPath = "target/clip-keep-1.0.0-fat.jar"
)

$repoPath = Split-Path -Parent $MyInvocation.MyCommand.Path
$jarFullPath = Join-Path $repoPath $JarPath

if (-not (Test-Path $jarFullPath)) {
    Write-Error "JAR file not found: $jarFullPath"
    exit 1
}

Write-Host "Release Details:"
Write-Host "  Owner: $Owner"
Write-Host "  Repo: $Repo"
Write-Host "  Tag: $Tag"
Write-Host "  JAR: $jarFullPath"
Write-Host "  JAR Size: $((Get-Item $jarFullPath).Length / 1MB) MB"
Write-Host ""
Write-Host "To create the release manually on GitHub:"
Write-Host "1. Go to: https://github.com/$Owner/$Repo/releases/new"
Write-Host "2. Click 'Choose a tag' and select '$Tag'"
Write-Host "3. Title: 'ClipKeep $Tag'"
Write-Host "4. Description:"
Write-Host "---"
Write-Host @"
# ClipKeep $Tag

Initial release of ClipKeep - Clipboard history manager for Windows and Linux.

## Features
- Clipboard history tracking
- System tray integration  
- JSON-based storage
- JavaFX UI

## Installation

Download the `clip-keep-1.0.0-fat.jar` file and run it with Java 17+:

``````bash
java -jar clip-keep-1.0.0-fat.jar
``````

## Requirements
- Java 17 or higher
- Windows or Linux with X11 (for Astra Linux)
"@
Write-Host "---"
Write-Host ""
Write-Host "5. Upload the JAR file: $jarFullPath"
Write-Host "6. Click 'Publish release'"
