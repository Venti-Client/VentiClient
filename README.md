# Venti Client

A client-side Fabric mod for **Minecraft 1.21.11** focused on visuals, HUD and quality of life. No combat, movement or ESP cheats.

Open the GUI with **Right Shift** (or `/venti`). Switch between the Venti layout and floating Panels under **Theme → GUI Style**.

## Modules

**Visuals:** 3D Hand, Hand Aura, Hand Pose, Swing Animation, Slow Swing, Custom Glint, Atmosphere, Color Grading, Motion Blur, Block Overlay, Jump Circles, Floating Relics, Particles, Totem Spirit, Time Changer, Low Health Warning, Zoom, Custom Fov, Freelook, Fullbright, No Render, Custom Crosshair, Nametags (vanilla visibility only: name, held item, ping and distance; no health or armor, never through walls)

**Render:** 3D Character, Hit FX, Custom Sky (any picture or GIF), Custom Totem (any picture or GIF)

**HUD:** HUD (info bar, coordinates, module list, potions, your armor and cooldowns), Hotkeys, Spotify HUD (synced lyrics, sky and screen lyrics, CJK and Cyrillic fonts)

**Misc:** Discord Presence, No Inv Leak (fake-item styles for streams), Waypoints, Weather Notifier, Sprint, Coord Snapper, Name Protect, Skin Change

## Install

1. Install [Fabric Loader](https://fabricmc.net/) for Minecraft 1.21.11 and [Fabric API](https://modrinth.com/mod/fabric-api).
2. Put the Venti jar in your `mods` folder.

## Build

```
./gradlew build
```

The jar ends up in `build/libs/`. Java 21 or newer.

## Credits and license

Venti is licensed under the **GNU General Public License v3.0** (see [LICENSE](LICENSE)). It includes code ported from these GPL-3.0 projects:

- **Anubis Client** by 4ldenz: Spotify HUD, 3D Character, HUD, and most of the visual, view and misc modules
- **LiquidBounce** by CCBlueX: Hit FX and its sound effects
- **Meteor Client** by MeteorDevelopment: Discord Presence, using Meteor's `discord-ipc` library

Fonts are under the SIL Open Font License (see the `OFL-*.txt` files next to them). Lyrics come from [lrclib.net](https://lrclib.net) at runtime and are never bundled.
