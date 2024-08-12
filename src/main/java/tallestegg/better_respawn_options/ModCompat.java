package tallestegg.better_respawn_options;

import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;

import java.util.UUID;

public class ModCompat {
    public static void doSBCompat(ItemStack savedStack) {
        if (ModList.get().isLoaded("sophisticatedbackpacks")) {
            if (savedStack.getItem() instanceof BackpackItem) {
                ItemStack originalStack = BackpackWrapper.fromStack(savedStack).cloneBackpack();
                BackpackWrapper.fromStack(savedStack).setContentsUuid(UUID.randomUUID());
                BackpackWrapper.fromStack(originalStack).copyDataTo(new net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper(savedStack));
                originalStack.setCount(0);
            }
        }
    }
}
