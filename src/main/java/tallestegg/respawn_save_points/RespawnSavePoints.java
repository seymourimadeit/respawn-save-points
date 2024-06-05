package tallestegg.respawn_save_points;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tallestegg.respawn_save_points.block_entities.RespawnAnchorBlockEntity;
import tallestegg.respawn_save_points.capablities.RSPCapabilities;
import tallestegg.respawn_save_points.capablities.SavedPlayerInventory;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.Optional;
import java.util.stream.IntStream;

@Mod.EventBusSubscriber(modid = RespawnSavePoints.MODID)
@Mod(RespawnSavePoints.MODID)
public class RespawnSavePoints {
    public static final String MODID = "respawn_save_points";

    public RespawnSavePoints() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::addCreative);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (player instanceof ServerPlayer serverPlayer && !event.isEndConquered()) {
            if (serverPlayer.getRespawnPosition() != null && (level.getBlockEntity(serverPlayer.getRespawnPosition()) instanceof BedBlockEntity || level.getBlockEntity(serverPlayer.getRespawnPosition()) instanceof RespawnAnchorBlockEntity)) {
                SavedPlayerInventory savedPlayerInventory = getSavedInventory(level.getBlockEntity(serverPlayer.getRespawnPosition()));
                if (savedPlayerInventory == null)
                    return;
                Inventory inventory = serverPlayer.getInventory();
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    inventory.setItem(i, savedPlayerInventory.getStackInSlot(i).copy());
                }
                if (ModList.get().isLoaded("curios")) {
                    Optional<ICuriosItemHandler> curiosApi = CuriosApi.getCuriosInventory(player).resolve();
                    if (curiosApi.isPresent()) {
                        for (int i = 0; i < curiosApi.get().getSlots(); i++) {
                            curiosApi.get().getEquippedCurios().setStackInSlot(i, savedPlayerInventory.getCuriosStackInSlot(i));
                        }
                    }
                }
                if (Config.COMMON.saveXP.get()) {
                    serverPlayer.setExperienceLevels(savedPlayerInventory.getExperienceLevel());
                    serverPlayer.experienceProgress = savedPlayerInventory.getExperienceProgress();
                    serverPlayer.totalExperience = savedPlayerInventory.getTotalExperience();
                    serverPlayer.setScore(savedPlayerInventory.getPlayerScore());
                }
            }
        }
    }

    @SubscribeEvent
    public void onDead(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            BlockEntity respawnPoint = serverPlayer.level().getBlockEntity(serverPlayer.getRespawnPosition());
            SavedPlayerInventory savedPlayerInventory = getSavedInventory(respawnPoint);
            if (savedPlayerInventory == null)
                return;
            for (int i = 0; i < serverPlayer.getInventory().getContainerSize(); i++) {
                if (!savedPlayerInventory.getStackInSlot(i).isEmpty() && serverPlayer.getInventory().getItem(i).isEmpty())
                    savedPlayerInventory.setStackInSlot(i, ItemStack.EMPTY);
                respawnPoint.setChanged();
            }
        }
    }

    @SubscribeEvent
    public void onDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            Level level = serverPlayer.level();
            if (serverPlayer.getRespawnPosition() != null && (level.getBlockEntity(serverPlayer.getRespawnPosition()) instanceof BedBlockEntity || level.getBlockEntity(serverPlayer.getRespawnPosition()) instanceof RespawnAnchorBlockEntity)) {
                SavedPlayerInventory savedPlayerInventory = getSavedInventory(level.getBlockEntity(serverPlayer.getRespawnPosition()));
                if (savedPlayerInventory == null)
                    return;
                Inventory inventory = serverPlayer.getInventory();
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    ItemStack stack = savedPlayerInventory.getStackInSlot(i);
                    event.getDrops().stream().findAny().filter(itemEntity -> ItemStack.isSameItem(itemEntity.getItem(), stack) && itemEntity.getItem().getCount() < stack.getCount()).ifPresent(itemEntity -> stack.setCount(itemEntity.getItem().getCount()));
                    event.getDrops().stream().findAny().filter(itemEntity -> ItemStack.isSameItem(itemEntity.getItem(), stack) && itemEntity.getItem().getCount() > stack.getCount()).ifPresent(itemEntity -> itemEntity.getItem().setCount(itemEntity.getItem().getCount() - stack.getCount()));
                    event.getDrops().stream().findAny().filter(itemEntity -> ItemStack.isSameItem(itemEntity.getItem(), stack) && itemEntity.getItem().getDamageValue() > stack.getDamageValue()).ifPresent(itemEntity -> stack.setDamageValue(itemEntity.getItem().getDamageValue()));
                    event.getDrops().removeIf(itemEntity -> ItemStack.isSameItem(itemEntity.getItem(), stack) && itemEntity.getItem().getDamageValue() > stack.getDamageValue());
                    event.getDrops().removeIf(itemEntity -> ItemStack.matches(itemEntity.getItem(), stack));
                    level.getBlockEntity(serverPlayer.getRespawnPosition()).setChanged();
                }
            }
        }
    }

    @SubscribeEvent
    public void onDropXP(LivingExperienceDropEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer && Config.COMMON.saveXP.get()) {
            SavedPlayerInventory savedPlayerInventory = getSavedInventory(serverPlayer.level().getBlockEntity(serverPlayer.getRespawnPosition()));
            if (savedPlayerInventory == null)
                return;
            event.setDroppedExperience(serverPlayer.totalExperience - savedPlayerInventory.getTotalExperience());
        }
    }

    @SubscribeEvent
    public void onPlayerBed(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = player.level();
        BlockPos blockPos = event.getPos();
        BlockState state = level.getBlockState(blockPos);
        if (player instanceof ServerPlayer serverPlayer) {
            if (state.getBlock() instanceof BedBlock) {
                if (state.getValue(BedBlock.PART) != BedPart.HEAD)
                    blockPos = blockPos.relative(state.getValue(BedBlock.FACING));
            }
            if (level.getBlockEntity(blockPos) instanceof BedBlockEntity || level.getBlockEntity(blockPos) instanceof RespawnAnchorBlockEntity) {
                SavedPlayerInventory savedPlayerInventory = getSavedInventory(level.getBlockEntity(blockPos));
                if (savedPlayerInventory == null)
                    return;
                if (ModList.get().isLoaded("curios")) {
                    Optional<ICuriosItemHandler> curiosApi = CuriosApi.getCuriosInventory(player).resolve();
                    if (curiosApi.isPresent()) {
                        savedPlayerInventory.setCuriosItemsSize(curiosApi.get().getSlots());
                        for (int i = 0; i < curiosApi.get().getSlots(); i++) {
                            savedPlayerInventory.setCuriosStackInSlot(i, curiosApi.get().getEquippedCurios().getStackInSlot(i));
                        }
                    }
                }
                Inventory inventory = serverPlayer.getInventory();
                savedPlayerInventory.setSize(inventory.getContainerSize());
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    savedPlayerInventory.setStackInSlot(i, inventory.getItem(i).copy());
                    savedPlayerInventory.getStackInSlot(i).setCount(inventory.getItem(i).copy().getCount());
                    if (Config.COMMON.saveXP.get()) {
                        savedPlayerInventory.setExperienceLevel(serverPlayer.experienceLevel);
                        savedPlayerInventory.setTotalExperience(serverPlayer.totalExperience);
                        savedPlayerInventory.setExperienceProgress(serverPlayer.experienceProgress);
                    }
                    level.getBlockEntity(blockPos).setChanged();
                }
                serverPlayer.sendSystemMessage(Component.translatable("Inventory saved"));
                level.getBlockEntity(blockPos).setChanged();
            }
        }
    }

    @SubscribeEvent
    public void registerCaps(RegisterCapabilitiesEvent event) {
        event.register(SavedPlayerInventory.class);
    }

    @SubscribeEvent
    public void onAttachingCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
        if (event.getObject() instanceof BedBlockEntity || event.getObject() instanceof RespawnAnchorBlockEntity) {
            SavedPlayerInventory backend = new SavedPlayerInventory(49);
            LazyOptional<IItemHandlerModifiable> optionalStorage = LazyOptional.of(() -> backend);
            ICapabilityProvider provider = new ICapabilitySerializable() {
                @Override
                public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                    return RSPCapabilities.SAVED_INVENTORY.orEmpty(cap, optionalStorage.cast());
                }

                @Override
                public Tag serializeNBT() {
                    return backend.serializeNBT();
                }

                @Override
                public void deserializeNBT(Tag nbt) {
                    backend.serializeNBT();
                }
            };
            event.addCapability(new ResourceLocation(MODID, "saved_player_inventory"), provider);
        }
    }


    public static SavedPlayerInventory getSavedInventory(BlockEntity blockEntity) {
        return RSPCapabilities.getSavedInventory(blockEntity);
    }
}
