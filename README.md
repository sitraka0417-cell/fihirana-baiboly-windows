# Baiboly sy Fihirana Advantista — Windows

Édition Windows indépendante de l'application Android. Le projet Android d'origine n'est pas modifié.

## Contenu
- Fihirana MG/FR/EN, recherche par numéro/titre, consultation et correction locale des paroles.
- Modification des couplets et refrains via le marqueur `[REFRAIN]`.
- Bible multi-versions, sélection de livre et chapitre, recherche de texte.
- Projection sur fenêtre distincte, utilisable sur un deuxième écran.
- Navigation projection par flèches gauche/droite et touche `R` pour afficher le refrain suivant.
- Hira manokana : catégories, numéros facultatifs, création, consultation, modification et suppression de chants enregistrés.
- Projection temporaire à plusieurs pages, avec remplacement des pages déjà enregistrées pour éviter les doublons, historique, taille de texte réglable et couleurs par mot avec balises `{#RRGGBB}mot{/}`.
- Taille de police distincte pour les chants et la Bible.
- Fonds de projection intégrés (bleu nuit, forêt, bordeaux et dégradé), plus image/GIF (JPG/PNG/BMP/GIF), avec zoom et position de recadrage (crop), et fonds vidéo MP4/M4V/MOV/AVI en lecture en boucle.
- Base de contenu en lecture seule et base Windows séparée pour les modifications.

## Données
La base `fihirana_baiboly.db` est téléchargée automatiquement au build depuis le dépôt Android public.
Au premier lancement, elle est copiée dans `%USERPROFILE%\.fihirana-baiboly\content.db`.
Les données modifiées par l'utilisateur sont stockées dans `%USERPROFILE%\.fihirana-baiboly\user.db`.
Aucune modification n'est écrite dans la base Android originale.

## Compilation Windows
Prérequis : Windows 10/11 64 bits, JDK 17 et connexion Internet au premier build.
Installer Gradle 8.10.2, puis dans PowerShell à la racine du projet :

```powershell
gradle :app:run
gradle :app:packageReleaseMsi
gradle :app:packageReleaseExe
```

Les installateurs seront créés dans :
`app/build/compose/binaries/main-release/`

Le workflow GitHub Actions `.github/workflows/windows-build.yml` compile automatiquement les deux installateurs et les publie comme artifact à chaque push sur `main` ou lancement manuel.

## Identité
Baiboly sy Fihirana Advantista  
Novolavolain'i Sitraka Nambinintsoa  
sitraka0417@gmail.com  
034 80 175 59

## Limites connues de cette livraison
- Les fonds vidéo MP4/M4V/MOV/AVI utilisent JavaFX Media et les codecs disponibles sur Windows. La compatibilité exacte dépend du codec du fichier; certains AVI/MOV peuvent ne pas être lisibles. Le recadrage zoom/déplacement concerne les images/GIF, pas la vidéo. La couleur d’un mot dans les diapositives temporaires est prise en charge avec la balise `{#RRGGBB}mot{/}`.
- Les refrains sont reconnus selon leur position dans la liste des strophes ; leur association spécifique à chaque couplet peut être corrigée dans l'éditeur.
- Le code source et le workflow de build sont fournis, mais les binaires MSI/EXE doivent être produits par GitHub Actions ou sur un PC Windows : l'environnement de préparation ici ne permet pas de compiler ni de tester un installateur Windows. Il faut donc attendre la réussite du workflow avant de considérer l'installateur comme validé.

### Couleur d’un mot dans une diapositive temporaire

Dans le champ Contenu, entourez le mot avec une balise couleur hexadécimale, par exemple :

```text
Andriamanitra dia {#D4A853}fitiavana{/}.
```

La couleur est appliquée pendant l’aperçu et la projection. La taille de police des diapositives temporaires se règle avec le curseur dédié.

### Fonds vidéo
Dans Réglages, choisissez un fichier MP4, M4V, MOV ou AVI comme fond. La vidéo démarre automatiquement et tourne en boucle pendant la projection. Le build Windows inclut les bibliothèques JavaFX Media natives; la lecture de certains codecs reste dépendante de la prise en charge du système.

### Recadrage des fonds vidéo
Les mêmes curseurs de zoom, déplacement horizontal et vertical sont également appliqués au MediaView pour les fonds vidéo. La prévisualisation de recadrage en direct n'est pas encore disponible dans l'écran Réglages.

## Dépannage
- **"Failed to launch JVM"** au démarrage de l'installateur : corrigé en ajoutant les modules JDK nécessaires
  (`java.sql`, `java.naming`, `java.logging`, …) au runtime embarqué (`modules(...)` dans `app/build.gradle.kts`).
  Si une erreur survient quand même, consultez `%USERPROFILE%\.fihirana-baiboly\error.log`.
- Raccourcis projection : ← / → (andininy), R (refrain), F11 ou F (plein écran), Échap (fermer).

## Version 1.1.0 — refonte projection et design
- **Vraie projection** : détection automatique du 2ᵉ écran (projecteur/TV), fenêtre plein écran sans bordure. Choix manuel dans Réglages → Écran de projection. Un seul écran : fenêtre flottante (F11 = plein écran).
- **Fihirana** : cliquer sur un couplet/refrain l'affiche immédiatement ; ←/→ suit l'ordre exact (1, refrain, 2, 3…) ; `R` saute au refrain et, pressé à nouveau, passe au couplet suivant (répétable) ; option « refrain après chaque couplet ».
- **Baiboly** : recherche de livre à la frappe (« Sa » → Salamo, Samoela…), ↑/↓ + Entrée, puis grilles Toko / Andininy ; saisie rapide « Sal 139:1-10 » ; un verset par écran, ←/→ pour avancer ; bouton Projeter / Akatona.
- **Fonds séparés** Fihirana / Baiboly : presets, import image/GIF animé/vidéo (copiés dans `%USERPROFILE%\.fihirana-baiboly\backgrounds`), zoom, position, assombrissement.
- **Ctrl + ↑/↓** (ou Ctrl + ←/→) : taille du texte pendant la projection ; le texte se réduit aussi automatiquement s'il déborde.
- Nouveau design sombre/or, rendu identique entre l'aperçu et l'écran de projection.
- Fond vidéo : affiché sur le 2ᵉ écran uniquement (fenêtre JavaFX sous une fenêtre de texte transparente).

### Refrains par couplet (v1.1.1)
Dans l'éditeur d'un chant : `[REFRAIN]` avant un refrain ; `[REFRAIN 4]` ou `[REFRAIN 1-3,5]` pour un refrain réservé à certains couplets.
Priorité : portée explicite → refrain juste après le couplet → dernier refrain placé avant → premier refrain du chant.
Exemple : `couplet 1`, `[REFRAIN]`, `couplet 2`, `couplet 3`, `[REFRAIN 4]`, `couplet 4` → les couplets 1–3 utilisent le 1er refrain, le 4 utilise le sien.
Touche **B** : masquer le texte (le fond reste). ★ : chants favoris.

Les refrains viennent déjà de la base (ordre des `stanzas`, ex. `v r v v v r`) : la règle est la même que dans l'application Android — refrain du couplet lui-même, sinon refrain juste après, sinon dernier refrain avant, sinon premier refrain après. La touche **R** suit cette règle ; l'édition manuelle (`[REFRAIN 4]`) n'est qu'une option de correction.

### Éditeur de chant (v1.1.4)
Chaque couplet/refrain est une carte : type Andininy/Refrain, monter/descendre, supprimer, et pour un refrain le champ « Ho an'ny andininy » (ex. `4` ou `1-3`) pour le réserver à certains couplets.
