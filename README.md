# Automation

## Info

Automation is a Bukkit plugin which bundles together a bunch of automation related functionality. Before being released, it probably needs to be split into separate plugins.

## Basic Mechanics

### Expanded Dispenser Functionality

Dispensers can place and break blocks, plant seeds, dye and shear sheep, milk cows, place items in itemframes, and quite a bit more. When a given functionality makes use of some tool, the enchantments, durability, and material of that tool are respected.

The plugin also exposes an interface for extending and overwriting dispenser behavior. Many of the other modules make use of this functionality.

### Autocrafting

Vertical dispensers facing into an adjacent halfslab act as an autocrafter. When activated, autocrafters will craft the contents and eject the result along with any leftovers. Items added to an autocrafter via hopper will not stack, and will fill the grid from top to bottom, left to right. If blank spaces are needed for a recipe, filler and blocker items can be used.

### BlockCarts

Blocks can be placed in carts, and in some cases interacted with. For example, a cauldron minecart can be filled and emptied with bottles or buckets. A shulker box minecart can be accessed like a chest minecart and the shulker box will keep its contents when removed from the minecart. Cake can be eaten, trap doors can be toggled, ect. When combined with the dispenser module, blocks can be placed in and removed from minecarts automatically.

### Hopper Filters

Hopper inputs can be whitelist filtered with item frames. If the item is upside-down, the filter will work as a blacklist instead.

### Pulsers

A repeater pointing out of a hopper, brewing stand, or furnace will pulse when the hopper pushes an item, the brewing stand finishes brewing, or the furnace finishes smelting.

It's a nice feature to have, but it's a little hacky to attach that feature to a repeater.

### Enderpearls

Enderpearls bind to the last player to touch them, and teleport that player when broken by a dispenser.
