# SentinelCore

A Fabric mod project providing a modular core with utilities and gameplay features (chat controls, vanish, fly, inventory protections, teleportation, and more) for Minecraft.

## Project layout
```
org.github.shatterz.sentinelcore
├─ SentinelCore.java
├─ util/
├─ config/
├─ permission/
├─ command/
└─ feature/
   ├─ mode/
   ├─ vanish/
   ├─ fly/
   ├─ chat/
   ├─ inventory/
   ├─ spawnprot/
   ├─ spawnelytra/
   ├─ prefix/
   ├─ freeze/
   ├─ itempickup/
   ├─ instantbreak/
   ├─ advancementblock/
   └─ teleport/
mixin/
```

## Build
- Java 21
- Gradle Wrapper included
- Fabric Loom

Build the mod jar:
```
./gradlew build
```
(On Windows, use `gradlew.bat`.)

The artifact will be in `build/libs`.

## Run (Dev)
- Use Loom run configs or `gradlew runClient` / `gradlew runServer` as configured.

## License
See `LICENSE.txt`.
