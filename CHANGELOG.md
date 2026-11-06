# Changelog

## 2026.11.2

- Forge on 1.21.1 and 26.x: ship the pack metadata. Without it the pixel font did not load on 1.21.1 (empty boxes in menus and the delete dialog) and 26.x could crash on the first block drop.
- 26.1.2, 26.2, 26.3: the delete confirmation shows each stack's count again.
- Checked with a scripted client tour on every loader and version except NeoForge 1.20.1; see the README for what has been played by hand.

## 2026.11.1

First release. One combined jar per Minecraft version (1.20.1, 1.21.1, 26.1.2, 26.2, 26.3) for Forge, NeoForge and Fabric.

- Sort any inventory group with a button, middle-click or R; rows or columns, by quantity, item name or mod name; stacks merge first.
- Per-slot locks, hotbar autofill with optional tool swap, hold-X mass delete with confirmation, and quick-move shortcuts.
- Ignores vanilla workstations, creative screens and Applied Energistics 2; works with large modded containers.
- Tested in a game client: NeoForge 1.21.1, Forge 1.20.1, NeoForge 26.1.2, NeoForge 26.3. The other loaders and versions load on a headless server but are not yet played.
