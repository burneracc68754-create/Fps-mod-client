# Lyrics Cord (neonlyrics) 3.0.0

A client-side Fabric mod for **Minecraft 1.21.11** that adds a polished in-game menu,
Spotify lyrics, animated skies, cosmetic hats, a glass HUD, a performance overlay and
an aggressive (but reversible) FPS mode.

## Requirements

| | |
|---|---|
| Minecraft | 1.21.11 |
| Fabric Loader | 0.20.1+ |
| Fabric API | 0.141.6+1.21.11 |
| Java | 21+ |

## Install

1. Install Fabric Loader for 1.21.11.
2. Drop [Fabric API](https://modrinth.com/mod/fabric-api) into `mods/`.
3. Drop `build/libs/neonlyrics-3.0.0.jar` into `mods/`.
4. Launch the game with the Fabric profile.

## Build from source

```bash
# Windows
gradlew.bat build

# macOS / Linux
./gradlew build
```

The remapped jar is written to `build/libs/neonlyrics-3.0.0.jar`.
A sources jar is produced next to it.

For development, `gradlew runClient` starts a test client with the mod loaded.

## Menu

Open the menu with **J** (rebindable in Options → Controls).

| Page | What it does |
|---|---|
| Sky | Enable/disable the custom sky and pick one of 20 sky styles |
| Lyrics | 3D or floating lyrics, follow-view, colours, font |
| Style | Cosmetic hats and lyric appearance |
| Spotify | Connect Spotify, choose playback device, sync offset |
| Display | Top bar, glass hotbar, font picker, **performance overlay** |
| Menu | Normal vs. world ("phone") menu, accent colour, blur, sounds |
| Performance | FPS presets, vanilla options, culling, adaptive render distance |
| Saved | Saved lyric lines and recently played songs |

## Performance

The **Performance** page contains three presets and fine-grained controls:

- **Maximum FPS** – 6 chunks, minimal particles, all culling on, adaptive distance.
- **Balanced** – 10 chunks, fewer particles, light culling.
- **Disable boost** – restores every vanilla option that was changed.

**Low latency mode** (in the vanilla-options group) forces raw mouse input, disables
VSync and sets a 260 FPS cap, which removes the extra frame of latency and the
mouse smoothing that vanilla applies by default.

Everything the boost touches is snapshotted first, so toggling it off puts your
original settings back. Settings are stored in `config/neonlyrics/perf.json`; the
snapshot lives in `perf-state.json`, which also allows recovery after a crash.

The top bar shows a live ping value with signal bars and a smoothed readout (it shows
`0 ms` in singleplayer because there is no network round-trip). Fine tuning includes
render/simulation distance, FPS cap, entity shadows, clouds, weather, smooth lighting,
adaptive view distance, entity/block-entity/particle culling distances and optional
name-tag culling. Render-side mixins skip distant
particles, block entities, clouds, weather and (optionally) all particles while the
boost is active, without touching world data.

The **performance overlay** (Display → Performance overlay) shows FPS, frame time,
RAM, coordinates and ping. It updates four times per second and reuses its strings,
so it costs essentially nothing per frame.

## Config

All settings are saved as JSON under `config/neonlyrics/`.
Delete that folder to reset the mod to defaults.

## License

See [LICENSE](LICENSE). Bundled fonts are licensed under the SIL Open Font License
(see `assets/neonlyrics/font/ofl.txt`).
