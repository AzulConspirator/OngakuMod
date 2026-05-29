# Somewhat Sophisticated Vinyl Decor

A mod that adds decorative and functional blocks focused on improving the music experience in Minecraft.

Made for NeoForge 1.21.1

## Features

### Disc Displays

```txt
Includes Disc Racks, Disc Boxes, and Wall Mounts.
```

- Store and display your music disc collection
- Displayed discs are dynamically color-sampled from their textures
- Designed to work with most modded music discs with JUKEBOX_PLAYABLE support

[Disc Display Image]()

### Sound Box

- A jukebox controller block that connects to nearby Disc Displays.
- Browse connected music discs through a dedicated interface
- Play discs directly without manually swapping them,
Automatically returns discs to their original storage slot after playback

[Disc Display Image]()

## Technical Notes

- Music disc detection is based on the JUKEBOX_PLAYABLE item component
- Disc color rendering uses model ColorIndex sampling
- Multiplayer/server stability is still being tested, especially for networking-related features
