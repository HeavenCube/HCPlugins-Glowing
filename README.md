# HCPlugins-Glowing

Plugin Paper de sélection de profils de glow vanilla.

**Licence :** code source consultable et contributions bienvenues, mais usage
réservé aux serveurs HeavenCube. Toute réutilisation ou distribution exige une
autorisation écrite préalable. Voir [LICENSE](LICENSE).

Cloner `HCPlugins-Core` et `HCPlugins-PlaceholdersExtra` à côté de ce dépôt,
puis lancer `./gradlew build`. La CI clone les branches `main` publiques
et publie un JAR versionné pour chaque build réussi de `main`.

Le serveur requiert HCCore et HCPlaceholdersExtra. Le resource pack contenant
les shaders est installé séparément, via Nexo.

La configuration éditable se trouve dans `plugins/HCPlugins/HCGlowing.yml`.
Les sélections persistantes des joueurs sont dans
`plugins/HCPlugins/HCGlowing/data.yml`. Les anciens fichiers situés dans
`plugins/HCGlowing/` ne sont pas repris automatiquement.
