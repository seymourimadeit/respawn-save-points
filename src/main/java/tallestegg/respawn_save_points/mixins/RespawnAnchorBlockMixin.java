package tallestegg.respawn_save_points.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import tallestegg.respawn_save_points.block_entities.RespawnAnchorBlockEntity;

@Mixin(RespawnAnchorBlock.class)
public abstract class RespawnAnchorBlockMixin extends Block implements EntityBlock {
    public RespawnAnchorBlockMixin(Properties p_49795_) {
        super(p_49795_);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new RespawnAnchorBlockEntity(pPos, pState);
    }
}
