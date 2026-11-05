# Project Status
<!-- status-format: 1 -->
Updated: 2026-11-04
Summary: Sorting mod for Forge and NeoForge across five Minecraft versions, with the plan written and the first module scaffolded.
Spend: none

## Foundation
- [x] Plan and decisions written (PLAN.md) (w:2)
- [~] Repository scaffold: NeoForge 1.21.1 module with Gradle wrapper (w:2)
- [ ] Shared common code layout (common/) (w:1)
- [ ] Forge and NeoForge build files for 1.20.1, 1.21.1, 26.1.2, 26.2, 26.3 (w:4)
- [ ] Combined jar per Minecraft version (w:3)

## Sorting
- [ ] Sort logic: rows and columns, quantity, name and mod sort, stack merging (w:3)
- [ ] Hotbar excluded from sorting (w:1)
- [ ] Lock per group (w:1)
- [ ] Autofill from main inventory when the last hotbar item is used (w:3)
- [ ] Middle-click and R key sort (w:2)

## Interface
- [ ] Sort glyph and settings (hamburger) buttons on each inventory group (w:3)
- [ ] Settings screen: sort method, sort by, lock (w:3)
- [ ] Quick-move extras: ctrl-click single item, shift-ctrl-click all of type, shift-drag (w:3)

## Testing and release
- [ ] Unit tests for sort logic (w:2)
- [ ] Headless self test on each target (w:3)
- [ ] GitHub repository and CI (w:2)
- [ ] CurseForge project and first release (w:2)

## Documentation and branding
- [ ] README, LICENSE, PROJECT_STATUS kept current (w:1)
- [ ] Branding: icon and banners (w:2)
- [ ] Wiki page (w:2)
