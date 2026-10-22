# Shoreline Client

Needs [Baritone API](https://github.com/cabaletta/baritone/releases/download/v1.11.2/baritone-api-fabric-1.11.2.jar) in order to work.
[Build for 1.21.1](https://github.com/ImLegiitXD/shoreline-client-buildable/releases)

I'll update this if theres new jar leaks 

### **ClickGUI:**  
![image](https://github.com/user-attachments/assets/ab74c01c-0181-4c6e-bc3d-00dc76bbd8ff)

### **Credits**  
- **[Skitttyy](https://github.com/Skitttyy/shoreline-client)** – for the src  


## Mojmap development

Minecraft 1.21.1 / Fabric / Java 21. This project uses Mojang official mappings
via `mappings loom.officialMojangMappings()`.

Build from Java sources with JDK 21:

```sh
./gradlew build
```

Windows:

```bat
gradlew.bat build
```

The remapped mod JAR is written to `build/libs/`. Install Fabric API and
Baritone API Fabric 1.11.2 alongside the mod when running Minecraft 1.21.1.
Baritone is resolved from its official GitHub release during development.

The Java sources, Mixin selectors, shadow/accessor/invoker names, and access
widener were migrated from Yarn. Loom is pinned to 1.6.12; Shadow is pinned to
8.3.5 for Gradle 8.7. The absent `launch` subproject reference was removed.

Compilation and runtime validation were not completed, as requested.
