# BarelySorts plan

BarelySorts sorts inventories with the middle mouse button or the R key, and adds a sort button and a settings button to every inventory group.

## Decisions (so far)

- **Name and id:** BarelySorts, mod id `barelysorts`, group `com.itmecasey.barelysorts`.
- **License:** MIT.
- **Side:** both. Sorting runs on the server so multiplayer stays correct; the buttons, settings screen and keys run on the client.
- **Targets:** Forge and NeoForge for Minecraft 1.20.1, 1.21.1, 26.1.2, 26.2 and 26.3.
- **Output:** one combined jar per Minecraft version, not one per loader. Each jar carries the shared code plus the Forge and NeoForge loader layers, and each loader loads its own entry point and metadata.
- **Sort rules:**
  - Sort methods: *Rows* (fill left to right, then top to bottom) and *Columns* (fill top to bottom, then left to right).
  - Sort by: quantity (largest first), item name, or mod name.
  - Stackable items merge into full stacks before sorting.
  - The hotbar is never sorted and never moves.
- **Lock:** a locked group ignores sort requests.
- **Autofill:** when the last item in a hotbar slot is used up and the same item is in the main inventory, the main-inventory stack moves into that hotbar slot.

## Sort groups

- **Player inventory:** the 27 main-inventory slots. The hotbar is excluded.
- **Container screens** (chest, barrel, shulker, and so on): the container's own slots, one group per screen.

## Versions and toolchains

Carried over from the playbook where it is known. The 26.2 and 26.3 rows are not checked yet.

| Minecraft | Forge build tool | NeoForge build tool | Java |
|---|---|---|---|
| 1.20.1 | ForgeGradle 6 | NeoGradle 7 | 17 |
| 1.21.1 | ForgeGradle 6 | ModDevGradle | 21 |
| 26.1.2 | ForgeGradle 7 | ModDevGradle | 25 |
| 26.2 | to check | to check | to check |
| 26.3 | to check | to check | to check |

## Layout (as built)

```
BarelySorts/
  common/src/main/java        sort logic, settings model, autofill (no loader classes)
  common/src/test/java        unit tests for sort logic
  forge-<mc>/ neoforge-<mc>/  per-loader layers and build files (one set per Minecraft version)
  PLAN.md  PROJECT_STATUS.md  README.md  LICENSE  .gitignore
```

The first module built is `neoforge-1.21.1`, because its toolchain is already proven in ModJS. The rest follow once it builds.

## Open questions

- Client-only settings (sort method, sort by, lock) are stored on the client for now. Lock is enforced on the client; the server does not check it yet.
- Quick-move extras (ctrl-click moves one item, shift-ctrl-click moves all of that type, shift-drag) are in.
- Middle-click and R are wired to sorting; R may conflict with the recipe book key in some screens.

## Combined jar (how it is built)

- `common/` holds the shared code. Each loader module compiles it with `sourceSets` pointing at `../common/src/main/java`.
- `neoforge-1.21.1/` and `forge-1.21.1/` each build their own jar: shared classes plus their own loader layer and metadata.
- `scripts/combine_jars.py <out.jar> <loader jars...>` merges them into one jar, keeping both `META-INF/mods.toml` and
  `META-INF/neoforge.mods.toml`. Output: `dist/barelysorts-mc1.21.1-<version>.jar`.
- Loaders reach shared code only through `Platform` (config folder, sort sender) and the `ScreenActions` and `Autofill`
  entry points.

Verified so far: both loader jars compile; a headless dedicated server starts on NeoForge and on Forge with the mod loaded
(each module's own run). Not verified: the combined jar itself loaded by either loader, and client-side behavior.

Fabric is the third layer for 1.21.1 (`fabric-1.21.1/`, Loom 1.9.2, Fabric API 0.116.17+1.21.1, loader 0.16.9). Its
dedicated server needs a JDK 21 runtime: on JDK 25 the Mixin in fabric-gametest-api fails with a ClassNotFoundException
for java.lang.System. Run it with `JAVA_HOME` pointing at a JDK 21 install. Fabric reads `leftPos`/`topPos` through
`barelysorts.accesswidener`; NeoForge and Forge use `getGuiLeft()`/`getGuiTop()` through `Platform.GuiOrigin`.

## Viewer and info mod integrations

Not applicable. BarelySorts adds no items, blocks, fluids, recipes or entities, so BEI, REI, JEI, EMI, SMWTI and Jade have
nothing to show. See [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md).
