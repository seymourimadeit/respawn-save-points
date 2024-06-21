package tallestegg.respawn_save_points;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = RespawnSavePoints.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final CommonConfig COMMON;

    static {
        final Pair<CommonConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(CommonConfig::new);
        COMMON = specPair.getLeft();
        COMMON_SPEC = specPair.getRight();
    }

    public static class CommonConfig {
        public final ForgeConfigSpec.BooleanValue saveXP;
        public final ForgeConfigSpec.BooleanValue transferDurability;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> itemBlacklist;
        public final ForgeConfigSpec.BooleanValue excludedItemsMessage;
        public final ForgeConfigSpec.BooleanValue includedItemsMessage;
        public final ForgeConfigSpec.BooleanValue itemDrops;
        public final ForgeConfigSpec.DoubleValue percentageOfItemsKept;

        public CommonConfig(ForgeConfigSpec.Builder builder) {
            builder.push("additional options");
            saveXP = builder.define("Save xp along with inventory", true);
            transferDurability = builder.define("Transfer lost durability", true);
            itemBlacklist = builder.defineList("Items that cannot be saved via beds or respawn anchors", ImmutableList.of("mekanism:cardboard_box", "minecraft:bundle"), obj -> true);
            includedItemsMessage = builder.define("Show saved items message", true);
            excludedItemsMessage = builder.define("Show excluded items message", true);
            percentageOfItemsKept = builder.defineInRange("% of items kept after dying", 1.0F, Double.MIN_VALUE, Double.MAX_VALUE);
            itemDrops = builder.define("Drop items still", true);
            builder.pop();
        }
    }
}