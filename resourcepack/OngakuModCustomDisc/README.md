# Somewhat Sophisticated Vinyl Decor : Custom Design Template

It's a texture template for [SS Vinyl Decor](https://modrinth.com/mod/ssv-decor/)

as of version 1.1.0 of [SS Vinyl Decor](https://modrinth.com/mod/ssv-decor/), the displays on Rack/Box/Wall Mounts can utilize dedicated texture.

## this includes

- Disc Display Textures for Cat - C418
``` 
assets\ongakumod\textures\vvs_decor_custom\music_disc_cat.png
```
- Album/Cover/Sleeve Display Textures for Cat - C418 
``` 
assets\ongakumod\textures\vvs_decor_custom\music_disc_cat_cover.png
```

## Technical Note
- The system check for files in assets\ongakumod\textures\vvs_decor_custom\, specifically looking for **[disc id].png** AND **[disc id]_cover.png**
- if both files isnt present it will default to using the color sample method.
- [disc id] ive seen usually looks like "music_disc_cat", "music_disc_13", "music_disc_creator". can check by hovering on music disc in F3+H