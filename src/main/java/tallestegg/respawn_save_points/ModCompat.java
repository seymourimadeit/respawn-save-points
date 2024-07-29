package tallestegg.respawn_save_points;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;

import java.util.UUID;

public class ModCompat {
    public static void doSBCompat(ItemStack savedStack) {
        if (ModList.get().isLoaded("sophisticatedbackpacks")) {
            if (savedStack.getItem() instanceof BackpackItem) {
                ItemStack originalStack = savedStack.getCapability(net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper.getCapabilityInstance()).orElseGet(null).cloneBackpack();
                savedStack.getCapability(net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper.getCapabilityInstance()).orElseGet(null).setContentsUuid(UUID.randomUUID());
                originalStack.getCapability(net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper.getCapabilityInstance()).orElseGet(null).copyDataTo(new net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper(savedStack));
                originalStack.setCount(0);
            }
        }
    }
}
