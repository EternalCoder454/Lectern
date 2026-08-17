# Lectern — developer notes

A mod list for **Minecraft 26.1.2** on **NeoForge**. Player-facing documentation is in
[README.md](README.md); this is everything else.

## 🛠️ Building

```bash
export TMP=C:/gtmp TEMP=C:/gtmp   # required, see below
./gradlew.bat build
```

**Set `TMP`/`TEMP` first, every time.** Gradle talks to its workers over a loopback socket,
and on a path with a space in it they fail to start with an error that reads like a
build-script fault and is not one. `org.gradle.jvmargs` also pins `-Djava.net.preferIPv4Stack=true`
for the same class of problem.

## 🗂️ Layout

Two source sets, and the split is enforced rather than conventional:

```
src/main/java/net/lectern/          names no mod loader
src/neoforge/java/net/lectern/      the only place allowed to
```

`checkMainIsLoaderNeutral` fails the build if `src/main` imports `net.neoforged`,
`net.fabricmc` or `net.minecraftforge`. This matters because ModDevGradle puts Minecraft
*and* NeoForge on the main compile classpath — a stray import there compiles happily and
only fails once somebody builds another target.

Adding a loader is one new source set with an entry point and a `ModSource`. Nothing in
`src/main` changes.

## 🏗️ Architecture

Four pieces, and the shape is deliberate:

| | |
|---|---|
| `ModEntry` | one mod, flattened to a record. No loader types survive into it. |
| `ModSource` | the single-method interface a loader implements. The only thing that needs one. |
| `ModIndex` | the list, sorted once, plus the search over it |
| `LecternApi` | how a mod gets a settings button |

The screen holds no loader types and no mod-loader knowledge. That is what keeps a second
loader to a second `ModSource` rather than a second copy of the menu.

## ⚡ Performance

Three decisions, all of them about the same thing: a menu is redrawn every frame it is open
and a search runs on every keystroke, so anything costing more than a field read has to
happen before either.

**Search allocates nothing per query.** Each `ModEntry` carries its searchable fields
pre-joined and pre-lowercased at index time. A query is one `toLowerCase` for the query
itself plus a `contains` per mod — no per-mod string building, no regex, no streams. Results
are written into a caller-owned list, so holding the search box open does not allocate a list
per keystroke.

**The index is built once**, at client setup, and sorted there. Building it lazily on first
open would move that work to the exact moment somebody clicks the button. The screen never
sorts.

**Rows are drawn, not delegated.** Vanilla's `ObjectSelectionList` allocates an entry object
per row and keeps it for the life of the screen — fine for a settings menu with nine rows,
wasteful for a mod list with three hundred of which a dozen are visible. Drawing the visible
rows straight from the filtered list means the screen holds no per-row state: scrolling
changes an int, and scrolling past three hundred mods costs what scrolling past ten does.

## 🔌 The API, and why it has no entrypoint

Mod Menu asks mods to declare a `ModMenuApi` entrypoint in their metadata. That works, and it
means a mod cannot register a screen without editing its manifest, and cannot compile without
Mod Menu on the classpath.

Lectern uses a static call instead. Guarded by a mod-loaded check it costs the caller a soft
dependency and nothing else:

```java
if (ModList.get().isLoaded("lectern")) {
    LecternApi.register("yourmod", parent -> new YourScreen(parent));
}
```

Nothing in `net.lectern.api` touches a Minecraft class except `ConfigScreenFactory`, which
returns a `Screen` because it has to.

Most mods never call it. `LecternNeoForge.adoptLoaderScreens` walks every mod at client setup
and registers a button for anything that already told NeoForge about a config screen through
`IConfigScreenFactory` — the loader's own registry, which mods with settings generally already
populate. For those, Lectern is a different route to a screen that was always there. Explicit
registrations win, so a mod can override what Lectern shows.

## 🔀 26.1.2 moved twice

Both found by disassembling the jar rather than guessing, and both worth knowing if this is
ported forward:

- `GuiGraphics` is gone. Screens now take `GuiGraphicsExtractor` and the render pass is split
  into extract-then-render, so the hook is `extractRenderState`, not `render`.
- `mouseClicked(double, double, int)` is gone, replaced by
  `mouseClicked(MouseButtonEvent, boolean)` — coordinates and button live on the event.

## 🔘 The button

Lectern does not add a button. NeoForge patches `TitleScreen` and `PauseScreen` to carry one
already -- both reference `fml.menu.mods` and `ModListScreen` in the patched jar -- and a
second button with the same label doing nearly the same thing is a worse menu than either
alone. So `LecternButtons` finds that one, takes its exact bounds, and puts Lectern behind it.

Through `ScreenEvent.Init.Post`, not a mixin. Nothing here needs bytecode: the event hands
over the finished widget list plus `addListener`/`removeListener`, which is precisely the
access required. A mixin would have to track every layout change and would collide with the
other mods that rearrange those two screens.

The button is matched on the **translation key** of its message, not the rendered text.
Matching "Mods" as a string works in English and quietly stops working in every other
language -- the sort of bug only ever reported by the people least able to describe it.

If no such button is found, Lectern adds its own to the title screen and logs why. That means
another mod removed it or the key changed; either way a mod list with no way to open it is
worse, and the duplication this exists to avoid cannot happen when the original is missing.

## 📜 Licence

MIT. Catalogue and Mod Menu are both MIT and both were read while writing this; no code was
copied from either.
