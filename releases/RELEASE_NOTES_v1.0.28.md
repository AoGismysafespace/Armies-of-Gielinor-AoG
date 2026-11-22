# Armies of Gielinor — Standalone Release v1.0.28

## Overview
Release **v1.0.28** resolves the asset loading freeze where the game permanently hung at `Asset Loading: 67% ("Loading extra data - 75%")` on fresh installations.

## What's Changed
- **Dynamic JS5 Disk Cache Streaming Fallback (`AoGServer`)**:
  - `AoGServer.java` now includes a dynamic fallback mechanism (`readArchiveFromDisk`) that reads missing cache files directly from disk (`main_file_cache.dat2` and `.idx`), caches them in memory, and streams them over the standard JS5 chunk protocol to the game client.
  - No streaming request (including Archive 17 files 43..56) is ever dropped again.
- **Portable Game-Folder Cache (`caches/armiesofgielinor/`)**:
  - The standalone release ZIP now bundles the complete, verified 27-index golden master cache directory directly inside the game folder.
  - The game is now 100% self-contained and portable on any drive (such as external or secondary SSDs) without depending on `C:\Users\<Name>\.alterorb` or `\.armiesofgielinor`.
- **Embedded Cache Synchronization (`ArmiesOfGielinor.jar`)**:
  - Replaced the legacy truncated `main_file_cache.dat2` (7,754,520 bytes) with the complete 7,793,387-byte file inside the application JAR, ensuring fallback extraction is fully intact.

## Compatibility & Integrity
- Bytecode target strictly compiled with `--release 18` (OpenJDK 18.0.2.1, major version 62.0).
- Fully compatible with Singleplayer Campaign, Custom Skirmish, and Steamworks Multiplayer.
