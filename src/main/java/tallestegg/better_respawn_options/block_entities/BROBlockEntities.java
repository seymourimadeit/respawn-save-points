package tallestegg.better_respawn_options.block_entities;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import tallestegg.better_respawn_options.BetterRespawnOptions;

public class BROBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BetterRespawnOptions.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RespawnAnchorBlockEntity>> RESPAWN_ANCHOR_BLOCK_ENTITY = BLOCK_ENTITIES.register("respawn_anchor_block_entity", () -> BlockEntityType.Builder.of(RespawnAnchorBlockEntity::new, Blocks.RESPAWN_ANCHOR).build(null));
}
