package tallestegg.respawn_save_points;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.item.enchantment.BindingCurseEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
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
import net.minecraftforge.registries.ForgeRegistries;
import net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.violetmoon.quark.base.handler.ProxiedItemStackHandler;
import tallestegg.respawn_save_points.block_entities.RespawnAnchorBlockEntity;
import tallestegg.respawn_save_points.capablities.RSPCapabilities;
import tallestegg.respawn_save_points.capablities.SavedPlayerInventory;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.*;
import java.util.stream.Collectors;

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
                if (savedPlayerInventory != null && savedPlayerInventory.getUuid().equals(serverPlayer.getUUID())) {
                    Inventory inventory = serverPlayer.getInventory();
                    for (int i = 0; i < inventory.getContainerSize(); i++) {
                        ItemStack savedStack = savedPlayerInventory.getStackInSlot(i);
                        ItemStack playerStack = inventory.getItem(i);
                        if (savedPlayerInventory.getStackInSlot(i).isStackable() && Config.COMMON.percentageOfItemsKept.get().floatValue() < 1.0F && savedPlayerInventory.getStackInSlot(i).getCount() > 1)
                            savedPlayerInventory.getStackInSlot(i).setCount((int) (savedPlayerInventory.getStackInSlot(i).getCount() * Config.COMMON.percentageOfItemsKept.get().floatValue()));
                        if (ModList.get().isLoaded("sophisticatedbackpacks") && savedStack.getItem() instanceof BackpackItem) {
                            ItemStack originalStack = savedStack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).orElseGet(null).cloneBackpack();
                            savedStack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).orElseGet(null).setContentsUuid(UUID.randomUUID());
                            originalStack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).orElseGet(null).copyDataTo(new BackpackWrapper(savedStack)); // This shit made so fucking crazy like what the fuck who saves their shit with a UUID:??????????!?!?!?!?!??!!? I spent more time than anyone should working on this!!!!!!!
                        }
                        inventory.setItem(i, savedPlayerInventory.getStackInSlot(i).copy());
                    }
                    if (ModList.get().isLoaded("curios")) {
                        Optional<ICuriosItemHandler> curiosApi = CuriosApi.getCuriosInventory(player).resolve();
                        if (curiosApi.isPresent()) {
                            for (int i = 0; i < curiosApi.get().getSlots(); i++) {
                                if (ModList.get().isLoaded("sophisticatedbackpacks") && savedPlayerInventory.getCuriosStackInSlot(i).getItem() instanceof BackpackItem) {
                                    ItemStack originalStack = savedPlayerInventory.getCuriosStackInSlot(i).getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).orElseGet(null).cloneBackpack();
                                    savedPlayerInventory.getCuriosStackInSlot(i).getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).orElseGet(null).setContentsUuid(UUID.randomUUID());
                                    originalStack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).orElseGet(null).copyDataTo(new BackpackWrapper(savedPlayerInventory.getCuriosStackInSlot(i)));
                                }
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
    }

    @SubscribeEvent
    public void onDead(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            if (serverPlayer.getRespawnPosition() != null) {
                BlockEntity respawnPoint = serverPlayer.level().getBlockEntity(serverPlayer.getRespawnPosition());
                SavedPlayerInventory savedPlayerInventory = getSavedInventory(respawnPoint);
                if (savedPlayerInventory != null) {
                    for (int i = 0; i < serverPlayer.getInventory().getContainerSize(); i++) {
                        ItemStack savedStack = savedPlayerInventory.getStackInSlot(i);
                        ItemStack playerStack = serverPlayer.getInventory().getItem(i);
                        if (!savedStack.isEmpty() && playerStack.isEmpty() || !ItemStack.isSameItem(playerStack, savedStack))
                            savedPlayerInventory.setStackInSlot(i, ItemStack.EMPTY);
                        if (savedStack.getItem() instanceof BundleItem && playerStack.getItem() instanceof BundleItem) {
                            List<ItemStack> savedItems = getContents(savedStack);
                            List<ItemStack> playerItems = getContents(playerStack);
                            List<ItemStack> savedNestedBundles = new ArrayList<>();
                            List<ItemStack> unsavedNestedBundles = new ArrayList<>();
                            handleNestedBundles(savedItems, savedNestedBundles);
                            handleNestedBundles(playerItems, unsavedNestedBundles);
                            int limiter = Math.min(savedNestedBundles.size(), unsavedNestedBundles.size());
                            for (int slot = 0; slot < limiter; slot++) {
                                ItemStack savedNested = savedNestedBundles.get(slot);
                                ItemStack unSavedNested = unsavedNestedBundles.get(slot);
                                List<ItemStack> savedNestedBundleItemList = getContents(savedNested);
                                List<ItemStack> unsavedNestedBundleItemList = getContents(unSavedNested);
                                handleBundles(serverPlayer, savedNested, unSavedNested, savedNestedBundleItemList, unsavedNestedBundleItemList);
                            }
                            handleBundles(serverPlayer, savedStack, playerStack, savedItems, playerItems);
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
                                    if (Config.COMMON.itemBlacklist.get().contains(ForgeRegistries.ITEMS.getKey(shulkerItem.getItem()).toString()))
                                        serverPlayer.drop(shulkerItem, false);
                                    if (shulkerItem.isEmpty() && !savedShulkerItem.isEmpty() || !ItemStack.isSameItem(savedShulkerItem, shulkerItem))
                                        savedShulkerItemList.set(shulkerSlot, ItemStack.EMPTY);
                                    if (ItemStack.isSameItem(shulkerItem, savedShulkerItem)) {
                                        if (shulkerItem.getCount() > savedShulkerItem.getCount()) {
                                            shulkerItem.setCount(shulkerItem.getCount() - savedShulkerItem.getCount());
                                            serverPlayer.drop(shulkerItem, false);
                                        }
                                        if (shulkerItem.getCount() < savedShulkerItem.getCount())
                                            savedShulkerItem.setCount(shulkerItem.getCount());
                                        if (shulkerItem.getDamageValue() > savedShulkerItem.getDamageValue())
                                            savedShulkerItem.setDamageValue(shulkerItem.getDamageValue());
                                        if (!ItemStack.isSameItemSameTags(shulkerItem, savedShulkerItem)) {
                                            if (Config.COMMON.transferData.get()) {
                                                savedShulkerItemList.set(shulkerSlot, shulkerItem.copyAndClear());
                                            } else {
                                                shulkerItem.setCount(0);
                                            }
                                        }
                                    } else {
                                        serverPlayer.drop(shulkerItem, false);
                                    }
                                    ContainerHelper.saveAllItems(BlockItem.getBlockEntityData(savedStack), savedShulkerItemList);
                                    ContainerHelper.loadAllItems(BlockItem.getBlockEntityData(playerStack), shulkerItemList);
                                }
                                playerStack.setCount(0);
                            }
                        }
                        IItemHandlerModifiable playerBackpackHandler = RSPCapabilities.getItemModifiableCap(playerStack);
                        IItemHandlerModifiable savedBackpackHandler = RSPCapabilities.getItemModifiableCap(savedStack);
                        if (playerBackpackHandler != null && savedBackpackHandler != null)
                            cycleInventoryOnDeathCaps(savedStack, playerStack, savedBackpackHandler, playerBackpackHandler, playerBackpackHandler.getSlots(), serverPlayer);
                        removeAndModifyDroppedItems(serverPlayer, savedStack, playerStack, savedPlayerInventory, i);
                        respawnPoint.setChanged();
                    }
                }
                for (int i = 0; i < savedPlayerInventory.getCuriosItems().size(); i++) {
                    if (ModList.get().isLoaded("curios")) {
                        Optional<ICuriosItemHandler> curiosApi = CuriosApi.getCuriosInventory(serverPlayer).resolve();
                        ICuriosItemHandler playerCuriosHandler = curiosApi.get();
                        ItemStack savedCuriosStack = savedPlayerInventory.getCuriosStackInSlot(i);
                        ItemStack playerCuriosStack = playerCuriosHandler.getEquippedCurios().getStackInSlot(i);
                        if (!savedCuriosStack.isEmpty() && playerCuriosStack.isEmpty() || !ItemStack.isSameItem(savedCuriosStack, playerCuriosStack))
                            savedPlayerInventory.setCuriosStackInSlot(i, ItemStack.EMPTY);
                        IItemHandlerModifiable playerBackpackHandler = RSPCapabilities.getItemModifiableCap(playerCuriosStack);
                        IItemHandlerModifiable savedBackpackHandler = RSPCapabilities.getItemModifiableCap(savedCuriosStack);
                        if (playerBackpackHandler != null && savedBackpackHandler != null)
                            cycleInventoryOnDeathCaps(savedCuriosStack, playerCuriosStack, savedBackpackHandler, playerBackpackHandler, playerBackpackHandler.getSlots(), serverPlayer);
                        if (ModList.get().isLoaded("backpacked")) {
                            if (playerCuriosStack.getItem() instanceof com.mrcrayfish.backpacked.item.BackpackItem backpackItem && savedCuriosStack.getItem() instanceof com.mrcrayfish.backpacked.item.BackpackItem) {
                                for (int backPackSlot = 0; backPackSlot < (backpackItem.getColumnCount() * backpackItem.getRowCount()); backPackSlot++) {
                                    ItemStack savedBackpackItem = RespawnSavePoints.getItemsFromNBT(backPackSlot, savedCuriosStack);
                                    ItemStack playerBackpackitem = RespawnSavePoints.getItemsFromNBT(backPackSlot, playerCuriosStack);
                                    if (Config.COMMON.itemBlacklist.get().contains(BuiltInRegistries.ITEM.containsKey(ResourceLocation.tryParse(playerBackpackitem.getItem().toString()))))
                                        serverPlayer.drop(playerBackpackitem, false);
                                    if (playerBackpackitem.isEmpty() && !savedBackpackItem.isEmpty() || !ItemStack.isSameItem(playerBackpackitem, savedBackpackItem))
                                        RespawnSavePoints.setBackpackedBackpackItems(backPackSlot, savedCuriosStack, ItemStack.EMPTY);
                                    if (ItemStack.isSameItem(playerBackpackitem, savedBackpackItem)) {
                                        if (playerBackpackitem.getCount() > savedBackpackItem.getCount()) {
                                            playerBackpackitem.setCount(playerBackpackitem.getCount() - savedBackpackItem.getCount());
                                            serverPlayer.drop(playerBackpackitem.copy(), false);
                                        }
                                        if (playerBackpackitem.getCount() < savedBackpackItem.getCount()) {
                                            RespawnSavePoints.setBackpackedBackpackItems(backPackSlot, savedCuriosStack, savedBackpackItem.copyWithCount(playerBackpackitem.getCount()));
                                        }
                                        if (playerBackpackitem.getDamageValue() > savedBackpackItem.getDamageValue())
                                            savedBackpackItem.setDamageValue(playerBackpackitem.getDamageValue());
                                        if (!ItemStack.isSameItemSameTags(playerBackpackitem, savedBackpackItem)) {
                                            if (Config.COMMON.transferData.get()) {
                                                RespawnSavePoints.setBackpackedBackpackItems(backPackSlot, savedCuriosStack, playerBackpackitem.copy());
                                            } else {
                                                playerBackpackitem.setCount(0);
                                            }
                                        }
                                    } else {
                                        serverPlayer.drop(playerBackpackitem, false);
                                    }
                                }
                                playerCuriosStack.setCount(0);
                            }
                        }
                        removeAndModifyDroppedItems(serverPlayer, savedCuriosStack, playerCuriosStack, savedPlayerInventory, i);
                        respawnPoint.setChanged();
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            Level level = serverPlayer.level();
            if (serverPlayer.getRespawnPosition() != null && (level.getBlockEntity(serverPlayer.getRespawnPosition()) instanceof BedBlockEntity || level.getBlockEntity(serverPlayer.getRespawnPosition()) instanceof RespawnAnchorBlockEntity)) {
                if (!Config.COMMON.itemDrops.get()) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public void onDropXP(LivingExperienceDropEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer && serverPlayer.getRespawnPosition() != null && Config.COMMON.saveXP.get()) {
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
        List<String> itemsNotSaved = new ArrayList<>();
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
                            ItemStack curiosItemToBeSaved = curiosApi.get().getEquippedCurios().getStackInSlot(i);
                            if (Config.COMMON.itemBlacklist.get().contains(ForgeRegistries.ITEMS.getKey(curiosItemToBeSaved.getItem()).toString()))
                                itemsNotSaved.add(curiosItemToBeSaved.getHoverName().copy().getString());
                            savedPlayerInventory.setCuriosStackInSlot(i, curiosItemToBeSaved.copy());
                        }
                        if (Config.COMMON.includedItemsMessage.get())
                            serverPlayer.sendSystemMessage(Component.translatable("message.respawn_save_points.saved_curios"));
                        itemsNotSaved.clear();
                    }
                }
                Inventory inventory = serverPlayer.getInventory();
                savedPlayerInventory.setSize(inventory.getContainerSize());
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    ItemStack inventoryItemToBeSaved = inventory.getItem(i);
                    if (Config.COMMON.itemBlacklist.get().contains(ForgeRegistries.ITEMS.getKey(inventoryItemToBeSaved.getItem()).toString()))
                        itemsNotSaved.add(inventoryItemToBeSaved.getHoverName().copy().getString());
                    savedPlayerInventory.setStackInSlot(i, inventoryItemToBeSaved.copy());
                    if (Config.COMMON.saveXP.get()) {
                        savedPlayerInventory.setExperienceLevel(serverPlayer.experienceLevel);
                        savedPlayerInventory.setTotalExperience(serverPlayer.totalExperience);
                        savedPlayerInventory.setExperienceProgress(serverPlayer.experienceProgress);
                    }
                    level.getBlockEntity(blockPos).setChanged();
                }
                if (Config.COMMON.includedItemsMessage.get())
                    serverPlayer.sendSystemMessage(Component.translatable("message.respawn_save_points.saved"));
                if (!itemsNotSaved.isEmpty() && Config.COMMON.excludedItemsMessage.get())
                    serverPlayer.sendSystemMessage(Component.translatable("message.respawn_save_points.not_saved", ArrayUtils.toString(itemsNotSaved)));
                savedPlayerInventory.setUuid(serverPlayer.getUUID());
                level.getBlockEntity(blockPos).setChanged();
                itemsNotSaved.clear();
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

    public static void cycleInventoryOnDeathCaps(ItemStack savedStack, ItemStack unsavedStack, IItemHandlerModifiable savedCap, IItemHandlerModifiable playerCap, int inventorySize, ServerPlayer player) {
        if (ItemStack.isSameItem(savedStack, unsavedStack)) {
            for (int backPackSlot = 0; backPackSlot < inventorySize; backPackSlot++) {
                ItemStack savedBackpackItem = savedCap.getStackInSlot(backPackSlot);
                ItemStack playerBackpackitem = playerCap.getStackInSlot(backPackSlot);
                if (playerBackpackitem.isEmpty() && !savedBackpackItem.isEmpty() || !ItemStack.isSameItem(savedBackpackItem, playerBackpackitem)) {
                    savedCap.setStackInSlot(backPackSlot, ItemStack.EMPTY);
                }
                if (Config.COMMON.itemBlacklist.get().contains(ForgeRegistries.ITEMS.getKey(playerBackpackitem.getItem()).toString()))
                    player.drop(playerBackpackitem, false);
                if (ItemStack.isSameItem(playerBackpackitem, savedBackpackItem)) {
                    if (playerBackpackitem.getCount() > savedBackpackItem.getCount()) {
                        playerBackpackitem.setCount(playerBackpackitem.getCount() - savedBackpackItem.getCount());
                        player.drop(playerBackpackitem, false);
                        savedCap.setStackInSlot(backPackSlot, savedBackpackItem.copy());
                    }
                    if (playerBackpackitem.getCount() < savedBackpackItem.getCount()) {
                        savedBackpackItem.setCount(playerBackpackitem.getCount());
                    }
                    if (playerBackpackitem.getDamageValue() > savedBackpackItem.getDamageValue())
                        savedBackpackItem.setDamageValue(playerBackpackitem.getDamageValue());
                    if (!ItemStack.isSameItemSameTags(playerBackpackitem, savedBackpackItem)) {
                        if (Config.COMMON.transferData.get()) {
                            savedCap.setStackInSlot(backPackSlot, playerBackpackitem.copyAndClear());
                        } else {
                            playerBackpackitem.setCount(0);
                        }
                    }
                } else {
                    player.drop(playerBackpackitem, false);
                }
            }
            unsavedStack.setCount(0);
        }
    }

    public static void handleBundleItems(List<ItemStack> savedItems, List<ItemStack> playerItems, ServerPlayer serverPlayer, ItemStack playerStack) {
        for (ItemStack savedBundled : savedItems) {
            playerItems.stream().filter(itemStack -> ItemStack.isSameItem(itemStack, savedBundled) && itemStack.getCount() < savedBundled.getCount()).findAny().ifPresent(itemStack -> savedBundled.setCount(itemStack.getCount()));
            playerItems.stream().filter(itemStack -> ItemStack.isSameItem(itemStack, savedBundled) && itemStack.getCount() > savedBundled.getCount()).findAny().ifPresent(itemStack -> serverPlayer.drop(itemStack.copyWithCount(itemStack.getCount() - savedBundled.getCount()), false));
            playerItems.removeIf(itemStack -> ItemStack.isSameItem(itemStack, savedBundled) && itemStack.getCount() > savedBundled.getCount());
            if (Config.COMMON.transferDurability.get()) {
                playerItems.stream().filter(itemStack -> ItemStack.isSameItem(itemStack, savedBundled) && itemStack.getDamageValue() != savedBundled.getDamageValue()).findAny().ifPresent(itemStack -> savedBundled.setDamageValue(itemStack.getDamageValue()));
            } else {
                playerItems.removeIf(itemStack -> ItemStack.isSameItem(itemStack, savedBundled) && itemStack.getDamageValue() != savedBundled.getDamageValue());
            }
            playerItems.removeIf(itemStack -> ItemStack.matches(itemStack, savedBundled));
            playerItems.forEach(itemStack -> serverPlayer.drop(itemStack, false));
        }
        playerStack.setCount(0);
    }

    public static void handleBundles(ServerPlayer serverPlayer, ItemStack savedStack, ItemStack playerStack, List<ItemStack> savedItems, List<ItemStack> playerItems) {
        if (playerItems.isEmpty() && !savedItems.isEmpty()) {
            savedStack.removeTagKey("Items");
        } else if (!playerItems.isEmpty() && savedItems.isEmpty()) {
            playerItems.forEach(itemStack -> serverPlayer.drop(itemStack, false));
        }
        handleBundleItems(savedItems, playerItems, serverPlayer, playerStack);
    }

    public static List<ItemStack> handleNestedBundles(List<ItemStack> bundleItemList, List<ItemStack> nestedBundles) {
        for (ItemStack savedBundleItem : bundleItemList) {
            if (savedBundleItem.getItem() instanceof BundleItem) {
                nestedBundles.add(savedBundleItem);
                handleNestedBundles(getContents(savedBundleItem), nestedBundles);
                return nestedBundles;
            }
        }
        return nestedBundles;
    }

    private static List<ItemStack> getContents(ItemStack itemStack) {
        CompoundTag compoundtag = itemStack.getTag();
        if (compoundtag == null) {
            return Collections.emptyList();
        } else {
            ListTag listtag = compoundtag.getList("Items", 10);
            return listtag.stream().map(CompoundTag.class::cast).map(ItemStack::of).collect(Collectors.toCollection((ArrayList::new)));
        }
    }

    public static void removeAndModifyDroppedItems(ServerPlayer serverPlayer, ItemStack savedStack, ItemStack playerStack, SavedPlayerInventory savedPlayerInventory, int slot) {
        if (ItemStack.isSameItem(playerStack, savedStack)) {
            if (playerStack.getCount() < savedStack.getCount()) {
                savedStack.setCount(playerStack.getCount());
            }
            if (playerStack.getDamageValue() != savedStack.getDamageValue())
                savedStack.setDamageValue(playerStack.getDamageValue());
            if (!ItemStack.isSameItemSameTags(playerStack, savedStack)) {
                if (Config.COMMON.transferData.get()) {
                    savedPlayerInventory.setStackInSlot(slot, playerStack.copyAndClear());
                } else {
                    playerStack.setCount(0);
                }
            }
        }
        if (EnchantmentHelper.hasBindingCurse(savedStack)) {
            Map<Enchantment, Integer> map = EnchantmentHelper.getEnchantments(savedStack).entrySet().stream().filter((p_39584_) -> !(p_39584_.getKey() instanceof BindingCurseEnchantment)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            EnchantmentHelper.setEnchantments(map, savedStack);
        }
        if (ItemStack.matches(savedStack, playerStack))
            playerStack.setCount(0);
        if (playerStack.getCount() > savedStack.getCount())
            playerStack.setCount(playerStack.getCount() - savedStack.getCount());
    }
}
