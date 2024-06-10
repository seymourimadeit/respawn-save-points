package tallestegg.respawn_save_points;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
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
import net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.violetmoon.quark.base.handler.ProxiedItemStackHandler;
import tallestegg.respawn_save_points.block_entities.RespawnAnchorBlockEntity;
import tallestegg.respawn_save_points.capablities.RSPCapabilities;
import tallestegg.respawn_save_points.capablities.SavedPlayerInventory;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.Optional;
import java.util.stream.Stream;

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
                if (savedPlayerInventory == null) return;
                Inventory inventory = serverPlayer.getInventory();
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    inventory.setItem(i, savedPlayerInventory.getStackInSlot(i).copy());
                }
                if (ModList.get().isLoaded("curios")) {
                    Optional<ICuriosItemHandler> curiosApi = CuriosApi.getCuriosInventory(player).resolve();
                    if (curiosApi.isPresent()) {
                        for (int i = 0; i < curiosApi.get().getSlots(); i++) {
                            curiosApi.get().getEquippedCurios().setStackInSlot(i, savedPlayerInventory.getCuriosStackInSlot(i).copy());
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
            if (savedPlayerInventory == null) return;
            for (int i = 0; i < serverPlayer.getInventory().getContainerSize(); i++) {
                ItemStack savedStack = savedPlayerInventory.getStackInSlot(i);
                ItemStack playerStack = serverPlayer.getInventory().getItem(i);
                if (!savedStack.isEmpty() && playerStack.isEmpty())
                    savedPlayerInventory.setStackInSlot(i, ItemStack.EMPTY);
                if (savedStack.getItem() instanceof BundleItem && playerStack.getItem() instanceof BundleItem) {
                    for (int bundleSlot = 0; bundleSlot < 64; bundleSlot++) {
                        ItemStack bundleItem = RespawnSavePoints.getItemsFromNBT(bundleSlot, playerStack);
                        ItemStack savedBundleItem = RespawnSavePoints.getItemsFromNBT(bundleSlot, savedStack);
                        if (!savedBundleItem.isEmpty() && bundleItem.isEmpty())
                            RespawnSavePoints.setBundleItem(bundleSlot, savedStack, new ItemStack(Items.AIR));
                        if (ItemStack.isSameItem(bundleItem, savedBundleItem)) {
                            if (bundleItem.getCount() > savedBundleItem.getCount()) {
                                bundleItem.setCount(bundleItem.getCount() - savedBundleItem.getCount());
                                serverPlayer.drop(bundleItem, true);
                            }
                            if (bundleItem.getCount() < savedBundleItem.getCount())
                                savedBundleItem.setCount(bundleItem.getCount());
                            if (bundleItem.getDamageValue() > savedBundleItem.getDamageValue())
                                savedBundleItem.setDamageValue(bundleItem.getDamageValue());
                        } else {
                            serverPlayer.drop(bundleItem, true);
                        }
                        playerStack.setCount(0);
                    }
                }
                if (ModList.get().isLoaded("sophisticatedbackpacks")) {
                    if (savedStack.getItem() instanceof BackpackItem && playerStack.getItem() instanceof BackpackItem) {
                        InventoryHandler playerBackpackHandler = playerStack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).orElseGet(null).getInventoryHandler();
                        InventoryHandler savedBackpackHandler = savedStack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).orElseGet(null).getInventoryHandler();
                        cycleInventoryOnDeathCaps(savedBackpackHandler, playerBackpackHandler, playerBackpackHandler.getSlots(), serverPlayer);
                    }
                }
                if (ModList.get().isLoaded("quark")) {
                    if (savedStack.getItem() instanceof org.violetmoon.quark.addons.oddities.item.BackpackItem && playerStack.getItem() instanceof org.violetmoon.quark.addons.oddities.item.BackpackItem) {
                        ProxiedItemStackHandler playerBackpackHandler = (ProxiedItemStackHandler) playerStack.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseGet(null);
                        ProxiedItemStackHandler savedBackpackHandler = (ProxiedItemStackHandler) savedStack.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseGet(null);
                        cycleInventoryOnDeathCaps(savedBackpackHandler, playerBackpackHandler, playerBackpackHandler.getSlots(), serverPlayer);
                        playerStack.setCount(0);
                    }
                }
                if (savedStack.getItem() instanceof BlockItem savedBlockItem && playerStack.getItem() instanceof BlockItem playerBlockItem) {
                    if (savedBlockItem.getBlock() instanceof ShulkerBoxBlock && playerBlockItem.getBlock() instanceof ShulkerBoxBlock) {
                        NonNullList<ItemStack> shulkerItemList = NonNullList.withSize(27, ItemStack.EMPTY);
                        NonNullList<ItemStack> savedShulkerItemList = NonNullList.withSize(27, ItemStack.EMPTY);
                        ContainerHelper.loadAllItems(BlockItem.getBlockEntityData(playerStack), shulkerItemList);
                        ContainerHelper.loadAllItems(BlockItem.getBlockEntityData(savedStack), savedShulkerItemList);
                        for (int shulkerSlot = 0; shulkerSlot < shulkerItemList.size(); shulkerSlot++) {
                            ItemStack shulkerItem = shulkerItemList.get(shulkerSlot);
                            ItemStack savedShulkerItem = savedShulkerItemList.get(shulkerSlot);
                            if (shulkerItem.isEmpty() && !savedShulkerItem.isEmpty())
                                savedShulkerItemList.set(shulkerSlot, shulkerItem);
                            if (ItemStack.isSameItem(shulkerItem, savedShulkerItem)) {
                                if (shulkerItem.getCount() > savedShulkerItem.getCount()) {
                                    shulkerItem.setCount(shulkerItem.getCount() - savedShulkerItem.getCount());
                                    serverPlayer.drop(shulkerItem, true);
                                }
                                if (shulkerItem.getCount() < savedShulkerItem.getCount())
                                    savedShulkerItem.setCount(shulkerItem.getCount());
                                if (shulkerItem.getDamageValue() > savedShulkerItem.getDamageValue())
                                    savedShulkerItem.setDamageValue(shulkerItem.getDamageValue());
                            } else {
                                serverPlayer.drop(shulkerItem, true);
                            }
                            ContainerHelper.saveAllItems(BlockItem.getBlockEntityData(savedStack), savedShulkerItemList);
                            ContainerHelper.loadAllItems(BlockItem.getBlockEntityData(playerStack), shulkerItemList);
                        }
                        playerStack.setCount(-1);
                    }
                    respawnPoint.setChanged();
                }
            }
            for (int i = 0; i < savedPlayerInventory.getCuriosItems().size(); i++) {
                Optional<ICuriosItemHandler> curiosApi = CuriosApi.getCuriosInventory(serverPlayer).resolve();
                ICuriosItemHandler playerCuriosHandler = curiosApi.get();
                ItemStack savedCuriosStack = savedPlayerInventory.getCuriosStackInSlot(i);
                ItemStack playerCuriosStack = playerCuriosHandler.getEquippedCurios().getStackInSlot(i);
                if (ModList.get().isLoaded("sophisticatedbackpacks")) {
                    if (savedCuriosStack.getItem() instanceof BackpackItem && playerCuriosStack.getItem() instanceof BackpackItem) {
                        InventoryHandler playerBackpackHandler = playerCuriosStack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).orElseGet(null).getInventoryHandler();
                        InventoryHandler savedBackpackHandler = savedCuriosStack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).orElseGet(null).getInventoryHandler();
                        cycleInventoryOnDeathCaps(savedBackpackHandler, playerBackpackHandler, playerBackpackHandler.getSlots(), serverPlayer);
                        playerCuriosStack.setCount(0);
                    }
                }
                if (ModList.get().isLoaded("quark")) {
                    if (savedCuriosStack.getItem() instanceof org.violetmoon.quark.addons.oddities.item.BackpackItem && playerCuriosStack.getItem() instanceof org.violetmoon.quark.addons.oddities.item.BackpackItem) {
                        ProxiedItemStackHandler playerBackpackHandler = (ProxiedItemStackHandler) playerCuriosStack.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseGet(null);
                        ProxiedItemStackHandler savedBackpackHandler = (ProxiedItemStackHandler) savedCuriosStack.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseGet(null);
                        cycleInventoryOnDeathCaps(savedBackpackHandler, playerBackpackHandler, playerBackpackHandler.getSlots(), serverPlayer);
                        playerCuriosStack.setCount(0);
                    }
                }
                if (ModList.get().isLoaded("backpacked")) {
                    if (playerCuriosStack.getItem() instanceof com.mrcrayfish.backpacked.item.BackpackItem backpackItem && savedCuriosStack.getItem() instanceof com.mrcrayfish.backpacked.item.BackpackItem) {
                        for (int backPackSlot = 0; backPackSlot < (backpackItem.getColumnCount() * backpackItem.getRowCount()); backPackSlot++) {
                            ItemStack savedBackpackItem = RespawnSavePoints.getItemsFromNBT(backPackSlot, savedCuriosStack);
                            ItemStack playerBackpackitem = RespawnSavePoints.getItemsFromNBT(backPackSlot, playerCuriosStack);
                            if (playerBackpackitem.isEmpty() && !savedBackpackItem.isEmpty())
                                RespawnSavePoints.setBackpackedBackpackItems(backPackSlot, savedCuriosStack, ItemStack.EMPTY);
                            if (ItemStack.isSameItem(playerBackpackitem, savedBackpackItem)) {
                                if (playerBackpackitem.getCount() > savedBackpackItem.getCount()) {
                                    playerBackpackitem.setCount(playerBackpackitem.getCount() - savedBackpackItem.getCount());
                                    serverPlayer.drop(playerBackpackitem, true);
                                }
                                if (playerBackpackitem.getCount() > savedBackpackItem.getCount())
                                    savedBackpackItem.setCount(playerBackpackitem.getCount());
                                if (playerBackpackitem.getDamageValue() > savedBackpackItem.getDamageValue())
                                    savedBackpackItem.setDamageValue(playerBackpackitem.getDamageValue());
                            } else {
                                serverPlayer.drop(playerBackpackitem, true);
                            }
                        }
                        playerCuriosStack.setCount(0);
                    }
                    respawnPoint.setChanged();
                }
            }
        }
    }

    @SubscribeEvent
    public void onDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            Level level = serverPlayer.level();
            if (serverPlayer.getRespawnPosition() != null && (level.getBlockEntity(serverPlayer.getRespawnPosition()) instanceof BedBlockEntity || level.getBlockEntity(serverPlayer.getRespawnPosition()) instanceof RespawnAnchorBlockEntity)) {
                SavedPlayerInventory savedPlayerInventory = getSavedInventory(level.getBlockEntity(serverPlayer.getRespawnPosition()));
                if (savedPlayerInventory == null) return;
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
            if (savedPlayerInventory == null) return;
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
                if (savedPlayerInventory == null) return;
                if (ModList.get().isLoaded("curios")) {
                    Optional<ICuriosItemHandler> curiosApi = CuriosApi.getCuriosInventory(player).resolve();
                    if (curiosApi.isPresent()) {
                        savedPlayerInventory.setCuriosItemsSize(curiosApi.get().getSlots());
                        for (int i = 0; i < curiosApi.get().getSlots(); i++) {
                            savedPlayerInventory.setCuriosStackInSlot(i, curiosApi.get().getEquippedCurios().getStackInSlot(i).copy());
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
                    backend.deserializeNBT((CompoundTag) nbt);
                }
            };
            event.addCapability(new ResourceLocation(MODID, "saved_player_inventory"), provider);
        }
    }

    public static SavedPlayerInventory getSavedInventory(BlockEntity blockEntity) {
        return RSPCapabilities.getSavedInventory(blockEntity);
    }

    private static Stream<ItemStack> getContents(ItemStack p_150783_) {
        CompoundTag compoundtag = p_150783_.getTag();
        if (compoundtag == null) {
            return Stream.empty();
        } else {
            ListTag listtag = compoundtag.getList("Items", 10);
            return listtag.stream().map(CompoundTag.class::cast).map(ItemStack::of);
        }
    }

    private static void setBundleItem(int slot, ItemStack bundleStack, ItemStack newStack) {
        CompoundTag compoundtag = bundleStack.getOrCreateTag();
        if (!compoundtag.contains("Items")) {
            compoundtag.put("Items", new ListTag());
        } else {
            ListTag listtag = compoundtag.getList("Items", 10);
            CompoundTag compoundtag2 = new CompoundTag();
            newStack.save(compoundtag2);
            listtag.set(slot, compoundtag2);
        }
    }

    public static ItemStack getItemsFromNBT(int slot, ItemStack item) {
        CompoundTag tag = item.getOrCreateTag();
        if (!tag.contains("Items")) {
            tag.put("Items", new ListTag());
        } else {
            ListTag listtag = tag.getList("Items", 10);
            return ItemStack.of(listtag.getCompound(slot));
        }
        return ItemStack.EMPTY;
    }

    public static void setBackpackedBackpackItems(int slot, ItemStack backpackStack, ItemStack stackToPutIn) {
        CompoundTag tag = backpackStack.getOrCreateTag();
        if (!tag.contains("Items")) {
            tag.put("Items", new ListTag());
        } else {
            CompoundTag compound = new CompoundTag();
            compound.putByte("Slot", (byte) slot);
            stackToPutIn.save(compound);
            ListTag listtag = tag.getList("Items", 10);
            listtag.add(compound);
        }
    }

    public static void cycleInventoryOnDeathCaps(IItemHandlerModifiable savedCap, IItemHandlerModifiable playerCap, int inventorySize, ServerPlayer player) {
        for (int backPackSlot = 0; backPackSlot < inventorySize; backPackSlot++) {
            ItemStack savedBackpackItem = savedCap.getStackInSlot(backPackSlot);
            ItemStack playerBackpackitem = playerCap.getStackInSlot(backPackSlot);
            if (playerBackpackitem.isEmpty() && !savedBackpackItem.isEmpty())
                savedBackpackItem.setCount(0);
            if (ItemStack.isSameItem(playerBackpackitem, savedBackpackItem)) {
                if (playerBackpackitem.getCount() > savedBackpackItem.getCount()) {
                    playerBackpackitem.setCount(playerBackpackitem.getCount() - savedBackpackItem.getCount());
                    player.drop(playerBackpackitem, true);
                }
                if (playerBackpackitem.getCount() < savedBackpackItem.getCount())
                    savedBackpackItem.setCount(playerBackpackitem.getCount());
                if (playerBackpackitem.getDamageValue() > savedBackpackItem.getDamageValue())
                    savedBackpackItem.setDamageValue(playerBackpackitem.getDamageValue());
            } else {
                player.drop(playerBackpackitem, true);
            }
            playerCap.setStackInSlot(backPackSlot, savedBackpackItem);
        }
    }
}
