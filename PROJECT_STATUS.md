# Project Status
<!-- status-format: 1 -->
Updated: 2026-11-04
Summary: NeoForge 1.21.1 build compiles with sorting, buttons, settings, lock and autofill; other loaders and versions are next.
Spend: none

## Foundation
- [x] Plan and decisions written (PLAN.md) (w:2)
- [x] Repository scaffold: NeoForge 1.21.1 module with Gradle wrapper (w:2)
- [ ] Shared common code layout (common/) (w:1)
- [ ] Forge and NeoForge build files for 1.20.1, 1.21.1, 26.1.2, 26.2, 26.3 (w:4)
- [ ] Combined jar per Minecraft version (w:3)
- [ ] Fabric target (w:4)

## Sorting
- [x] Sort logic: rows and columns, quantity, name and mod sort, stack merging (w:3)
- [x] Hotbar excluded from sorting (w:1)
- [x] Lock per group, client-side only (w:1)
- [x] Autofill from main inventory when the last hotbar item is used (w:3)
- [x] Middle-click and R key sort (w:2)

## Interface
- [x] Sort glyph and settings (hamburger) buttons on each inventory group (w:3)
- [x] Settings screen: sort method, sort by, lock (w:3)
- [x] Quick-move extras: ctrl-click single item, shift-ctrl-click all of type, shift-drag (w:3)

## Testing and release
- [ ] Unit tests for sort logic (w:2)
- [ ] Headless self test on each target (w:3)
- [x] GitHub repository (public, ItMeCasey/BarelySorts) (w:2)
- [ ] GitHub CI workflow (w:2)
- [~] CurseForge project created (id 1784568), first release not yet (w:2)

## Documentation and branding
- [ ] README, LICENSE, PROJECT_STATUS kept current (w:1)
- [x] Branding: icon, banners, CurseForge description and listing kit (w:2)
- [x] Wiki (9 pages) written, deployed and checked live at itmecasey.com/wiki/barelysorts (w:2)
