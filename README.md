# Mouse Mixer

A client-side Minecraft mod that enhances container and inventory management with powerful mouse interactions.

**Minecraft 1.21 · NeoForge**

---

## Features

### Container Sorting
Middle-click any container to sort its contents. Three sort modes are available: by registry ID, display name, or item quantity. The hotbar is never affected.

### Scroll Transfer
Scroll the mouse wheel over a slot to move stacks between a container and your inventory — no more shift-clicking one stack at a time.

### Modifier Clicks
- **Ctrl + Click** — move all matching items at once
- **Alt + Click** — move a single item

### Shift Drag
Hold Shift and drag across slots to quick-move items in one gesture.

### Stack Refill
When the stack in your main hand or off-hand runs out, your inventory is searched automatically for a matching replacement.

### Quick Craft
Right-click a recipe in the recipe book to craft without opening a crafting table:
- **RMB** — craft 1
- **Shift + RMB** — craft a full stack
- **Ctrl + Shift + RMB** — craft as many as possible

---

## Configuration

All features can be toggled individually. The config file is generated at first launch under `config/mousemixer-client.toml`.

| Option | Default | Description |
|---|---|---|
| Sorting | Enabled | Toggle container sorting |
| Sort Mode | `RAW_ID` | `RAW_ID`, `ALPHABET`, or `QUANTITY` |
| Scroll Transfer | Enabled | Toggle scroll-based item transfer |
| Modifier Clicks | Enabled | Toggle Ctrl/Alt click behaviour |
| Shift Drag | Enabled | Toggle shift-drag |
| Stack Refill | Enabled | Toggle auto-refill |
| Quick Craft | Enabled | Toggle recipe book quick crafting |
| Click Delay | `0` | Ticks between simulated clicks (0–20) |

---

## Installation

1. Install [NeoForge](https://neoforged.net/) for Minecraft 1.21.
2. Download the latest release jar.
3. Place it in your `mods/` folder.
4. Launch the game.

Mouse Mixer is a **client-side only** mod and does not need to be installed on servers.

---

## Building from Source

```bash
git clone https://github.com/ManaZZZ/Mouse-Wheelie.git

cd Mouse-Wheelie
./gradlew build
```

The output jar is placed in `build/libs/`.

---

## License

This project is open source. See [LICENSE](LICENSE) for details.
