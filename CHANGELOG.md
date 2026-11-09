# Changelog

## 2026.11.3 (Minecraft 1.20.1 only)

Fixes the 1.20.1 jar, which crashed a real game. The other four Minecraft versions are unchanged and stay at 2026.11.2.

- 1.20.1 (Forge and NeoForge): the Forge layer was built with `reobf = false`, so the jar kept Minecraft's development names. A development run hides that, but a real Forge or NeoForge 1.20.1 game only knows Forge's own names and would stop with `NoSuchFieldError` or `NoSuchMethodError`. The jar is now reobfuscated (119 Forge-name references in the class files, none before).
- Loaded in a real Forge 47.4.28 install and a real NeoForge 47.1.106 install: the mod was found, `ForgeBarelySorts` was constructed and the main menu was reached with no linkage errors.
- Releases now upload only the Minecraft versions whose modules carry the tag's version (the release and CI checks require agreement per Minecraft version instead of across all modules), so v2026.11.3 uploads the 1.20.1 jar only.

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
