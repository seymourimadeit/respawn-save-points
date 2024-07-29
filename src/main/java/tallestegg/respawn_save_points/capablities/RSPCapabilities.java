package tallestegg.respawn_save_points.capablities;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandlerModifiable;
import tallestegg.respawn_save_points.RespawnSavePoints;

@Mod.EventBusSubscriber(modid = RespawnSavePoints.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class RSPCapabilities {
    public static final Capability<SavedPlayerInventory> SAVED_INVENTORY = CapabilityManager.get(new CapabilityToken<>() {
    });

    public static SavedPlayerInventory getSavedInventory(BlockEntity entity) {
        if (entity == null)
            return null;
        LazyOptional<SavedPlayerInventory> listener = entity.getCapability(SAVED_INVENTORY);
        if (listener.isPresent())
            return listener.orElseThrow(() -> new IllegalStateException("Capability not found! Report this to the Respawn Save Points github!"));
        return null;
    }

    public static IItemHandlerModifiable getItemModifiableCap(ItemStack stack) {
        if (stack == null)
            return null;
        LazyOptional<IItemHandlerModifiable> listener = stack.getCapability(ForgeCapabilities.ITEM_HANDLER).cast();
        if (listener.isPresent())
            return listener.orElseThrow(() -> new IllegalStateException("Capability not found! Report this to the Respawn Save Points github!"));
        return null;
    }
}
