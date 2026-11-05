# BarelySorts listing kit: CurseForge

Everything you need to fill in the CurseForge project form. Files are in `branding/icon/` and `branding/banners/`.

## Project fields

| Field | CurseForge |
|---|---|
| Project ID | 1784568 |
| Name | BarelySorts |
| Slug / URL | `barelysorts` |
| Icon / logo | `icon/icon_512.png` (square, at least 400x400) |
| Class | Mods |
| Main category | Utility & QoL |
| More categories | Inventory Management, if CurseForge offers it |
| Loader | NeoForge (Forge to follow) |
| Game versions | Set on each uploaded file |
| Environment | Client and server |
| License | MIT |
| Source | https://github.com/ItMeCasey/BarelySorts |
| Issues | https://github.com/ItMeCasey/BarelySorts/issues |

### One-line description

> Sort any inventory with one click: a sort button and a settings button on every chest, barrel and your own pack.

Use it for the CurseForge summary, social posts and the top of the description.

### Gallery

| File | Use |
|---|---|
| `banners/features.png` (1920x1080) | First gallery image |
| `banners/sorting.png` (1920x1080) | Before and after, second gallery image |
| `banners/social.png` (1280x640) | GitHub social preview (repo Settings > Social preview), third gallery image |
| `banners/banner.png` (800x150, saved at 2x) | Top of the description |
| `banners/sort_strip.png` (800x150, saved at 2x) | Before and after row inside the description |
| `banners/h_*.png` (800x46, saved at 2x) | Section headers inside the description |

The panels in these images are illustrations drawn for the branding, not game screenshots. Add real in-game screenshots to the gallery once the mod has been checked in a client.

### Description

The CurseForge description is in [`curseforge/DESCRIPTION.md`](../curseforge/DESCRIPTION.md). Paste it with the description type set to **Markdown**. Its images load from GitHub, so push `branding/` first.

## Checklist

1. Push `branding/` to GitHub so the description images load.
2. Create the CurseForge project, with `icon_512.png` as the logo (project id 1784568 is recorded above). New projects are reviewed before they go live.
3. Set `banners/social.png` as the GitHub social preview.
4. Add `icon/barelysorts_logo.png` to the jar as the mod list logo (`logoFile` in the mod metadata, file in `src/main/resources/`).
