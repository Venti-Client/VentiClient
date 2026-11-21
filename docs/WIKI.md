# Venti Client Wiki

Everything you need to install, set up and get the most out of Venti Client.

- [Getting started](#getting-started)
- [The menu](#the-menu)
- [Modules](#modules)
- [Keys and commands](#keys-and-commands)
- [Pictures and themes](#pictures-and-themes)
- [Where settings are saved](#where-settings-are-saved)
- [Troubleshooting](#troubleshooting)

---

## Getting started

1. Install **Fabric Loader** for your Minecraft version from [fabricmc.net](https://fabricmc.net/use/).
2. Download **Fabric API** for the same version and the **Venti jar** that matches your Minecraft version.
3. Put both jars in your `.minecraft/mods` folder.
4. Start Minecraft with the Fabric profile and press <kbd>Right Shift</kbd> to open the menu.

Each Venti jar only loads on the Minecraft version in its file name, for example `venti-1.0.0+mc1.21.11.jar` for 1.21.11.

## The menu

Open it with <kbd>Right Shift</kbd> or by typing `/venti` in chat. Close it with <kbd>Esc</kbd>.

There are two layouts. Switch between them under **Theme → GUI Style**.

| Layout | How it works |
| --- | --- |
| **Venti** | A sidebar with the categories (Visuals, Render, HUD, Misc), a search box and list or grid view. Click a module card to open its settings page, or use the switch on the card to turn it on or off. |
| **Panels** | One floating panel per category. Click a module to turn it on or off; click its arrow (or right-click it) to open its settings underneath. Drag panels anywhere; their positions are remembered. |

On every settings page:

- A small dot next to a setting means you changed it from the default.
- **Reset all** puts the module back to its defaults.
- **Bind** sets a key that toggles the module. Press <kbd>Esc</kbd> or <kbd>Backspace</kbd> while binding to clear it.

## Modules

### Visuals

| Module | What it does |
| --- | --- |
| **Viewmodel Arm** | Shows your own arm holding items in first person. |
| **Hand Glow** | A glowing outline around your hand and held item. |
| **Viewmodel** | Move, rotate and resize your first-person hands. |
| **Swing Animation** | Pick a different first-person swing style. |
| **Swing Speed** | Make your hand swing faster or slower. |
| **Custom Glint** | Recolor the enchantment glint and change its speed and pattern size. |
| **Aurora Sky** | A custom night sky with stars, a Milky Way and auroras, plus sky shaders. Turns itself off while Custom Sky is on. |
| **Color Grading** | Adjust vibrance, contrast, brightness, warmth and vignette. |
| **Motion Blur** | Blur when you turn the camera. |
| **Block Overlay** | A smooth animated outline and fill on the block you look at. |
| **Jump Circles** | A fading ring where you jump. |
| **Orbit Orbs** | Glowing orbs that float around you and leave a trail. |
| **Effect Particles** | Stylized particles for totem pops, projectiles and footsteps. |
| **Totem Ghost** | A glowing ghost rises when a player pops a totem. |
| **Time Changer** | Set the time of day on your screen only. |
| **Low Health Warning** | The screen edges glow when your health is low. |
| **Zoom** | Hold <kbd>C</kbd> to zoom; scroll to change how far. |
| **Custom Fov** | Set your field of view beyond the vanilla slider. |
| **Freelook** | Hold <kbd>Left Alt</kbd> to look around without turning your character. |
| **Fullbright** | See clearly in the dark. |
| **No Render** | Hide overlays like fire, pumpkin and nausea, and other effects. |
| **Custom Crosshair** | Choose the crosshair's style, size, gap, color and outline. |
| **Nametags** | Restyled player nametags with held item, ping and distance. They follow vanilla visibility and never show through walls. |

### Render

| Module | What it does |
| --- | --- |
| **Avatar** | Replace your player model with a 3D character: Sakura, Luna, Mika, Aoi, or your own `.glb` model. |
| **Hit FX** | Particles and custom sounds when your hits land or when you get hit, with sound volume up to 500%. |
| **Custom Sky** | Wrap any picture or animated GIF around the sky. |
| **Custom Totem** | Replace the Totem of Undying, and its pop animation, with any picture or GIF. |

### HUD

| Module | What it does |
| --- | --- |
| **HUD** | Info bar (FPS, clock, ping), coordinates, module list, potions, your armor and item cooldowns. |
| **Hotkeys** | A panel listing the modules you bound to keys. |
| **FPS Counter** | Your FPS on a rounded card that matches the Spotify HUD. |
| **Spotify HUD** | What Spotify is playing, with album art, playback buttons and time-synced lyrics. Lyrics can also be shown on screen (Screen Lyrics) or across the sky (Sky Lyrics), with sung words highlighted. Supports Korean, Japanese, Chinese and Cyrillic. |

**Moving HUD elements:** open the HUD, Hotkeys, FPS Counter or Spotify HUD page in the menu, then drag the elements on screen. Scroll over an element to resize it.

### Misc

| Module | What it does |
| --- | --- |
| **Discord Presence** | Shows Venti as your Discord status with rotating lines you can edit. |
| **Emojis** | Type `:sob:`, `:skull:`, `:fire:` and about 250 more codes in chat and they're sent as emoji. Add your own as `name=emoji`. |
| **Stream Hider** | Hides your real items on screen for streams and screen shares, with several decoy styles. **Stream Only** (Windows) keeps your real items visible to you alone. |
| **Waypoints** | Saved per world and dimension, with labels, beams and automatic death points. Press <kbd>B</kbd> to add one where you stand. |
| **Weather Notifier** | A small popup when it starts raining. |
| **Sprint** | Keeps you sprinting, like vanilla Toggle Sprint. |
| **Copy Coords** | Copy the coordinates you look at or stand on with one key. |
| **Name Protect** | Show aliases instead of player names on your screen. |
| **Skin Change** | Wear any player's skin, visible to you only. |

## Keys and commands

All keys can be changed in **Options → Controls → Venti**.

| Key or command | Action |
| --- | --- |
| <kbd>Right Shift</kbd> or `/venti` | Open the menu |
| <kbd>C</kbd> (hold) | Zoom |
| <kbd>Left Alt</kbd> (hold) | Freelook |
| <kbd>B</kbd> | Add a waypoint |
| `/waypoint add [name]`, `/waypoint remove <name>`, `/waypoint list` | Manage waypoints from chat |
| Unbound by default | Spotify play/pause, next and previous; Copy Coords |

## Pictures and themes

Several settings take a picture: Theme header and background, Custom Sky and Custom Totem.

- Click **Browse** next to the setting and pick a **PNG, JPG or animated GIF**.
- Venti copies the picture into `config/venti/images/`, so you can delete or move the original.
- Click **X** to clear it.

**Theme** also sets the accent (menu) color, panel color and opacity, corner radius and UI scale.

## Where settings are saved

Everything lives in `.minecraft/config/venti/`:

| Folder or file | Contents |
| --- | --- |
| `modules/` | One file per module with its settings, on/off state and key |
| `images/` | Pictures you picked |
| `characters/` | Your own `.glb` Avatar models |
| `waypoints/` | Waypoints, one file per world or server |
| `hud.json` | Positions of HUD elements |
| `totem-pack/` | The resource pack Custom Totem generates |

To reset Venti completely, close Minecraft and delete the `config/venti` folder.

## Troubleshooting

**Venti doesn't load.** Check that the Venti jar matches your Minecraft version exactly and that Fabric API is installed.

**The menu doesn't open.** Check that <kbd>Right Shift</kbd> isn't bound to something else in **Controls**, or type `/venti` in chat.

**Spotify HUD says "Spotify is not open".** Start the Spotify desktop app and play something. Venti reads the desktop app, not the web player.

**No lyrics show up.** **Lyrics Online** must be on, and the song has to exist on lrclib.net with synced lyrics. Less popular songs often aren't there.

**Discord status shows the wrong name or no picture.** Create your own application in the Discord Developer Portal, upload a picture named the same as **Large Image Key**, and paste the app's ID into **Application ID**.

**Custom Totem didn't change.** Wait a second after picking the picture while resources reload. Another resource pack that also changes the totem can override it.

**Something looks wrong or crashes.** Open an issue with your `logs/latest.log` attached.
