<div align="center">

# Venti Client

**A clean, performance-minded visual and quality-of-life client for Minecraft.**

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-62B47A?style=flat-square)
![Loader](https://img.shields.io/badge/Loader-Fabric-DBD0B4?style=flat-square)
![Java](https://img.shields.io/badge/Java-21%2B-E76F00?style=flat-square)
![License](https://img.shields.io/badge/License-GPL--3.0-8B7CF7?style=flat-square)

</div>

---

## Overview

Venti is a client-side Fabric mod that adds visual polish, a customizable HUD and everyday quality-of-life tools, all managed from one smooth, themeable interface. It is built to be fair play friendly: there are no combat, movement or X-ray features, and nothing that reveals information the vanilla game would hide.

## Highlights

- **Modern interface.** Open it with <kbd>Right Shift</kbd> or `/venti`. Choose between a sidebar layout with search and list/grid views, or classic floating panels. Fully themeable, including custom header and background images (PNG, JPG and animated GIF).
- **Now-playing HUD.** Shows the current Spotify track with album art and time-synced lyrics, with optional lyrics on screen or across the sky. Supports Korean, Japanese, Chinese and Cyrillic text.
- **Make it yours.** Replace the sky or the Totem of Undying with any picture or GIF, wear a 3D character model, and fine-tune hands, swing animations, crosshair and enchantment glint.
- **Stream-safe.** No Inv Leak swaps your items for decoys on screen so viewers can't see your gear, with a capture-proof overlay option on Windows.
- **Waypoints.** Saved per world and dimension, with labels, beams and automatic death points.

## Modules

| Category | Modules |
| --- | --- |
| **Visuals** | 3D Hand, Hand Aura, Hand Pose, Swing Animation, Slow Swing, Custom Glint, Atmosphere, Color Grading, Motion Blur, Block Overlay, Jump Circles, Floating Relics, Particles, Totem Spirit, Time Changer, Low Health Warning, Zoom, Custom FOV, Freelook, Fullbright, No Render, Custom Crosshair, Nametags |
| **Render** | 3D Character, Hit FX, Custom Sky, Custom Totem |
| **HUD** | HUD (info bar, coordinates, module list, potions, armor, cooldowns), Hotkeys, Spotify HUD |
| **Misc** | Discord Presence, No Inv Leak, Waypoints, Weather Notifier, Sprint, Coord Snapper, Name Protect, Skin Change |

Nametags follow vanilla visibility rules: they show name, held item, ping and distance, and are never drawn through walls.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft **1.21.11**.
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) for the same version.
3. Place the Venti `.jar` and Fabric API in your `.minecraft/mods` folder.
4. Launch the game and press <kbd>Right Shift</kbd>.

## Building from source

Requires JDK 21 or newer.

```bash
git clone https://github.com/mishoassenov10-prog/VentiClient.git
cd VentiClient
./gradlew build
```

The finished jar is written to `build/libs/`.

## Configuration

Settings are saved automatically to `.minecraft/config/venti/`. Pictures you pick for the theme, sky and totem are copied to `config/venti/images/`.

## License

Venti Client is free software, released under the [GNU General Public License v3.0](LICENSE). See [NOTICE](NOTICE.md) for third-party components.
