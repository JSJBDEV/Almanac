package ace.actually.almanac;

import ace.actually.almanac.items.AlmanacItem;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

/**
 * Literally BookCloningRecipe.
 */
public class AlmanacCloningRecipe extends SpecialCraftingRecipe {
    public AlmanacCloningRecipe(Identifier id, CraftingRecipeCategory category) {
        super(id, category);
    }

    public boolean matches(RecipeInputInventory recipeInputInventory, World world) {
        int i = 0;
        ItemStack itemStack = ItemStack.EMPTY;

        for(int j = 0; j < recipeInputInventory.size(); ++j) {
            ItemStack itemStack2 = recipeInputInventory.getStack(j);
            if (!itemStack2.isEmpty()) {
                if (itemStack2.isOf(Almanac.ALMANAC_ITEM)) {
                    if (!itemStack.isEmpty()) {
                        return false;
                    }

                    itemStack = itemStack2;
                } else {
                    if (!itemStack2.isOf(Items.WRITABLE_BOOK)) {
                        return false;
                    }

                    ++i;
                }
            }
        }

        return !itemStack.isEmpty() && itemStack.hasNbt() && i > 0;
    }

    public ItemStack craft(RecipeInputInventory recipeInputInventory, DynamicRegistryManager dynamicRegistryManager) {
        int i = 0;
        ItemStack itemStack = ItemStack.EMPTY;

        for(int j = 0; j < recipeInputInventory.size(); ++j) {
            ItemStack itemStack2 = recipeInputInventory.getStack(j);
            if (!itemStack2.isEmpty()) {
                if (itemStack2.isOf(Almanac.ALMANAC_ITEM)) {
                    if (!itemStack.isEmpty()) {
                        return ItemStack.EMPTY;
                    }

                    itemStack = itemStack2;
                } else {
                    if (!itemStack2.isOf(Items.WRITABLE_BOOK)) {
                        return ItemStack.EMPTY;
                    }

                    ++i;
                }
            }
        }

        if (!itemStack.isEmpty() && itemStack.hasNbt() && i >= 1) {
            ItemStack itemStack3 = new ItemStack(Almanac.ALMANAC_ITEM, i);
            NbtCompound nbtCompound = itemStack.getNbt().copy();
            //nbtCompound.putInt("generation", WrittenBookItem.getGeneration(itemStack) + 1);
            itemStack3.setNbt(nbtCompound);
            return itemStack3;
        } else {
            return ItemStack.EMPTY;
        }
    }

    public DefaultedList<ItemStack> getRemainder(RecipeInputInventory recipeInputInventory) {
        DefaultedList<ItemStack> defaultedList = DefaultedList.ofSize(recipeInputInventory.size(), ItemStack.EMPTY);

        for(int i = 0; i < defaultedList.size(); ++i) {
            ItemStack itemStack = recipeInputInventory.getStack(i);
            if (itemStack.getItem().hasRecipeRemainder()) {
                defaultedList.set(i, new ItemStack(itemStack.getItem().getRecipeRemainder()));
            } else if (itemStack.getItem() instanceof AlmanacItem) {
                defaultedList.set(i, itemStack.copyWithCount(1));
                break;
            }
        }

        return defaultedList;
    }

    public RecipeSerializer<?> getSerializer() {
        return Almanac.ALMANAC_CLONING;
    }

    public boolean fits(int width, int height) {
        return width >= 3 && height >= 3;
    }
}
