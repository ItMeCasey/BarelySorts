# Project Status
<!-- status-format: 1 -->
Updated: 2026-11-05
Summary: 2026.11.1 is released; 2026.11.2 (Forge pack metadata and 26.x delete-dialog counts fixed) is committed locally and every layer but NeoForge 1.20.1 passed a scripted client tour.
Spend: none

## Foundation
- [x] Plan and decisions written (PLAN.md) (w:2)
- [x] Repository scaffold with Gradle wrappers (w:2)
- [x] Shared common code layout (common/ plus version-<mc>/) (w:1)
- [x] Forge and NeoForge build files for 1.20.1 (Forge jar), 1.21.1, 26.1.2, 26.2, 26.3 (w:4)
- [x] Combined jar per Minecraft version (scripts/combine_jars.py) (w:3)
- [x] Fabric target for all five versions (w:4)

## Sorting
- [x] Sort logic: rows and columns, quantity, name and mod sort, stack merging (w:3)
- [x] Hotbar excluded from sorting (w:1)
- [x] Per-slot locks, chosen by the player (w:2)
- [x] Autofill from main inventory, tool swap at 1 durability or when broken (w:3)
- [x] Middle-click and R key sort, rebindable (w:2)
- [x] Compatibility: largest-container detection, vanilla workstations and AE2 ignored (w:2)

## Interface
- [x] Sort glyph and settings (hamburger) buttons on each inventory group (w:3)
- [x] Settings popup: sort method, sort by, locked slots, tool swap (w:3)
- [x] Quick-move extras: ctrl-click, ctrl+shift-click, shift-click stack, shift-drag (w:3)
- [x] Hold X to mark slots and delete with a confirm dialog (w:3)

## Testing and release
- [x] Unit tests for the shared logic (w:2)
- [x] Headless dedicated-server load on every layer (w:3)
- [x] Client testing: played by hand on four layers; scripted client tour passed on Forge 1.21.1/26.1.2/26.2/26.3, NeoForge 26.2, Fabric all five versions; NeoForge 1.20.1 not run (w:4)
- [x] Dev-only client tour (common/src/devtest, scripts/run-tour.ps1) with frames in docs/frames (w:2)
- [ ] Release 2026.11.2 (version bumped in all modules, awaiting push, tag and CurseForge upload) (w:2)
- [x] GitHub repository (public, ItMeCasey/BarelySorts) (w:2)
- [x] GitHub CI workflow: build all layers, test, combine jars (w:2)
- [x] Release workflow and CurseForge upload script (publish off by default) (w:2)
- [x] CurseForge project created (id 1784568); 2026.11.1 released, all five files approved (w:2)

## Documentation and branding
- [x] README, CHANGELOG, LICENSE and CurseForge description match the tested state (w:1)
- [x] Branding: icon, banners, CurseForge description and listing kit (w:2)
- [x] Compatibility standard: BEI, REI, JEI, EMI, SMWTI and Jade not applicable, no content added (docs/COMPATIBILITY.md) (w:1)
- [x] Wiki (9 pages) written, deployed and checked live at itmecasey.com/wiki/barelysorts (w:2)
