package tallestegg.better_respawn_options.data_attachments;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import tallestegg.better_respawn_options.Config;

import java.util.UUID;

public class SavedPlayerInventory extends ItemStackHandler {
    public int experienceLevel;
    public int totalExperience;
    public float experienceProgress;
    public int playerScore;
    public UUID uuid;
    protected NonNullList<ItemStack> curiosItems;

    public SavedPlayerInventory(int size) {
        super(size);
        this.curiosItems = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag nbt = super.serializeNBT(provider);
        ListTag curiosItems = new ListTag();
        for(int i = 0; i < this.curiosItems.size(); ++i) {
            if (!this.curiosItems.get(i).isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                curiosItems.add(this.curiosItems.get(i).save(provider, itemTag));
            }
        }
        nbt.put("CuriosItems", curiosItems);
        nbt.putInt("CuriosSize", this.curiosItems.size());
        nbt.putInt("ExperienceLevel", this.getExperienceLevel());
        nbt.putInt("TotalExperience", this.getTotalExperience());
        nbt.putInt("PlayerScore", this.getPlayerScore());
        nbt.putFloat("ExperienceProgress", this.getExperienceProgress());
        if (this.getUuid() != null)
            nbt.putUUID("SavedUUID", this.getUuid());
        return nbt;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        super.deserializeNBT(provider, nbt);
        this.setExperienceLevel(nbt.getInt("ExperienceLevel"));
        this.setTotalExperience(nbt.getInt("TotalExperience"));
        this.setExperienceProgress(nbt.getFloat("ExperienceProgress"));
        this.setPlayerScore(nbt.getInt("PlayerScore"));
        if (nbt.hasUUID("SavedUUID"))
            this.setUuid(nbt.getUUID("SavedUUID"));
        this.curiosItems = NonNullList.withSize((nbt.contains("CuriosSize", Tag.TAG_INT) ? nbt.getInt("CuriosSize") : this.curiosItems.size()), ItemStack.EMPTY);
        ListTag tagList = nbt.getList("CuriosItems", Tag.TAG_COMPOUND);
        for(int i = 0; i < tagList.size(); ++i) {
            CompoundTag itemTags = tagList.getCompound(i);
            int slot = itemTags.getInt("Slot");
            if (slot >= 0 && slot < this.curiosItems.size()) {
                ItemStack.parse(provider, itemTags).ifPresent((stack) -> {
                    this.curiosItems.set(slot, stack);
                });
            }
        }
    }

    public void setCuriosStackInSlot(int slot, @NotNull ItemStack stack) {
        if (!Config.COMMON.itemBlacklist.get().contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()) && !EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP))
            this.curiosItems.set(slot, stack);
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        if (!Config.COMMON.itemBlacklist.get().contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()) && !EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP))
            super.setStackInSlot(slot, stack);
    }

    public ItemStack getCuriosStackInSlot(int slot) {
        return this.curiosItems.get(slot);
    }

    public void setCuriosItemsSize(int size) {
        this.curiosItems = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    public NonNullList<ItemStack> getCuriosItems() {
        return this.curiosItems;
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

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
}
