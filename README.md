# LumaGlass Launcher

Un launcher Android complet de démonstration inspiré d'une esthétique glassmorphism violette.

## Fonctions
- Peut être choisi comme application d'accueil Android.
- Détecte les applications installées exposées comme applications de lancement.
- Lance les applications depuis la grille.
- Recherche d'applications.
- Barre latérale flottante.
- Panneau Settings translucide.
- Raccourcis vers les vrais écrans Android : Wi-Fi, Bluetooth, écran, notifications, batterie, stockage, localisation, accessibilité, applications et son.
- Design violet sombre, transparence et bordures lumineuses.
- Interface portrait.

## Ouvrir
1. Extraire le ZIP.
2. Ouvrir le dossier `LumaGlassLauncher` dans Android Studio.
3. Laisser Android Studio synchroniser Gradle.
4. Brancher le téléphone avec le débogage USB activé.
5. Cliquer sur Run.
6. Android peut proposer de choisir l'application d'accueil : sélectionner `LumaGlass Launcher`.

Le projet nécessite Android Studio avec un JDK 17 et un SDK Android 35.

## Important
C'est un launcher Android, pas un nouveau système d'exploitation. Android reste le système sous-jacent. Le projet utilise l'intent `CATEGORY_HOME` pour être reconnu comme écran d'accueil.
