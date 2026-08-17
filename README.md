# Lectern

A mod list that stays out of the way. Shows what you have installed, finds each mod's
settings screen for you, and searches three hundred mods without stuttering.

![Minecraft](https://img.shields.io/badge/minecraft-26.1.2-brightgreen.svg)
![Loader](https://img.shields.io/badge/loader-NeoForge-orange.svg)
![Side](https://img.shields.io/badge/side-client-blue.svg)
![License](https://img.shields.io/badge/license-MIT-lightgrey.svg)

## 📖 About

Every pack ends up with a hundred-odd mods and no good way to answer "what is that one
called, and does it have settings?". Lectern answers both: one screen, a search box, and a
button that opens the mod's own settings when it has any.

It is deliberately small. There is no update checker phoning a server, no favourites system,
no badge taxonomy — just the list, the search, and the way through to a config screen.

## 📦 Installing

Drop `lectern-*.jar` into `mods/`. Client-side only; it does nothing on a server and can sit
in a server pack harmlessly.

**Java 25** is required, which is what NeoForge 26.1 runs on anyway.

## 🎮 Using it

Lectern takes over the **Mods** button NeoForge already puts on the title screen and the
pause menu -- same place, same label, it just opens Lectern instead of the loader's list.
There is no second button to hunt for.

| | |
|---|---|
| Type in the box | filters as you type, across name, id, description and authors |
| Click a row | shows that mod's details on the right |
| **Settings** | opens that mod's own settings screen, when it has one |
| **Hiding libraries** | toggles the loader and library mods in or out of the list |

## 🧰 For mod authors

**Most mods need to do nothing.** If your mod registers NeoForge's `IConfigScreenFactory` —
which is what you already do to get a settings button in the vanilla mod list — Lectern finds
it and shows a button. No dependency, no code, no manifest entry.

If you want something different, one static call from your client initialiser:

```java
if (ModList.get().isLoaded("lectern")) {
    LecternApi.register("yourmod", parent -> new YourScreen(parent));
}
```

Guarded like that, Lectern stays a soft dependency — your mod runs fine without it.

Config libraries can register on behalf of everything they handle at once:

```java
LecternApi.provide(modIdsIHandle, modId -> parent -> buildScreenFor(modId, parent));
```

There is deliberately **no entrypoint to implement**. Mod Menu asks you to declare one in your
metadata, which means you cannot add a button without editing your manifest and cannot compile
without Mod Menu present. A guarded static call needs neither.

Declare yourself a library — hidden unless the player asks — with a mod property:

```toml
[mods.modproperties.yourmod]
"lectern:library" = "true"
```

## 📝 Credit

The two mods that got here first and did it well: **[Catalogue](https://github.com/MrCrayfish/Catalogue)**
by MrCrayfish and **[Mod Menu](https://github.com/TerraformersMC/ModMenu)** by the Terraformers,
both MIT. Lectern borrows their good ideas — search, library filtering, config shortcuts,
adopting the loader's own screen registry — and leaves out the rest.

## 💻 For developers

Build instructions and the architecture are in [DEV.md](DEV.md).
