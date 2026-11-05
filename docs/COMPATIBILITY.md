# Compatibility with viewers and info mods

The playbook's compatibility standard covers mods that add items, blocks, fluids, recipes or entities. BarelySorts adds
none of them: it only changes how existing inventories are sorted, so it has nothing to show in a viewer or an info
overlay and no integration code was written. Checked on 2026-11-05.

| Integration | Needed? | 1.20.1 | 1.21.1 | 26.1.2 | 26.2 | 26.3 |
|---|---|---|---|---|---|---|
| BEI (Barely Enough Items) | Not applicable: no items, blocks, fluids or recipes | n/a | n/a | n/a | n/a | n/a |
| REI | Not applicable | n/a | n/a | n/a | n/a | n/a |
| JEI | Not applicable | n/a | n/a | n/a | n/a | n/a |
| EMI | Not applicable | n/a | n/a | n/a | n/a | n/a |
| SMWTI | Not applicable: no blocks or entities | n/a | n/a | n/a | n/a | n/a |
| Jade | Not applicable | n/a | n/a | n/a | n/a | n/a |

## What was checked instead

The only way sorting touches a viewer is the keyboard and mouse. The sort key (R) and middle-click act only when the
mouse is over an inventory group the mod sorts; anywhere else, including a viewer's item list, the input is passed on
untouched. Viewer recipe transfers and item cheating are not intercepted. This was confirmed by reading the code; it has not
been client-tested with a viewer installed. Players can rebind the sort key in Controls if R clashes with a viewer
shortcut over their inventory.
