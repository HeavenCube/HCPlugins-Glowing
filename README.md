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

## Liens importants

- [HCPlugins-Core](https://github.com/HeavenCube/HCPlugins-Core) : HCCore, services communs et guide de création des plugins.
- [HCPlugins-actions](https://github.com/HeavenCube/HCPlugins-actions) : workflows GitHub Actions partagés.
- [HCPack-CustomGlowing](https://github.com/HeavenCube/HCPack-CustomGlowing) : resource pack Nexo des shaders de glow custom.
- [HCPlugins-AdvancementsRedirect](https://github.com/HeavenCube/HCPlugins-AdvancementsRedirect)
- [HCPlugins-Glowing](https://github.com/HeavenCube/HCPlugins-Glowing)
- [HCPlugins-HuskHomesGUI](https://github.com/HeavenCube/HCPlugins-HuskHomesGUI)
- [HCPlugins-ItemFrame](https://github.com/HeavenCube/HCPlugins-ItemFrame)
- [HCPlugins-JoinMessage](https://github.com/HeavenCube/HCPlugins-JoinMessage)
- [HCPlugins-PlaceholdersExtra](https://github.com/HeavenCube/HCPlugins-PlaceholdersExtra)

## Maintenance et documentation technique

HCCore est obligatoire. Pour toute modification technique, commencer par [AGENTS.md](AGENTS.md),
puis [le guide du plugin](docs/TECHNICAL.md) et le Core voisin.
Le [guide commun](https://github.com/HeavenCube/HCPlugins-Core/blob/main/docs/ECOSYSTEM.md) décrit les conventions de toute la suite.
`CLAUDE.md` et `GEMINI.md` renvoient aux mêmes instructions, sans copie des règles.
Le catalogue commun `plugins/HCPlugins/translations.yml` se recharge par `/hcplugins core reload`.
