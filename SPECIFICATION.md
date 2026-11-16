# LeafActu — Cahier des charges

## Vision
Application Android LeafVibe permettant de suivre l'information par sujets, territoires et temporalités : passé, présent et futur.

## Fonctionnalités
### Flux d'actualité
- Lecture de flux RSS 2.0 et Atom compatibles.
- Source, titre, résumé, date, lien original.
- Actualisation manuelle et regroupement des doublons.
- Gestion des erreurs et indisponibilités par source.

### Personnalisation
- Catégories et sous-catégories personnalisables.
- Sélection de territoires à plusieurs niveaux : monde, continent, pays, région, ville.
- Filtres combinables par catégorie, territoire, période et source.
- Préférences enregistrées localement.

### Temporalités
- Présent : articles récents avec date de publication.
- Passé : archives historiques datées, avec source.
- Futur : événements annoncés et prévisions clairement identifiés.
- Les prévisions ne doivent jamais être présentées comme des faits certains.

### Organisation personnelle
- Favoris persistants.
- Recherche par titre et résumé.
- Historique de lecture.
- Mode hors ligne pour les contenus enregistrés.
- Export/import des préférences.

### Sources
- Sources RSS/Atom ajoutables par l'utilisateur.
- Catalogue de sources vérifiées avec nom, URL, langue, catégories et territoires.
- Respect des conditions d'utilisation, licences, limites techniques et droits d'auteur.
- Pas de scraping non autorisé ni de reproduction intégrale d'articles protégés.

## Fiabilité
- Conserver l'URL et le nom de la source originale.
- Afficher les dates dans le fuseau local.
- Distinguer publication, mise à jour, événement programmé et prévision.
- Signaler les contenus dont la date ou le territoire n'est pas déterminé.

## État de développement — 1.1.0
- [x] Application Android Kotlin / Jetpack Compose.
- [x] Workflow GitHub Actions pour APK debug.
- [x] Lecture initiale de flux RSS/Atom.
- [x] Attribution de source et lien externe.
- [x] Catégorisation automatique initiale.
- [x] Préférences de catégorie et région sauvegardées.
- [x] Recherche dans les titres et résumés du flux.
- [x] Filtre géographique initial par mots-clés.
- [x] Favoris locaux persistants.
- [ ] Sélection multiple et hiérarchie géographique complète.
- [ ] Catalogue de sources configurable dans l'application.
- [ ] Archives historiques réelles.
- [ ] Agenda d'événements futurs sourcés.
- [ ] Cache hors ligne et historique de lecture.
- [ ] Tests automatisés, tests réseau et validation sur appareil.
- [ ] APK release signée.

## Architecture cible
- UI : Jetpack Compose.
- Réseau : dépôt RSS/Atom avec exécution hors thread UI.
- Stockage : préférences locales puis base Room pour articles, favoris, sources et historique.
- Tests : parsing RSS/Atom, classement, filtres, persistance et scénarios réseau.
