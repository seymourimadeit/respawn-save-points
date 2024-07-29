package tallestegg.better_respawn_options;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.apache.commons.lang3.ArrayUtils;
import tallestegg.better_respawn_options.block_entities.BROBlockEntities;
import tallestegg.better_respawn_options.block_entities.RespawnAnchorBlockEntity;
import tallestegg.better_respawn_options.data_attachments.BROData;
import tallestegg.better_respawn_options.data_attachments.SavedPlayerInventory;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(BetterRespawnOptions.MODID)
public class BetterRespawnOptions {
    public static final String MODID = "respawn_save_points";

    public BetterRespawnOptions(IEventBus modEventBus, Dist dist, ModContainer container) {
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::addCreative);
        container.registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);
        BROData.ATTACHMENT_TYPES.register(modEventBus);
        BROBlockEntities.BLOCK_ENTITIES.register(modEventBus);
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
                if (savedPlayerInventory != null) {
                    if (savedPlayerInventory.getUuid() != null && serverPlayer.getUUID().equals(savedPlayerInventory.getUuid())) {
                        Inventory inventory = serverPlayer.getInventory();
                        for (int i = 0; i < inventory.getContainerSize(); i++) {
                            if (savedPlayerInventory.getStackInSlot(i).isStackable() && Config.COMMON.percentageOfItemsKept.get().floatValue() < 1.0F && savedPlayerInventory.getStackInSlot(i).getCount() > 1)
                                savedPlayerInventory.getStackInSlot(i).setCount((int) (savedPlayerInventory.getStackInSlot(i).getCount() * Config.COMMON.percentageOfItemsKept.get().floatValue()));
                            inventory.setItem(i, savedPlayerInventory.getStackInSlot(i).copy());
                        }
                        if (ModList.get().isLoaded("curios")) {
                            Optional<ICuriosItemHandler> curiosApi = CuriosApi.getCuriosInventory(player);
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
        }
    }

    @SubscribeEvent
    public void onDead(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer && serverPlayer.getRespawnPosition() != null) {
            if (serverPlayer.level().getBlockEntity(serverPlayer.getRespawnPosition()) != null && serverPlayer.level().getBlockEntity(serverPlayer.getRespawnPosition()).hasData(BROData.SAVED_INVENTORY.get())) {
                SavedPlayerInventory savedPlayerInventory = getSavedInventory(serverPlayer.level().getBlockEntity(serverPlayer.getRespawnPosition()));
                BlockEntity respawnPoint = serverPlayer.level().getBlockEntity(serverPlayer.getRespawnPosition());
                for (int i = 0; i < serverPlayer.getInventory().getContainerSize(); i++) {
                    ItemStack savedStack = savedPlayerInventory.getStackInSlot(i);
                    ItemStack playerStack = serverPlayer.getInventory().getItem(i);
                    if (!savedStack.isEmpty() && playerStack.isEmpty() || !ItemStack.isSameItem(playerStack, savedStack))
                        savedPlayerInventory.setStackInSlot(i, ItemStack.EMPTY);
                    if (savedStack.getItem() instanceof BundleItem && playerStack.getItem() instanceof BundleItem) {
                        BundleContents.Mutable bundleItemList = new BundleContents.Mutable(playerStack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY));
                        BundleContents.Mutable savedBundleItemList = new BundleContents.Mutable(savedStack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY));
                        List<ItemStack> savedItems = savedBundleItemList.items;
                        List<ItemStack> playerItems = bundleItemList.items;
                        List<ItemStack> savedNestedBundles = new ArrayList<>();
                        List<ItemStack> unsavedNestedBundles = new ArrayList<>();
                        handleNestedBundles(savedItems, savedNestedBundles);
                        handleNestedBundles(playerItems, unsavedNestedBundles);
                        int limiter = Math.min(savedNestedBundles.size(), unsavedNestedBundles.size());
                        for (int slot = 0; slot < limiter; slot++) {
                            ItemStack savedNested = savedNestedBundles.get(slot);
                            ItemStack unSavedNested = unsavedNestedBundles.get(slot);
                            BundleContents.Mutable savedNestedBundleItemList = new BundleContents.Mutable(savedNested.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY));
                            BundleContents.Mutable unsavedNestedBundleItemList = new BundleContents.Mutable(unSavedNested.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY));
                            handleBundles(serverPlayer, savedNested, unSavedNested, savedNestedBundleItemList.items, unsavedNestedBundleItemList.items, unsavedNestedBundleItemList, savedNestedBundleItemList);
                        }
                        handleBundles(serverPlayer, savedStack, playerStack, savedItems, playerItems, bundleItemList, savedBundleItemList);
                    }
                    IItemHandlerModifiable componentItemHandlerList = (IItemHandlerModifiable) playerStack.getCapability(Capabilities.ItemHandler.ITEM);
                    IItemHandlerModifiable savedComponentItemHandlerList = (IItemHandlerModifiable) savedStack.getCapability(Capabilities.ItemHandler.ITEM);
                    handleItemCaps(componentItemHandlerList, savedComponentItemHandlerList, serverPlayer, playerStack, savedStack);
                    removeAndModifyDroppedItems(savedStack, playerStack, savedPlayerInventory, i);
                }
                for (int i = 0; i < savedPlayerInventory.getCuriosItems().size(); i++) {
                    if (ModList.get().isLoaded("curios")) {
                        Optional<ICuriosItemHandler> curiosApi = CuriosApi.getCuriosInventory(serverPlayer);
                        ICuriosItemHandler playerCuriosHandler = curiosApi.get();
                        ItemStack savedCuriosStack = savedPlayerInventory.getCuriosStackInSlot(i);
                        ItemStack playerCuriosStack = playerCuriosHandler.getEquippedCurios().getStackInSlot(i);
                        if (!savedCuriosStack.isEmpty() && playerCuriosStack.isEmpty() || !ItemStack.isSameItem(playerCuriosStack, savedCuriosStack))
                            savedPlayerInventory.setCuriosStackInSlot(i, ItemStack.EMPTY);
                        // For the love of god random backpack mods please use this system please man!!!!!!!!!
                        IItemHandlerModifiable componentItemHandlerList = (IItemHandlerModifiable) playerCuriosStack.getCapability(Capabilities.ItemHandler.ITEM);
                        IItemHandlerModifiable savedComponentItemHandlerList = (IItemHandlerModifiable) savedCuriosStack.getCapability(Capabilities.ItemHandler.ITEM);
                        handleItemCaps(componentItemHandlerList, savedComponentItemHandlerList, serverPlayer, playerCuriosStack, savedCuriosStack);
                        removeAndModifyDroppedItems(savedCuriosStack, playerCuriosStack, savedPlayerInventory, i);
                    }
                }
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
                Inventory inventory = serverPlayer.getInventory();
                if (Config.COMMON.itemDrops.get()) {
                    for (int i = 0; i < inventory.getContainerSize(); i++) {
                        ItemStack stack = savedPlayerInventory.getStackInSlot(i);
                        EnchantmentHelper.updateEnchantments(
                                stack, p_330066_ -> p_330066_.removeIf(p_344368_ -> p_344368_.is(Enchantments.BINDING_CURSE))
                        );
                        level.getBlockEntity(serverPlayer.getRespawnPosition()).setChanged();
                    }
                    for (int i = 0; i < savedPlayerInventory.getCuriosItems().size(); i++) {
                        ItemStack stack = savedPlayerInventory.getCuriosStackInSlot(i);
                        EnchantmentHelper.updateEnchantments(
                                stack, p_330066_ -> p_330066_.removeIf(p_344368_ -> p_344368_.is(Enchantments.BINDING_CURSE))
                        );
                        level.getBlockEntity(serverPlayer.getRespawnPosition()).setChanged();
                    }
                } else {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public void onDropXP(LivingExperienceDropEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer && serverPlayer.getRespawnPosition() != null && serverPlayer.level().getBlockEntity(serverPlayer.getRespawnPosition()) != null && serverPlayer.level().getBlockEntity(serverPlayer.getRespawnPosition()).hasData(BROData.SAVED_INVENTORY.get()) && Config.COMMON.saveXP.get()) {
            SavedPlayerInventory savedPlayerInventory = getSavedInventory(serverPlayer.level().getBlockEntity(serverPlayer.getRespawnPosition()));
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
                Inventory inventory = serverPlayer.getInventory();
                savedPlayerInventory.setSize(inventory.getContainerSize());
                List<String> itemsNotSaved = new ArrayList<>();
                if (ModList.get().isLoaded("curios")) {
                    Optional<ICuriosItemHandler> curiosApi = CuriosApi.getCuriosInventory(player);
                    if (curiosApi.isPresent()) {
                        savedPlayerInventory.setCuriosItemsSize(curiosApi.get().getSlots());
                        for (int i = 0; i < curiosApi.get().getSlots(); i++) {
                            ItemStack curiosItemToBeSaved = curiosApi.get().getEquippedCurios().getStackInSlot(i);
                            if (Config.COMMON.itemBlacklist.get().contains(BuiltInRegistries.ITEM.getKey(curiosItemToBeSaved.getItem()).toString()))
                                itemsNotSaved.add(curiosItemToBeSaved.getHoverName().copy().getString());
                            savedPlayerInventory.setCuriosStackInSlot(i, curiosItemToBeSaved.copy());
                        }
                    }
                }
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    ItemStack inventoryItemToBeSaved = inventory.getItem(i);
                    if (Config.COMMON.itemBlacklist.get().contains(BuiltInRegistries.ITEM.getKey(inventoryItemToBeSaved.getItem()).toString()))
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
                level.getBlockEntity(blockPos).setChanged();
                savedPlayerInventory.setUuid(serverPlayer.getUUID());
                itemsNotSaved.clear();
            }
        }
    }

    public static SavedPlayerInventory getSavedInventory(BlockEntity blockEntity) {
        return blockEntity.getData(BROData.SAVED_INVENTORY); // This applies even to the custom RespawnAnchorBlockEntity class for compatibility with other mods
    }

    // Nested bundles do exist, so we have to account for that in the system
    public static List<ItemStack> handleNestedBundles(List<ItemStack> bundleItemList, List<ItemStack> nestedBundles) {
        for (ItemStack savedBundleItem : bundleItemList) {
            if (savedBundleItem.getItem() instanceof BundleItem) {
                nestedBundles.add(savedBundleItem);
                BundleContents.Mutable itemList = new BundleContents.Mutable(savedBundleItem.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY));
                handleNestedBundles(itemList.items, nestedBundles);
                return nestedBundles;
            }
        }
        return nestedBundles;
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
            if (Config.COMMON.transferData.get()) {
                playerItems.stream().filter(itemStack -> ItemStack.isSameItem(itemStack, savedBundled) && !ItemStack.isSameItemSameComponents(savedBundled, itemStack)).findAny().ifPresent(itemStack -> savedBundled.applyComponents(itemStack.getComponents()));
            } else {
                playerItems.removeIf(itemStack -> ItemStack.isSameItem(itemStack, savedBundled) && !ItemStack.isSameItemSameComponents(savedBundled, itemStack));
            }
            playerItems.removeIf(itemStack -> ItemStack.matches(itemStack, savedBundled));
            playerItems.forEach(itemStack -> serverPlayer.drop(itemStack, false));
        }
        playerStack.setCount(0);
    }

    public static void handleBundles(ServerPlayer serverPlayer, ItemStack savedStack, ItemStack playerStack, List<ItemStack> savedItems, List<ItemStack> playerItems, BundleContents.Mutable bundleItemList, BundleContents.Mutable savedBundleItemList) {
        if (playerItems.isEmpty() && !savedItems.isEmpty()) {
            savedBundleItemList.clearItems();
            savedStack.set(DataComponents.BUNDLE_CONTENTS, savedBundleItemList.toImmutable());
        } else if (!playerItems.isEmpty() && savedItems.isEmpty()) {
            playerItems.forEach(itemStack -> serverPlayer.drop(itemStack, false));
        }
        handleBundleItems(savedItems, playerItems, serverPlayer, playerStack);
    }

    public static void removeAndModifyDroppedItems(ItemStack savedStack, ItemStack playerStack, SavedPlayerInventory savedPlayerInventory, int slot) {
        if (ItemStack.isSameItem(playerStack, savedStack)) {
            if (playerStack.getCount() < savedStack.getCount()) {
                savedStack.setCount(playerStack.getCount());
            }
            if (playerStack.getDamageValue() != savedStack.getDamageValue())
                savedStack.setDamageValue(playerStack.getDamageValue());
            if (!ItemStack.isSameItemSameComponents(playerStack, savedStack)) {
                if (Config.COMMON.transferData.get()) {
                    savedPlayerInventory.setStackInSlot(slot, playerStack.copyAndClear());
                } else {
                    playerStack.setCount(0);
                }
            }
        }
        if (ItemStack.matches(savedStack, playerStack))
            playerStack.setCount(0);
        if (playerStack.getCount() > savedStack.getCount()) {
            playerStack.setCount(playerStack.getCount() - savedStack.getCount());
        }
    }

    public static void handleItemCaps(IItemHandlerModifiable componentItemHandlerList, IItemHandlerModifiable savedComponentItemHandlerList, ServerPlayer serverPlayer, ItemStack unsavedStack, ItemStack savedStack) {
        if (componentItemHandlerList != null && savedComponentItemHandlerList != null) {
            if (ItemStack.isSameItem(unsavedStack, savedStack)) {
                for (int componentSlots = 0; componentSlots < savedComponentItemHandlerList.getSlots(); componentSlots++) {
                    ItemStack unSavedItem = componentItemHandlerList.getStackInSlot(componentSlots);
                    ItemStack savedItem = savedComponentItemHandlerList.getStackInSlot(componentSlots);
                    if (unSavedItem.isEmpty() && !savedItem.isEmpty() || !ItemStack.isSameItem(savedItem, unSavedItem))
                        savedComponentItemHandlerList.setStackInSlot(componentSlots, ItemStack.EMPTY);
                    if (Config.COMMON.itemBlacklist.get().contains(BuiltInRegistries.ITEM.getKey(unSavedItem.getItem()).toString())) {
                        serverPlayer.drop(unSavedItem, false);
                        savedComponentItemHandlerList.setStackInSlot(componentSlots, ItemStack.EMPTY);
                    }
                    if (ItemStack.isSameItem(unSavedItem, savedItem)) {
                        if (unSavedItem.getCount() > savedItem.getCount()) {
                            unSavedItem.setCount(unSavedItem.getCount() - savedItem.getCount());
                            serverPlayer.drop(unSavedItem, false);
                        }
                        if (unSavedItem.getCount() < savedItem.getCount()) {
                            savedItem.setCount(unSavedItem.getCount());
                            savedComponentItemHandlerList.setStackInSlot(componentSlots, savedItem.copyAndClear());
                        }
                        if (unSavedItem.getDamageValue() != savedItem.getDamageValue())
                            savedItem.setDamageValue(unSavedItem.getDamageValue());
                        if (!ItemStack.isSameItemSameComponents(unSavedItem, savedItem)) {
                            if (Config.COMMON.transferData.get()) {
                                savedComponentItemHandlerList.setStackInSlot(componentSlots, unSavedItem.copyAndClear());
                            } else {
                                unSavedItem.setCount(0);
                            }
                        }
                    } else {
                        serverPlayer.drop(unSavedItem, false);
                    }
                }
                unsavedStack.setCount(0);
            }
        }
    }

    @Mod(value = MODID, dist = Dist.CLIENT)
    public static class BetterRespawnOptionsClient {
        public BetterRespawnOptionsClient(IEventBus modEventBus, Dist dist, ModContainer container) {
            container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }
    }
}
