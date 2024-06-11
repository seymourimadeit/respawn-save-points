package tallestegg.better_respawn_options;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.neoforged.api.distmarker.Dist;
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
import tallestegg.better_respawn_options.block_entities.BROBlockEntities;
import tallestegg.better_respawn_options.block_entities.RespawnAnchorBlockEntity;
import tallestegg.better_respawn_options.data_attachments.BROData;
import tallestegg.better_respawn_options.data_attachments.SavedPlayerInventory;

import java.util.List;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(BetterRespawnOptions.MODID)
public class BetterRespawnOptions {
    public static final String MODID = "better_respawn_options";

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
                Inventory inventory = serverPlayer.getInventory();
                for (int i = 0; i < inventory.getContainerSize(); i++) {
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

    @SubscribeEvent
    public void onDead(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer && serverPlayer.level().getBlockEntity(serverPlayer.getRespawnPosition()).hasData(BROData.SAVED_INVENTORY.get())) {
            SavedPlayerInventory savedPlayerInventory = getSavedInventory(serverPlayer.level().getBlockEntity(serverPlayer.getRespawnPosition()));
            BlockEntity respawnPoint = serverPlayer.level().getBlockEntity(serverPlayer.getRespawnPosition());
            for (int i = 0; i < serverPlayer.getInventory().getContainerSize(); i++) {
                ItemStack savedStack = savedPlayerInventory.getStackInSlot(i);
                ItemStack playerStack = serverPlayer.getInventory().getItem(i);
                if (!savedPlayerInventory.getStackInSlot(i).isEmpty() && serverPlayer.getInventory().getItem(i).isEmpty())
                    savedPlayerInventory.setStackInSlot(i, ItemStack.EMPTY);
                if (savedStack.getItem() instanceof BundleItem && playerStack.getItem() instanceof BundleItem) {
                    BundleContents.Mutable bundleItemList = new BundleContents.Mutable(playerStack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY));
                    BundleContents.Mutable savedBundleItemList = new BundleContents.Mutable(savedStack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY));
                    if (bundleItemList.items.isEmpty() && !savedBundleItemList.items.isEmpty()) {
                        savedBundleItemList.clearItems();
                        savedStack.set(DataComponents.BUNDLE_CONTENTS, savedBundleItemList.toImmutable());
                    }
                    for (int bundleSlot = 0; bundleSlot < bundleItemList.items.size(); bundleSlot++) {
                        ItemStack bundleItem = bundleItemList.items.get(bundleSlot);
                        ItemStack savedBundleItem = savedBundleItemList.items.get(bundleSlot);
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
                if (savedStack.getItem() instanceof BlockItem savedBlockItem && playerStack.getItem() instanceof BlockItem playerBlockItem) {
                    if (savedBlockItem.getBlock() instanceof ShulkerBoxBlock && playerBlockItem.getBlock() instanceof ShulkerBoxBlock) {
                        ComponentItemHandler shulkerContainerList = (ComponentItemHandler) playerStack.getCapability(Capabilities.ItemHandler.ITEM);
                        ComponentItemHandler savedShulkerContainerList = (ComponentItemHandler) savedStack.getCapability(Capabilities.ItemHandler.ITEM);
                        for (int shulkerSlot = 0; shulkerSlot < savedShulkerContainerList.getSlots(); shulkerSlot++) {
                            ItemStack shulkerItem = shulkerContainerList.getStackInSlot(shulkerSlot);
                            ItemStack savedShulkerItem = savedShulkerContainerList.getStackInSlot(shulkerSlot);
                            if (shulkerItem.isEmpty() && !savedShulkerItem.isEmpty()) {
                                savedShulkerContainerList.setStackInSlot(shulkerSlot, ItemStack.EMPTY);
                            }
                            if (ItemStack.isSameItem(shulkerItem, savedShulkerItem)) {
                                if (shulkerItem.getCount() > savedShulkerItem.getCount()) {
                                    shulkerItem.setCount(shulkerItem.getCount() - savedShulkerItem.getCount());
                                    serverPlayer.drop(shulkerItem, true);
                                }
                                if (shulkerItem.getCount() < savedShulkerItem.getCount()) {
                                    savedShulkerItem.setCount(shulkerItem.getCount());
                                }
                                if (shulkerItem.getDamageValue() > savedShulkerItem.getDamageValue())
                                    savedShulkerItem.setDamageValue(shulkerItem.getDamageValue());
                            } else {
                                serverPlayer.drop(shulkerItem, true);
                            }
                        }
                        playerStack.setCount(-1);
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
        if (event.getEntity() instanceof ServerPlayer serverPlayer && serverPlayer.level().getBlockEntity(serverPlayer.getRespawnPosition()).hasData(BROData.SAVED_INVENTORY.get()) && Config.COMMON.saveXP.get()) {
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

    public static SavedPlayerInventory getSavedInventory(BlockEntity blockEntity) {
        return blockEntity.getData(BROData.SAVED_INVENTORY); // This applies even to the custom RespawnAnchorBlockEntity class for compatibility with other mods
    }

    private static int findStackIndex(ItemStack itemStack, List<ItemStack> items) {
        if (itemStack.isStackable()) {
            for (int i = 0; i < items.size(); i++) {
                if (ItemStack.isSameItemSameComponents(items.get(i), itemStack)) {
                    return i;
                }
            }

        }
        return -1;
    }
}