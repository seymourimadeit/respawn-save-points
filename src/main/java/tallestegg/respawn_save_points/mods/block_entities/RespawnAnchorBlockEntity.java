package tallestegg.respawn_save_points.mods.block_entities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class RespawnAnchorBlockEntity extends BlockEntity {

    public RespawnAnchorBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(RSPBlockEntities.RESPAWN_ANCHOR_BLOCK_ENTITY.get(), pPos, pBlockState);
    }

    @Override
    public BlockEntityType<?> getType() {
        return RSPBlockEntities.RESPAWN_ANCHOR_BLOCK_ENTITY.get();
    }
}
