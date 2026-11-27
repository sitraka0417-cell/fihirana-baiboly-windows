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
