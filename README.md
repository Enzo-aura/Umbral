# Umbral — Minecraft Java 26.2 Fabric mod

Adds the **Umbral** shadow spear.

- Sharpness X
- Lunge IV
- Enchanted glint
- No fall damage while held
- No hunger exhaustion while held
- Sneak + right-click throws a visible Umbral projectile
- Projectile flies along the player's aim, stops at a block, then teleports its owner to the impact point
- 3-second vanilla item cooldown
- Shadow/portal particles and Enderman teleport sound
- Server-authoritative projectile/teleport logic for multiplayer
- Does not modify vanilla weapons

## Build

Minecraft 26.2 requires Java 25. Install JDK 25 and Gradle 9.5.1+ (or import this project into IntelliJ IDEA with the Fabric Loom plugin). Then run:

`gradle build`

The production JAR will be in `build/libs/`.

The environment used to prepare this archive has Java 21 and no Gradle/network access, so the binary JAR could not be compiled here. The project is intentionally supplied with the 26.2 Fabric toolchain configuration so it can be built on a normal Java 25 development machine.
