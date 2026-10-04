# HCPlugins-Glowing

**Cible : Paper/Minecraft 26.3 exclusivement, Java 25.** Le build utilise `26.3.build.+`.

**CI :** sources/ressources/build seulement ; docs seules sans runner. Pour les exceptions,
voir [la politique CI et les marqueurs de skip](https://github.com/HeavenCube/HCPlugins-actions/blob/main/docs/CI_COSTS.md).

Plugin Paper de sélection de profils de glow vanilla.

**Licence :** code source consultable et contributions bienvenues, mais usage
réservé aux serveurs HeavenCube. Toute réutilisation ou distribution exige une
autorisation écrite préalable. Voir [LICENSE](LICENSE).

Cloner `HCPlugins-Core` et `HCPlugins-PlaceholdersExtra` à côté de ce dépôt,
puis lancer `./gradlew build`. La CI clone les branches `main` publiques
et publie `HCGlowing.jar` dans chaque release après un build réussi de `main`.
Le fichier de build et la version interne restent `AAAA.MM.JJ-bN`.

Le serveur requiert HCCore et HCPlaceholdersExtra. Le resource pack contenant
les shaders est installé séparément, via Nexo.

La configuration éditable se trouve dans `plugins/HCPlugins/HCGlowing.yml`.
Le cosmétique choisi est stocké dans le PDC de chaque joueur, sous la clé
`hcglowing:selected_profile`, puis sauvegardé avec ses données par le serveur.
Il n'y a plus de fichier YAML de sauvegarde des sélections. Les anciens fichiers
`data.yml` ne sont ni lus ni importés ; chaque joueur repart sans sélection.

## Liens importants

- [HCPlugins-Core](https://github.com/HeavenCube/HCPlugins-Core) : HCCore, services communs et guide de création des plugins.
- [HCPlugins-actions](https://github.com/HeavenCube/HCPlugins-actions) : workflows GitHub Actions partagés.
- [HCPack-CustomAssets](https://github.com/HeavenCube/HCPack-CustomAssets) : resource pack Nexo 26.3 : glow, police et effets de texte.
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
