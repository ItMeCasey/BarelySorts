# BarelySorts

Sorts inventories with the middle mouse button or the R key. Every inventory group gets two buttons: a sort glyph that
sorts it, and a hamburger that opens its settings.

## Features

- **Sort:** middle-click a group, press R over it, or press the sort glyph.
- **Settings** (hamburger): sort method, sort by, and lock.
  - Sort method: *Rows* fills left to right, then top to bottom. *Columns* fills top to bottom, then left to right.
  - Sort by: quantity, item name, or mod name.
  - Lock: a locked group is not sorted.
- Stackable items merge into full stacks before sorting.
- The hotbar is never sorted and never moves.
- **Autofill:** when the last item in a hotbar slot is used up, the same item moves in from the main inventory.
- **Quick-move:** ctrl-click moves one item, shift-ctrl-click moves every item of that type, and holding shift and the left mouse button while dragging moves each stack you pass.

## Versions and loaders

One combined jar per Minecraft version works on Forge, NeoForge and Fabric: 1.20.1, 1.21.1, 26.1.2, 26.2 and 26.3.
On 1.20.1, NeoForge uses the Forge 1.20.1 jar. Install it on both the client and the server.

## Tested

Played by hand in a game client: NeoForge 1.21.1, Forge 1.20.1, NeoForge 26.1.2 and NeoForge 26.3.

A scripted client tour (sort button, middle-click, settings popup, slot locks, mass delete with the confirm dialog, autofill) ran on every other loader and version: Forge 1.21.1, 26.1.2, 26.2 and 26.3; NeoForge 26.2 (and 26.3 again); and Fabric on 1.20.1, 1.21.1, 26.1.2, 26.2 and 26.3. Those are not played by hand yet.

The 1.20.1 jar (2026.11.3) is reobfuscated to the names a real game uses and was loaded in real Forge 47.4.28 and NeoForge 47.1.106 installs (mod found and constructed, main menu reached); NeoForge 1.20.1 uses that same jar. Bug reports for any of them are welcome.

## Known gaps

- Middle-click and R sort the group under the mouse. R may conflict with the recipe book key in some screens.
- Vanilla crafting grids and machines, creative screens and Applied Energistics 2 screens are deliberately left alone.

## Other mods

BarelySorts adds no items, blocks, recipes or entities, so it has no BEI, REI, JEI, EMI, SMWTI or Jade integration. See
[docs/COMPATIBILITY.md](docs/COMPATIBILITY.md).

## License

MIT. See [LICENSE](LICENSE).
