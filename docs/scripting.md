# Scripting with ModJS

With ModJS installed, startup and server scripts get a global `BarelySorts`. ModJS is optional: without it nothing is loaded and nothing changes. Client scripts do not get the global, because sorting is decided on the server.

Players are named by name or UUID text. A script that throws is logged in one line and skipped; the sort still happens. Loading server scripts again (`/modjs reload`) replaces what they registered.

## Events

Every event runs on the server thread. Events marked cancellable have `cancel()`.

| Call | Fires | Event fields |
|---|---|---|
| `onSort(fn)` | before a group is sorted, cancellable | `player`, `uuid`, `group` (`player` or `container`), `method` and `key` (both can be changed for this sort), `lockedSlots` |
| `onSorted(fn)` | after a group was sorted | `player`, `uuid`, `group`, `method`, `key`, `slots` |
| `onQuickMove(fn)` | before a Ctrl-click or Shift-click quick move, cancellable | `player`, `uuid`, `mode` (`stack`, `one`, `type`), `slot`, `item` |
| `onDelete(fn)` | before marked stacks are deleted, cancellable | `player`, `uuid`, `items` |
| `onAutofill(fn)` | before the hotbar refills a slot or swaps a worn tool, cancellable | `player`, `uuid`, `hotbarSlot`, `fromSlot`, `item`, `reason` (`refill` or `swap`) |

`method` is `ROWS` or `COLUMNS`; `key` is `QUANTITY`, `NAME` or `MOD`. An `item` has `id`, `namespace`, `name` and `count`.

## Reading a player's state

| Call | Returns |
|---|---|
| `sortMode(player, group)` | `{method, key}` the player's client last asked for in that group, or `null` before their first sort |
| `lockedSlots(player, group)` | the slot ids that were locked at that sort (player slots use inventory index 0 to 35, container slots use the menu index) |
| `toolSwap(player)` | whether the player has tool swap at 1 durability on |

## Content

| Call | Does |
|---|---|
| `category(name, patterns)` | defines a category from item ids or globs (`"minecraft:*_ingot"`, `"mymod:*"`); stacks sort by category first, then by the player's key |
| `categoryOrder(names)` | the order of categories; unlisted ones follow in the order they were defined |
| `registerSortOrder(name, fn)` | adds a named order, `(a, b) => number`, negative when `a` goes first |
| `useSortOrder(name)` | uses that order instead of the player's key for every sort; `null` stops |

## Example

```js
// server_scripts/sorting.js
BarelySorts.category('ores', ['minecraft:*_ingot', 'minecraft:diamond', 'minecraft:emerald'])
BarelySorts.category('food', ['minecraft:apple', 'minecraft:bread', 'minecraft:cooked_*'])
BarelySorts.categoryOrder(['food', 'ores'])

// no sorting while a player is in the nether
BarelySorts.onSort(e => { if (e.player == 'Builder') e.cancel() })

// never delete a stack of netherite
BarelySorts.onDelete(e => {
  if (e.items.some(i => i.id == 'minecraft:netherite_ingot')) e.cancel()
})
```
