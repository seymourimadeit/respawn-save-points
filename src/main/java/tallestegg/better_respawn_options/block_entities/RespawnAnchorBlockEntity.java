package tallestegg.better_respawn_options.block_entities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class RespawnAnchorBlockEntity extends BlockEntity {

    public RespawnAnchorBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(BROBlockEntities.RESPAWN_ANCHOR_BLOCK_ENTITY.get(), pPos, pBlockState);
    }

    @Override
    public BlockEntityType<?> getType() {
        return BROBlockEntities.RESPAWN_ANCHOR_BLOCK_ENTITY.get();
    }
}
