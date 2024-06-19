package tallestegg.respawn_save_points.capablities;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import tallestegg.respawn_save_points.Config;
import tallestegg.respawn_save_points.RespawnSavePoints;

import java.util.stream.Stream;

public class SavedPlayerInventory extends ItemStackHandler {
    public int experienceLevel;
    public int totalExperience;
    public float experienceProgress;
    public int playerScore;
    protected NonNullList<ItemStack> curiosItems;

    public SavedPlayerInventory(int size) {
        super(size);
        this.curiosItems = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = super.serializeNBT();
        ListTag curiosItems = new ListTag();
        for (int i = 0; i < this.curiosItems.size(); i++) {
            if (!this.curiosItems.get(i).isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                this.curiosItems.get(i).save(itemTag);
                curiosItems.add(itemTag);
            }
        }

        nbt.put("CuriosItems", curiosItems);
        nbt.putInt("CuriosSize", this.curiosItems.size());
        nbt.putInt("ExperienceLevel", this.getExperienceLevel());
        nbt.putInt("TotalExperience", this.getTotalExperience());
        nbt.putInt("PlayerScore", this.getPlayerScore());
        nbt.putFloat("ExperienceProgress", this.getExperienceProgress());
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.setExperienceLevel(nbt.getInt("ExperienceLevel"));
        this.setTotalExperience(nbt.getInt("TotalExperience"));
        this.setExperienceProgress(nbt.getFloat("ExperienceProgress"));
        this.setPlayerScore(nbt.getInt("PlayerScore"));
        this.curiosItems = NonNullList.withSize((nbt.contains("CuriosSize", Tag.TAG_INT) ? nbt.getInt("CuriosSize") : this.curiosItems.size()), ItemStack.EMPTY);
        ListTag tagList = nbt.getList("CuriosItems", Tag.TAG_COMPOUND);
        for (int i = 0; i < tagList.size(); i++) {
            CompoundTag itemTags = tagList.getCompound(i);
            int slot = itemTags.getInt("Slot");
            if (slot >= 0 && slot < this.curiosItems.size()) {
                this.curiosItems.set(slot, ItemStack.of(itemTags));
            }
        }
        super.deserializeNBT(nbt);
    }

    public void setCuriosStackInSlot(int slot, @NotNull ItemStack stack) {
        if (!Config.COMMON.itemBlacklist.get().contains(ForgeRegistries.ITEMS.getKey(stack.getItem()).toString()) && !EnchantmentHelper.hasVanishingCurse(stack))
            this.curiosItems.set(slot, stack);
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        if (!Config.COMMON.itemBlacklist.get().contains(ForgeRegistries.ITEMS.getKey(stack.getItem()).toString()) && !EnchantmentHelper.hasVanishingCurse(stack))
            super.setStackInSlot(slot, stack);
    }

    public ItemStack getCuriosStackInSlot(int slot) {
        return this.curiosItems.get(slot);
    }

    public void setCuriosItemsSize(int size) {
        this.curiosItems = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    public int getExperienceLevel() {
        return this.experienceLevel;
    }

    public void setExperienceLevel(int experience) {
        this.experienceLevel = experience;
    }

    public int getTotalExperience() {
        return totalExperience;
    }

    public void setTotalExperience(int totalExperience) {
        this.totalExperience = totalExperience;
    }

    public float getExperienceProgress() {
        return experienceProgress;
    }

    public void setExperienceProgress(float experienceProgress) {
        this.experienceProgress = experienceProgress;
    }

    public int getPlayerScore() {
        return playerScore;
    }

    public void setPlayerScore(int playerScore) {
        this.playerScore = playerScore;
    }

    public NonNullList<ItemStack> getCuriosItems() {
        return this.curiosItems;
    }
}

