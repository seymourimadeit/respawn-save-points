package tallestegg.better_respawn_options.data_attachments;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.items.ItemStackHandler;

public class SavedPlayerInventory extends ItemStackHandler {
    public int experienceLevel;
    public int totalExperience;
    public float experienceProgress;
    public int playerScore;

    public SavedPlayerInventory(int size) {
        super(size);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag nbt = super.serializeNBT(provider);
        nbt.putInt("ExperienceLevel", this.getExperienceLevel());
        nbt.putInt("TotalExperience", this.getTotalExperience());
        nbt.putInt("PlayerScore", this.getPlayerScore());
        nbt.putFloat("ExperienceProgress", this.getExperienceProgress());
        return nbt;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        super.deserializeNBT(provider, nbt);
        this.setExperienceLevel(nbt.getInt("ExperienceLevel"));
        this.setTotalExperience(nbt.getInt("TotalExperience"));
        this.setExperienceProgress(nbt.getFloat("ExperienceProgress"));
        this.setPlayerScore(nbt.getInt("PlayerScore"));
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
}