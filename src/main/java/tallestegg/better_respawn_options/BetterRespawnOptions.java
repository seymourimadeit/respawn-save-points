package tallestegg.better_respawn_options;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.items.ComponentItemHandler;
import org.apache.commons.lang3.ArrayUtils;
import tallestegg.better_respawn_options.block_entities.BROBlockEntities;
import tallestegg.better_respawn_options.block_entities.RespawnAnchorBlockEntity;
import tallestegg.better_respawn_options.data_attachments.BROData;
import tallestegg.better_respawn_options.data_attachments.SavedPlayerInventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
                    Inventory inventory = serverPlayer.getInventory();
                    for (int i = 0; i < inventory.getContainerSize(); i++) {
                        if (savedPlayerInventory.getStackInSlot(i).isStackable() && Config.COMMON.percentageOfItemsKept.get().floatValue() < 1.0F && savedPlayerInventory.getStackInSlot(i).getCount() > 1)
                            savedPlayerInventory.getStackInSlot(i).setCount((int) (savedPlayerInventory.getStackInSlot(i).getCount() * Config.COMMON.percentageOfItemsKept.get().floatValue()));
                        inventory.setItem(i, savedPlayerInventory.getStackInSlot(i).copy());
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
                        if (bundleItemList.items.isEmpty() && !savedBundleItemList.items.isEmpty()) {
                            savedBundleItemList.clearItems();
                            savedStack.set(DataComponents.BUNDLE_CONTENTS, savedBundleItemList.toImmutable());
                        }
                        int limiter = Math.min(bundleItemList.items.size(), savedBundleItemList.items.size());
                        for (int bundleSlot = 0; bundleSlot < limiter; bundleSlot++) {
                            ItemStack bundleItem = bundleItemList.items.get(bundleSlot);
                            ItemStack savedBundleItem = savedBundleItemList.items.get(bundleSlot);
                            if (Config.COMMON.itemBlacklist.get().contains(BuiltInRegistries.ITEM.getKey(bundleItem.getItem()).toString()))
                                serverPlayer.drop(bundleItem, false);
                            if (ItemStack.isSameItem(bundleItem, savedBundleItem)) {
                                if (bundleItem.getCount() > savedBundleItem.getCount()) {
                                    bundleItem.setCount(bundleItem.getCount() - savedBundleItem.getCount());
                                    serverPlayer.drop(bundleItem, false);
                                }
                                if (bundleItem.getCount() < savedBundleItem.getCount())
                                    savedBundleItem.setCount(bundleItem.getCount());
                                if (bundleItem.getDamageValue() > savedBundleItem.getDamageValue())
                                    savedBundleItem.setDamageValue(bundleItem.getDamageValue());
                            } else {
                                savedBundleItemList.items.remove(bundleSlot);
                                savedStack.set(DataComponents.BUNDLE_CONTENTS, savedBundleItemList.toImmutable());
                                serverPlayer.drop(bundleItem, false);
                            }
                            playerStack.setCount(0);
                        }
                    }
                    if (savedStack.getItem() instanceof BlockItem savedBlockItem && playerStack.getItem() instanceof BlockItem playerBlockItem) {
                        if (savedBlockItem.getBlock() instanceof ShulkerBoxBlock && playerBlockItem.getBlock() instanceof ShulkerBoxBlock) {
                            ComponentItemHandler shulkerContainerList = (ComponentItemHandler) playerStack.getCapability(Capabilities.ItemHandler.ITEM);
                            ComponentItemHandler savedShulkerContainerList = (ComponentItemHandler) savedStack.getCapability(Capabilities.ItemHandler.ITEM);
                            for (int shulkerSlot = 0; shulkerSlot < savedShulkerContainerList.getSlots(); shulkerSlot++) {
                                ItemStack shulkerItem = shulkerContainerList.getStackInSlot(shulkerSlot);
                                ItemStack savedShulkerItem = savedShulkerContainerList.getStackInSlot(shulkerSlot);
                                if (shulkerItem.isEmpty() && !savedShulkerItem.isEmpty() || !ItemStack.isSameItem(savedShulkerItem, shulkerItem)) {
                                    savedShulkerContainerList.setStackInSlot(shulkerSlot, ItemStack.EMPTY);
                                }
                                if (Config.COMMON.itemBlacklist.get().contains(BuiltInRegistries.ITEM.getKey(shulkerItem.getItem()).toString()))
                                    serverPlayer.drop(shulkerItem, false);
                                if (ItemStack.isSameItem(shulkerItem, savedShulkerItem)) {
                                    if (shulkerItem.getCount() > savedShulkerItem.getCount()) {
                                        shulkerItem.setCount(shulkerItem.getCount() - savedShulkerItem.getCount());
                                        serverPlayer.drop(shulkerItem, false);
                                    }
                                    if (shulkerItem.getCount() < savedShulkerItem.getCount()) {
                                        savedShulkerItem.setCount(shulkerItem.getCount());
                                    }
                                    if (shulkerItem.getDamageValue() > savedShulkerItem.getDamageValue())
                                        savedShulkerItem.setDamageValue(shulkerItem.getDamageValue());
                                    if (Config.COMMON.transferData.get() && !ItemStack.isSameItemSameComponents(savedShulkerItem, shulkerItem))
                                        savedShulkerItem.applyComponents(shulkerItem.getComponents());

                                } else {
                                    serverPlayer.drop(shulkerItem, false);
                                }
                            }
                            playerStack.setCount(-1);
                        }
                        respawnPoint.setChanged();
                    }
                }
        /*    for (int i = 0; i < savedPlayerInventory.getCuriosItems().size(); i++) {
                if (ModList.get().isLoaded("curios")) {
                    Optional<ICuriosItemHandler> curiosApi = CuriosApi.getCuriosInventory(serverPlayer);
                    ICuriosItemHandler playerCuriosHandler = curiosApi.orElse(null);
                    if (playerCuriosHandler == null)
                        return;
                    ItemStack savedCuriosStack = savedPlayerInventory.getCuriosStackInSlot(i);
                    ItemStack playerCuriosStack = playerCuriosHandler.getEquippedCurios().getStackInSlot(i);
                    if (!savedCuriosStack.isEmpty() && playerCuriosStack.isEmpty())
                        savedPlayerInventory.setStackInSlot(i, ItemStack.EMPTY);
                }
            }*/
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
                        removeAndModifyDroppedItems(event.getDrops(), serverPlayer, stack, level);
                    }
                } else {
                    event.setCanceled(true);
                }
           /*     for (int i = 0; i < savedPlayerInventory.getCuriosItems().size(); i++) {
                    ItemStack stack = savedPlayerInventory.getCuriosStackInSlot(i);
                    event.getDrops().stream().findAny().filter(itemEntity -> ItemStack.isSameItem(itemEntity.getItem(), stack) && itemEntity.getItem().getCount() < stack.getCount()).ifPresent(itemEntity -> stack.setCount(itemEntity.getItem().getCount()));
                    event.getDrops().stream().findAny().filter(itemEntity -> ItemStack.isSameItem(itemEntity.getItem(), stack) && itemEntity.getItem().getCount() > stack.getCount()).ifPresent(itemEntity -> itemEntity.getItem().setCount(itemEntity.getItem().getCount() - stack.getCount()));
                    if (Config.COMMON.transferDurability.get())
                        event.getDrops().stream().findAny().filter(itemEntity -> ItemStack.isSameItem(itemEntity.getItem(), stack) && itemEntity.getItem().getDamageValue() > stack.getDamageValue()).ifPresent(itemEntity -> stack.setDamageValue(itemEntity.getItem().getDamageValue()));
                    event.getDrops().removeIf(itemEntity -> ItemStack.isSameItem(itemEntity.getItem(), stack) && itemEntity.getItem().getDamageValue() > stack.getDamageValue());
                    event.getDrops().removeIf(itemEntity -> ItemStack.matches(itemEntity.getItem(), stack));
                    level.getBlockEntity(serverPlayer.getRespawnPosition()).setChanged();
                }*/
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
             /*   if (ModList.get().isLoaded("curios")) {
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
                }*/
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
                itemsNotSaved.clear();
            }
        }
    }

    public static SavedPlayerInventory getSavedInventory(BlockEntity blockEntity) {
        return blockEntity.getData(BROData.SAVED_INVENTORY); // This applies even to the custom RespawnAnchorBlockEntity class for compatibility with other mods
    }

    public static void removeAndModifyDroppedItems(Collection<ItemEntity> drops, ServerPlayer player, ItemStack savedStack, Level level) {
        drops.stream().findAny().filter(itemEntity -> ItemStack.isSameItem(itemEntity.getItem(), savedStack) && itemEntity.getItem().getCount() < savedStack.getCount()).ifPresent(itemEntity -> savedStack.setCount(itemEntity.getItem().getCount()));
        drops.stream().findAny().filter(itemEntity -> ItemStack.isSameItem(itemEntity.getItem(), savedStack) && itemEntity.getItem().getCount() > savedStack.getCount()).ifPresent(itemEntity -> itemEntity.getItem().setCount(itemEntity.getItem().getCount() - savedStack.getCount()));
        if (Config.COMMON.transferDurability.get()) {
            drops.stream().findAny().filter(itemEntity -> ItemStack.isSameItem(itemEntity.getItem(), savedStack) && itemEntity.getItem().getDamageValue() > savedStack.getDamageValue()).ifPresent(itemEntity -> savedStack.setDamageValue(itemEntity.getItem().getDamageValue()));
            drops.stream().findAny().filter(itemEntity -> ItemStack.isSameItem(itemEntity.getItem(), savedStack) && itemEntity.getItem().getDamageValue() < savedStack.getDamageValue()).ifPresent(itemEntity -> savedStack.setDamageValue(itemEntity.getItem().getDamageValue()));
        }
        drops.removeIf(itemEntity -> ItemStack.isSameItem(itemEntity.getItem(), savedStack) && itemEntity.getItem().getDamageValue() > savedStack.getDamageValue());
        if (Config.COMMON.transferData.get())
            drops.stream().findAny().filter(itemEntity -> ItemStack.isSameItem(itemEntity.getItem(), savedStack) && !ItemStack.isSameItemSameComponents(savedStack, itemEntity.getItem())).ifPresent(itemEntity -> savedStack.applyComponents(itemEntity.getItem().getComponents()));
        drops.removeIf(itemEntity -> ItemStack.matches(itemEntity.getItem(), savedStack));
        EnchantmentHelper.updateEnchantments(
                savedStack, p_330066_ -> p_330066_.removeIf(p_344368_ -> p_344368_.is(Enchantments.BINDING_CURSE))
        );
        level.getBlockEntity(player.getRespawnPosition()).setChanged();
    }
}
