# HCPlugins-Glowing

- Java 25, Paper 26.2, Gradle Kotlin DSL.
- HCCore owns `/hcplugins`; this plugin owns the top-level `/glow` shortcut.
- Compile Core and PlaceholdersExtra APIs from sibling source checkouts.
- Preserve vanilla glow, TAB carrier integration, and the placeholder provider lifecycle.
- Do not add packets, NMS, reflection, scoreboard teams, or server tick animation.
- Keep profile IDs and carriers aligned with the HeavenCube resource pack catalogue.
- Run `./gradlew build` before finalizing Java or Gradle changes.
- Do not commit, push, reset, rebase, stash, or change branches without explicit user authorization.
