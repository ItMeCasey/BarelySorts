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

## Status

- **Minecraft 1.21.1 NeoForge:** first build. Compiles. Not yet run in a game client.
- Forge, other Minecraft versions, and Fabric are planned. See [PLAN.md](PLAN.md).

## Known gaps

- Lock is enforced on the client only.
- Middle-click and R sort the group under the mouse. R may conflict with the recipe book key in some screens.

## License

MIT. See [LICENSE](LICENSE).
