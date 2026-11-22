# Armies of Gielinor – Standalone Steam Edition v1.0.12

**Armies of Gielinor v1.0.12** introduces native Direct3D hardware acceleration and optimized memory pre-allocation, significantly reducing CPU rendering overhead and preventing runtime memory resizing pauses.

---

### What's New in v1.0.12

1. **Native Direct3D Hardware Acceleration (Option 2)**
   - Enabled Windows Direct3D hardware acceleration (`sun.java2d.d3d=true`) and translucent sprite acceleration (`sun.java2d.translaccel=true`).
   - Complex 2D rendering passes – including the war fog, spell auras, movement range rings, and sprite blitting – are now offloaded directly to the graphics card (GPU) rather than computed entirely in software on the CPU.
   - Lowers CPU workload and enhances overall visual smoothness across all battlefield viewports.

2. **Optimized 512 MB – 1 GB Heap Pre-Allocation (Option 3)**
   - Configured an initial heap size of 512 MB (`-Xms512m`) and a maximum heap ceiling of 1,024 MB / 1 GB (`-Xmx1024m`).
   - The game engine pre-allocates a generous 512 MB buffer immediately at startup, eliminating runtime requests to Windows for additional memory expansion during resource-heavy moments.
   - Keeps the memory ceiling compact enough to ensure internal memory cleanup passes (Garbage Collection) execute continuously in sub-millisecond intervals without perceptible frame drops.

3. **Full Domino & Regression Suite Verified**
   - Preserved all previous milestone protections: the AI frame-slicing optimization (v1.0.11), the 20-minute inactivity watchdog (`AntiIdleWatchdog`), post-combat manual turn retention, spectator banner synchronization, and the one-click diagnostic bug exporter (F9).

> **Multiplayer & Skirmish Tip:** Since matches run on whoever hosts the room (*Host & Play*), make sure **everyone in your group – especially the Host – downloads and runs `v1.0.12`**.

---

### Download & How to Play

1. Download **`ArmiesOfGielinor_v1.0.12.zip`** below.
2. Extract the ZIP folder anywhere on your PC.
3. Keep **Steam** running in the background.
4. Launch **`ArmiesOfGielinor.exe`** and enjoy!
