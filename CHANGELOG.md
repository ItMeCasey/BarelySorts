# Changelog

## 2026.11.4 (all Minecraft versions: 1.20.1, 1.21.1, 26.1.2, 26.2, 26.3)

- Tooltips are always on top now. The sort and settings buttons, the settings popup, slot locks and the delete dialog used to be drawn over item tooltips. On 1.20.1 and 1.21.1 the overlay now sits just below the tooltip layer (still above item counts). On 26.x it is drawn before the game queues its tooltip: NeoForge uses the screen's Foreground event, Forge the container Foreground event, and Fabric the `afterForeground` event (26.2, 26.3) or a small mixin (26.1.2, whose Fabric API has no such event yet).
- Shift-click on a crafting result slot crafts the full stack again, in the 2x2 inventory grid, the crafting table and modded crafters. Barely Sorts took over every Shift-click, but its own quick move cannot take items out of a result slot, so the click did nothing. Output slots (result slots and any slot that refuses its own item) are now left to the game for Shift-click, Ctrl-click and Shift-drag, and the server-side quick move skips them.
- Checked in a real client (isolated desktop): before the fix the mod swallowed the Shift-click on both the 2x2 and 3x3 result; after it, 16 crafting tables or chests arrive in the inventory. Tooltip order checked in frames on NeoForge 1.21.1, Forge 1.20.1, NeoForge, Forge and Fabric 26.3/26.2 and Fabric 26.1.2.
- Optional ModJS scripting: with ModJS installed, startup and server scripts get a `BarelySorts` global. Events: `onSort` (cancellable, can change method and key), `onSorted`, `onQuickMove`, `onDelete`, `onAutofill` (cancellable). Reads: `sortMode`, `lockedSlots`, `toolSwap`. Content: `category`, `categoryOrder`, `registerSortOrder`, `useSortOrder`. Without ModJS nothing is loaded. See docs/scripting.md.
- Hints removed from the screens: the sort and settings button tooltips are now just "Sort" and "Settings", the "Click slots to lock. Esc to finish." line above a group while choosing locks is gone, the delete dialog no longer says "This cannot be undone.", and the settings row reads "Locked: N" instead of "Locked: N (choose)".
- Dev tour: `BS_TOUR_PART=extras` runs only the tooltip-order and crafting checks, and `scripts/run-tour.ps1` now starts the client through the isolated launcher.

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
