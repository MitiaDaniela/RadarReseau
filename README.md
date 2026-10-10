# Radar Réseau — Android

Application Android légère (Kotlin natif, sans publicité ni compte) pour afficher le signal mobile en dBm, le signal Wi‑Fi si connecté, et enregistrer les points GPS où le signal semble meilleur.

## Limites importantes
- L'application ne peut pas créer une carte magique de la couverture de tous les opérateurs. Pour comparer les zones, il faut marcher avec le téléphone et enregistrer des mesures.
- La lecture du signal mobile varie selon le modem, la SIM, l'opérateur et les restrictions Android. Certains téléphones peuvent afficher « indisponible ».
- La force du signal ne garantit pas le débit Internet : congestion de l'antenne et qualité de la liaison peuvent aussi compter.
- L'application n'envoie aucune donnée sur Internet; les endroits enregistrés restent localement sur le téléphone.
- Elle ne mesure le Wi‑Fi que lorsque le téléphone est connecté à un réseau Wi‑Fi.

## Générer l'APK
1. Sur un ordinateur, installer Android Studio.
2. Décompresser ce projet puis choisir **Open** dans Android Studio et sélectionner le dossier `RadarReseauAndroid`.
3. Attendre la synchronisation Gradle.
4. Choisir **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. L'APK debug sera généralement dans `app/build/outputs/apk/debug/app-debug.apk`.
6. Transférer l'APK sur le téléphone, l'ouvrir et autoriser la localisation et l'accès à l'état du téléphone.

Min SDK : Android 8.0 (API 26). Cible : Android 15 (API 35).
