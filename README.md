# ![Somewhat Sophisticated Vinyl Decor](https://github.com/AzulConspirator/OngakuMod/blob/main/img/proj_title.png?raw=true)

A mod that adds decorative and functional blocks focused on improving the music experience in Minecraft.

Made for NeoForge 1.21.1

## Features

### Disc Displays

```txt
Includes Disc Racks, Disc Boxes, and Wall Mounts.
```

- Store and display your music disc collection
- Displayed discs are dynamically color-sampled from their textures
- should work with modded music discs (texture depended)

![Disc Display Image](https://cdn.modrinth.com/data/xVfyaI9r/images/b0e62f0aecab612cca3f72274e0e6500d6a0cab0.png)

### Sound Box

- A jukebox controller block that connects to nearby Disc Displays.
- Browse connected music discs through a dedicated interface
- Play discs directly without manually swapping them,
Automatically returns discs to their original storage slot after playback

![Sound Box Image](https://cdn.modrinth.com/data/xVfyaI9r/images/9f0c9404743a205e2abbcd60aed8732876525588.png)

## Usage

To connect a Sound Box, crouch + right-click it with a **Tuning Fork**, then right-click a Disc Display. Connected displays will appear in the Sound Box interface.

The Sound Box works by physically moving discs between connected storage and a nearby jukebox. Discs are returned to their original slot after playback whenever possible. If the original slot is unavailable, it will use another empty slot or eject the disc from the jukebox as a fallback.

Disc Displays can also be used independently without a Sound Box.

## Technical Notes

- Music disc detection is based on the JUKEBOX_PLAYABLE item component
- Disc color rendering uses model [TintIndex](https://minecraft.wiki/w/Model), thus output can look wonky.
- Multiplayer/server stability is still being tested, especially for networking-related features
- During the creation of this mod, it started with **OngakuMod**, internally it still uses that name such as Repo name, item ID and such.

## Credit

- Made it Solo Babyyy!!
- Inspired by [Sophisticated Storage by P3pp3rF1y](https://modrinth.com/mod/sophisticated-storage). this no relation to that mod or its dev.

## License

this mod is under CC-BY 4.0.

feel free to include in modpacks, videos, etc. Just link back to this page if possible For any general queries, DM me on Discord (prof_bones).

```txt
also if it seems to be silent more than a year with no updates to the most recent standard version of Modded MC (I use cobblemon & Create as a good measure), consider this abandoned by me then feel free to take over, contact me first tho.
```

⚠ This mod ONLY exists on Modrinth as of 2026. Any sites hosting this mod outside of Modrinth are not official releases. ⚠
