package tallestegg.respawn_save_points.block_entities;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import tallestegg.respawn_save_points.RespawnSavePoints;

public class RSPBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, RespawnSavePoints.MODID);

    public static final RegistryObject<BlockEntityType<RespawnAnchorBlockEntity>> RESPAWN_ANCHOR_BLOCK_ENTITY = BLOCK_ENTITIES.register("respawn_anchor_block_entity", () -> BlockEntityType.Builder.of(RespawnAnchorBlockEntity::new, Blocks.RESPAWN_ANCHOR).build(null));

}
