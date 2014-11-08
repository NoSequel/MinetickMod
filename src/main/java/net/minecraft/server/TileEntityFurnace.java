package net.minecraft.server;

// CraftBukkit start
import java.util.List;

import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
// CraftBukkit end

public class TileEntityFurnace extends TileEntity implements IWorldInventory {

    private static final int[] d = new int[] { 0};
    private static final int[] e = new int[] { 2, 1};
    private static final int[] f = new int[] { 1};
    private ItemStack[] items = new ItemStack[3];
    public int burnTime;
    public int ticksForCurrentFuel;
    public int cookTime;
    private String h;

    // CraftBukkit start
    private int lastTick = MinecraftServer.currentTick;
    private int maxStack = MAX_STACK;
    public List<HumanEntity> transaction = new java.util.ArrayList<HumanEntity>();

    public ItemStack[] getContents() {
        return this.items;
    }

    public void onOpen(CraftHumanEntity who) {
        transaction.add(who);
    }

    public void onClose(CraftHumanEntity who) {
        transaction.remove(who);
    }

    public List<HumanEntity> getViewers() {
        return transaction;
    }

    public void setMaxStackSize(int size) {
        maxStack = size;
    }
    // CraftBukkit end

    public TileEntityFurnace() {}

    public int getSize() {
        return this.items.length;
    }

    public ItemStack getItem(int i) {
        return this.items[i];
    }

    public ItemStack splitStack(int i, int j) {
        if (this.items[i] != null) {
            ItemStack itemstack;

            if (this.items[i].count <= j) {
                itemstack = this.items[i];
                this.items[i] = null;
                return itemstack;
            } else {
                itemstack = this.items[i].a(j);
                if (this.items[i].count == 0) {
                    this.items[i] = null;
                }

                return itemstack;
            }
        } else {
            return null;
        }
    }

    public ItemStack splitWithoutUpdate(int i) {
        if (this.items[i] != null) {
            ItemStack itemstack = this.items[i];

            this.items[i] = null;
            return itemstack;
        } else {
            return null;
        }
    }

    public void setItem(int i, ItemStack itemstack) {
        this.items[i] = itemstack;
        if (itemstack != null && itemstack.count > this.getMaxStackSize()) {
            itemstack.count = this.getMaxStackSize();
        }
    }

    public String getName() {
        return this.c() ? this.h : "container.furnace";
    }

    public boolean c() {
        return this.h != null && this.h.length() > 0;
    }

    public void a(String s) {
        this.h = s;
    }

    public void a(NBTTagCompound nbttagcompound) {
        super.a(nbttagcompound);
        NBTTagList nbttaglist = nbttagcompound.getList("Items");

        this.items = new ItemStack[this.getSize()];

        for (int i = 0; i < nbttaglist.size(); ++i) {
            NBTTagCompound nbttagcompound1 = (NBTTagCompound) nbttaglist.get(i);
            byte b0 = nbttagcompound1.getByte("Slot");

            if (b0 >= 0 && b0 < this.items.length) {
                this.items[b0] = ItemStack.createStack(nbttagcompound1);
            }
        }

        this.burnTime = nbttagcompound.getShort("BurnTime");
        this.cookTime = nbttagcompound.getShort("CookTime");
        this.ticksForCurrentFuel = fuelTime(this.items[1]);
        if (nbttagcompound.hasKey("CustomName")) {
            this.h = nbttagcompound.getString("CustomName");
        }
    }

    public void b(NBTTagCompound nbttagcompound) {
        super.b(nbttagcompound);
        nbttagcompound.setShort("BurnTime", (short) this.burnTime);
        nbttagcompound.setShort("CookTime", (short) this.cookTime);
        NBTTagList nbttaglist = new NBTTagList();

        for (int i = 0; i < this.items.length; ++i) {
            if (this.items[i] != null) {
                NBTTagCompound nbttagcompound1 = new NBTTagCompound();

                nbttagcompound1.setByte("Slot", (byte) i);
                this.items[i].save(nbttagcompound1);
                nbttaglist.add(nbttagcompound1);
            }
        }

        nbttagcompound.set("Items", nbttaglist);
        if (this.c()) {
            nbttagcompound.setString("CustomName", this.h);
        }
    }

    public int getMaxStackSize() {
        return maxStack; // CraftBukkit
    }

    public boolean isBurning() {
        return this.burnTime > 0;
    }

    public void h() {
        boolean flag = this.burnTime > 0;
        boolean flag1 = false;

        // CraftBukkit start - Use wall time instead of ticks for cooking
        int elapsedTicks = MinecraftServer.currentTick - this.lastTick;
        this.lastTick = MinecraftServer.currentTick;

        // CraftBukkit - moved from below
        if (this.isBurning() && this.canBurn()) {
            this.cookTime += elapsedTicks;
            if (this.cookTime >= 200) {
                this.cookTime %= 200;
                this.burn();
                flag1 = true;
            }
        } else {
            this.cookTime = 0;
        }
        // CraftBukkit end

        if (this.burnTime > 0) {
            this.burnTime -= elapsedTicks; // CraftBukkit
        }

        if (!this.world.isStatic) {
            // CraftBukkit start - Handle multiple elapsed ticks
            if (this.burnTime <= 0 && this.canBurn() && this.items[1] != null) { // CraftBukkit - == to <=
                CraftItemStack fuel = CraftItemStack.asCraftMirror(this.items[1]);

                FurnaceBurnEvent furnaceBurnEvent = new FurnaceBurnEvent(this.world.getWorld().getBlockAt(this.x, this.y, this.z), fuel, fuelTime(this.items[1]));
                this.world.getServer().getPluginManager().callEvent(furnaceBurnEvent);

                if (furnaceBurnEvent.isCancelled()) {
                    return;
                }

                this.ticksForCurrentFuel = furnaceBurnEvent.getBurnTime();
                this.burnTime += this.ticksForCurrentFuel;
                if (this.burnTime > 0 && furnaceBurnEvent.isBurning()) {
                    // CraftBukkit end
                    flag1 = true;
                    if (this.items[1] != null) {
                        --this.items[1].count;
                        if (this.items[1].count == 0) {
                            Item item = this.items[1].getItem().t();

                            this.items[1] = item != null ? new ItemStack(item) : null;
                        }
                    }
                }
            }

            /* CraftBukkit start - Moved up
            if (this.isBurning() && this.canBurn()) {
                ++this.cookTime;
                if (this.cookTime == 200) {
                    this.cookTime = 0;
                    this.burn();
                    flag1 = true;
                }
            } else {
                this.cookTime = 0;
            }
            // CraftBukkit end */

            if (flag != this.burnTime > 0) {
                flag1 = true;
                BlockFurnace.a(this.burnTime > 0, this.world, this.x, this.y, this.z);
            }
        }

        if (flag1) {
            this.update();
        }
    }

    private boolean canBurn() {
        if (this.items[0] == null) {
            return false;
        } else {
            ItemStack itemstack = RecipesFurnace.getInstance().getResult(this.items[0].getItem().id);
            if(itemstack == null || !this.world.isLoaded(this.x, this.y, this.z) || this.world.chunkProviderServer.unloadQueue.contains(this.x >> 4, this.z >> 4)) { return false; } // Poweruser

            // CraftBukkit - consider resultant count instead of current count
            return itemstack == null ? false : (this.items[2] == null ? true : (!this.items[2].doMaterialsMatch(itemstack) ? false : (this.items[2].count + itemstack.count <= this.getMaxStackSize() && this.items[2].count < this.items[2].getMaxStackSize() ? true : this.items[2].count + itemstack.count <= itemstack.getMaxStackSize())));
        }
    }

    public void burn() {
        if (this.canBurn()) {
            ItemStack itemstack = RecipesFurnace.getInstance().getResult(this.items[0].getItem().id);

            // CraftBukkit start
            CraftItemStack source = CraftItemStack.asCraftMirror(this.items[0]);
            CraftItemStack result = CraftItemStack.asCraftMirror(itemstack.cloneItemStack());

            FurnaceSmeltEvent furnaceSmeltEvent = new FurnaceSmeltEvent(this.world.getWorld().getBlockAt(this.x, this.y, this.z), source, result);
            this.world.getServer().getPluginManager().callEvent(furnaceSmeltEvent);

            if (furnaceSmeltEvent.isCancelled()) {
                return;
            }

            itemstack = CraftItemStack.asNMSCopy(furnaceSmeltEvent.getResult());

            if (this.items[2] == null) {
                this.items[2] = itemstack.cloneItemStack();
            } else if (this.items[2].id == itemstack.id) {
                // CraftBukkit - compare damage too
                if (this.items[2].getData() == itemstack.getData()) {
                    this.items[2].count += itemstack.count;
                }
                // CraftBukkit end
            }

            --this.items[0].count;
            if (this.items[0].count <= 0) {
                this.items[0] = null;
            }
        }
    }

    public static int fuelTime(ItemStack itemstack) {
        if (itemstack == null) {
            return 0;
        } else {
            int i = itemstack.getItem().id;
            Item item = itemstack.getItem();

            if (i < 256 && Block.byId[i] != null) {
                Block block = Block.byId[i];

                if (block == Block.WOOD_STEP) {
                    return 150;
                }

                if (block.material == Material.WOOD) {
                    return 300;
                }

                if (block == Block.COAL_BLOCK) {
                    return 16000;
                }
            }

            return item instanceof ItemTool && ((ItemTool) item).g().equals("WOOD") ? 200 : (item instanceof ItemSword && ((ItemSword) item).i().equals("WOOD") ? 200 : (item instanceof ItemHoe && ((ItemHoe) item).g().equals("WOOD") ? 200 : (i == Item.STICK.id ? 100 : (i == Item.COAL.id ? 1600 : (i == Item.LAVA_BUCKET.id ? 20000 : (i == Block.SAPLING.id ? 100 : (i == Item.BLAZE_ROD.id ? 2400 : 0)))))));
        }
    }

    public static boolean isFuel(ItemStack itemstack) {
        return fuelTime(itemstack) > 0;
    }

    public boolean a(EntityHuman entityhuman) {
        return this.world.getTileEntity(this.x, this.y, this.z) != this ? false : entityhuman.e((double) this.x + 0.5D, (double) this.y + 0.5D, (double) this.z + 0.5D) <= 64.0D;
    }

    public void startOpen() {}

    public void g() {}

    public boolean b(int i, ItemStack itemstack) {
        return i == 2 ? false : (i == 1 ? isFuel(itemstack) : true);
    }

    public int[] getSlotsForFace(int i) {
        return i == 0 ? e : (i == 1 ? d : f);
    }

    public boolean canPlaceItemThroughFace(int i, ItemStack itemstack, int j) {
        return this.b(i, itemstack);
    }

    public boolean canTakeItemThroughFace(int i, ItemStack itemstack, int j) {
        return j != 0 || i != 1 || itemstack.id == Item.BUCKET.id;
    }
}
