package tallestegg.better_respawn_options.data_attachments;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import tallestegg.better_respawn_options.BetterRespawnOptions;

import java.util.function.Supplier;

public class BROData {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, BetterRespawnOptions.MODID);

    public static final Supplier<AttachmentType<SavedPlayerInventory>> SAVED_INVENTORY = ATTACHMENT_TYPES.register(
            "saved_inventory", () -> AttachmentType.serializable(() -> new SavedPlayerInventory(41)).build()
    );
}
