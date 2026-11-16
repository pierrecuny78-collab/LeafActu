# 🍃 LeafActu

Application Android d'actualités organisée autour du Passé, du Présent et du Futur.

## Version 1.0.0 — prototype initial
- Interface Android native Kotlin + Jetpack Compose.
- Navigation Accueil, Passé, Présent, Futur et Explorer.
- Premiers filtres de catégories et territoires.
- Identité LeafColour.
- Workflow GitHub Actions pour produire une APK debug.

> Les premières cartes sont des données de démonstration. Les connecteurs RSS/Atom et la découverte automatique des sources ne sont pas encore implémentés.

## Compilation
Le workflow .github/workflows/android-debug.yml compile l'APK à chaque push sur main ou master et peut être lancé depuis Actions.
Artefact attendu : LeafActu-1.0.0-debug.

## Configuration
- JDK 17
- Android SDK Platform 35
- Android Gradle Plugin 8.7.3
- Gradle 8.9
- Android minimum API 26

## Identité LeafColour
- Oasis #206B4D
- Noyer #6B4935
- Or #C5A46D
- Crème #DCCBB1
- Brun sable #B69A7B
