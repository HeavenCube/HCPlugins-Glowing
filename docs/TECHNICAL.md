# Guide technique — HCGlowing

## Point d’entrée

Cible : Paper 26.3 (`26.3.build.+`), Java 25 sans preview. Compilation avec `-Xlint:all` ;
examiner les warnings avant de les attribuer au plugin ou à une dépendance.

Ce dépôt appartient à la suite privée d’usage HeavenCube, publiée comme source consultable.
Il dépend obligatoirement de HCCore. Lire d’abord [AGENTS.md](../AGENTS.md), puis le Core voisin.
Le [guide commun](https://github.com/HeavenCube/HCPlugins-Core/blob/main/docs/ECOSYSTEM.md) décrit les règles Java/Paper, les contrats Core,
le packaging et la CI. Ce guide local décrit les particularités à préserver ; le code reste l’autorité.

## Dépendances et compilation

HCCore et HCPlaceholdersExtra obligatoires. TAB transporte le carrier via le placeholder ; le pack donne les effets client.

Cloner Core et PlaceholdersExtra côte à côte ; les deux APIs sont `compileOnly` et substituées par `includeBuild`. JAR standard, sans Shadow.

```powershell
.\gradlew.bat build
```

Utiliser JDK 25. Sous Linux : `./gradlew build`. Le JAR est dans `build/libs/` ; installer aussi
les plugins serveur requis. Un clone Core modifié affecte le classpath local ; noter son commit.
Après extension d’API commune, construire Core séparément et installer sa version compatible en premier.

## Commandes et permissions

`/hcplugins glowing` ou `/glow` ouvre le dialogue ; `reload` est réservé aux opérateurs. Les cosmétiques créent `hcplugins.glowing.cosmetic.<id>` avec défaut false. Les choix sans permission restent visibles mais non sélectionnables.

La branche canonique est `/hcplugins glowing` ; elle est enregistrée chez Core, pas comme
une deuxième racine. Les résultats de reload, refus opérateur et autres textes partagés utilisent
`HCPluginsCore.translations(plugin)`. `{duration}` inclut déjà l’unité `ms`.

## Fichiers et données

`plugins/HCPlugins/HCGlowing.yml` : profils, boutons, messages métier.
La sélection appartient au PDC du joueur : clé `hcglowing:selected_profile`, type `PersistentDataType.STRING`.
La valeur est l'ID du cosmétique dans la configuration (ex. `heaven`), pas l'ID du profil Core (`heaven-gradient`).
L'absence de clé signifie aucun cosmétique équipé. Désactiver ou invalider le choix supprime uniquement cette clé.

Le serveur sauvegarde ce PDC avec les données du joueur ; le plugin ne force pas `Player#saveData()`.
Tous les accès passent par un joueur connecté, sur le thread serveur : aucune lecture hors ligne,
écriture disque directe, tâche de sauvegarde ou map globale de sélections.
Les anciens `data.yml` ne sont ni lus, importés, sauvegardés ni supprimés automatiquement.
Ils peuvent être retirés manuellement ; leurs sélections sont volontairement abandonnées.

Le démarrage, la connexion et le reload vérifient le choix contre la configuration et les permissions actuelles.
Un choix devenu invalide est supprimé ; les joueurs hors ligne sont vérifiés à leur prochaine connexion.
Le reload valide son candidat avant de remplacer le runtime et garde un snapshot temporaire des seuls joueurs
connectés pour restaurer leurs sélections, permissions et runtime si l'activation échoue.

Valeurs par défaut dans `src/main/resources/`, jamais écrasées à chaque démarrage. Aucun import
automatique des anciens dossiers du monorepo. Messages communs dans `plugins/HCPlugins/translations.yml` ;
messages métier locaux. Modifier le fichier partagé se recharge avec `/hcplugins core reload`.

## Chemin d’exécution

`HCGlowing` crée une génération configuration + moteur + dialogue, enregistre le module et le raccourci `/glow`, puis le provider chez PlaceholdersExtra. Le moteur applique le flag glowing vanilla ; il possède la restauration de l’état antérieur. Les couleurs du joueur passent par `%hcextra_glow_color%` dans le tagprefix TAB ; le pack reconnaît ce carrier et calcule l’effet avec GameTime.

## Carte du code pour une modification

| Fichier | Responsabilité et points à préserver |
| --- | --- |
| [HCGlowing.java](../src/main/java/fr/noltox/hcplugins/customplayerglowing/HCGlowing.java) | Orchestration, génération runtime, reload et rollback, cleanup. |
| [GlowConfiguration.java](../src/main/java/fr/noltox/hcplugins/customplayerglowing/config/GlowConfiguration.java) | Validation des IDs et profils du Core, configuration immuable. |
| [GlowEngine.java](../src/main/java/fr/noltox/hcplugins/customplayerglowing/service/GlowEngine.java) | Ownership du glowing, application/restitution et synchronisation des choix. |
| [GlowSelectionStore.java](../src/main/java/fr/noltox/hcplugins/customplayerglowing/storage/GlowSelectionStore.java) | Clé PDC joueur, sélection/suppression et snapshot temporaire pour rollback du reload. |
| [GlowSelectionDialog.java](../src/main/java/fr/noltox/hcplugins/customplayerglowing/dialog/GlowSelectionDialog.java) | Dialogue Paper, callbacks planifiés, génération active et permission revérifiée. |
| [PlayerGlowPlaceholderProvider.java](../src/main/java/fr/noltox/hcplugins/customplayerglowing/placeholder/PlayerGlowPlaceholderProvider.java) | Carrier legacy fourni au registre PlaceholdersExtra. |
| [PlayerGlowListener.java](../src/main/java/fr/noltox/hcplugins/customplayerglowing/listener/PlayerGlowListener.java) | Synchronisation au cours du lifecycle des joueurs. |
| [PlayerGlowCommand.java](../src/main/java/fr/noltox/hcplugins/customplayerglowing/command/PlayerGlowCommand.java) | Module canonique et même logique pour /glow. |

`src/main/resources/paper-plugin.yml` définit identité, dépendances et permissions serveur.
`settings.gradle.kts` définit les builds composites ; `build.gradle.kts` le packaging.
`.github/workflows/build.yml` appelle les actions partagées à `@main` ; `.github/dependabot.yml`
maintient les dépendances. Une mise à jour de dépendance doit conserver ces contrats.

## Invariants et zones à risque

- Audit performance : resynchronisations regroupées via `DeferredUpdates<UUID>` du Core,
  fermeture par génération ; audit contextuel 20 ticks conservé faute d'événement garanti.
  Buffer UUID réutilisé ; une révocation supprime la clé du PDC en mémoire, sans sérialiser de fichier.
  Le [rapport global](https://github.com/HeavenCube/HCPlugins-Core/blob/main/docs/PERFORMANCE_AUDIT.md)
  décrit l'audit précédent ; ses remarques sur la sauvegarde YAML de Glowing sont désormais historiques.

- Plugin serveur `HCGlowing`, HCCore obligatoire ; module `glowing`.
- HCCore et HCPlaceholdersExtra obligatoires. TAB transporte le carrier via le placeholder ; le pack donne les effets client.
- Ne pas ajouter de paquets, NMS, réflexion, scoreboard teams ou animation serveur par tick.
- Aligner profil ID/carrier avec Core et le pack ; le GPU calcule les gradients et animations.
- Préserver la restitution du flag glowing antérieur : le plugin ne doit pas effacer un effet appartenant à un autre système.
- Préserver rollback des sélections et des permissions, fermeture du provider et invalidation des anciens dialogues.

Avant une nouvelle logique transversale : chercher les usages dans Core et les autres plugins ;
ajouter au Core le contrat partagé réellement nécessaire avant le raccordement local. Ne pas recopier
un loader YAML, un registre de commandes ou un catalogue de traductions. Garder les événements et
états spécifiques ici. Thread serveur pour le jeu ; considérer callbacks et APIs tierces selon leur
thread réel, puis revalider le contexte avant mutation.

## Validation et limites

Une resynchronisation différée revérifie l’activité du moteur après reload/disable.
Choisir à nouveau le même cosmétique évite de réécrire sa clé PDC. Sélection, retrait et révocation
mutent seulement ce PDC en mémoire ; la persistance disque suit la sauvegarde native du joueur.
La déconnexion et le disable libèrent l'état visuel sans supprimer le choix persistant.
Le message `messages.save-failure` a été retiré : aucune écriture YAML de sélection n'est effectuée.

`GlowConfigurationTest`, `GlowOwnershipTest`, `GlowSelectionStoreTest` et `GlowEngineTest` existent.
Les tests PDC utilisent des joueurs simulés en mémoire et couvrent isolation des joueurs et des clés,
rollback, refus de permission, révocation, retrait, quit/join et profil supprimé.
Ils ne prouvent pas la sérialisation réelle des fichiers joueur par Paper.
En jeu : sélection/suppression, permission retirée, reload invalide, reconnexion puis redémarrage du serveur,
coexistence d’un glowing externe, résultat TAB et pack sur deux clients.

Sans transport TAB approprié, le glow reste blanc. Un build ne valide ni la fusion Nexo, ni le tagprefix TAB, ni le rendu GPU.

Documentation seule : vérifier les liens locaux et le diff. Modification runtime : build, tests ciblés,
et scénario serveur correspondant. Rapporter seulement ce qui a été exécuté, avec résultat et limite.
Pour transfert entre IA, donner le commit Core testé et les fichiers/changements encore non committés.
