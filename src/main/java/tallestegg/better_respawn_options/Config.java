package tallestegg.better_respawn_options;

import com.google.common.collect.ImmutableList;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class Config {
    public static final ModConfigSpec COMMON_SPEC;
    public static final CommonConfig COMMON;

    static {
        final Pair<CommonConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(CommonConfig::new);
        COMMON = specPair.getLeft();
        COMMON_SPEC = specPair.getRight();
    }

    public static class CommonConfig {
        public final ModConfigSpec.BooleanValue saveXP;
        public final ModConfigSpec.BooleanValue transferDurability;
        public final ModConfigSpec.ConfigValue<List<? extends String>> itemBlacklist;
        public final ModConfigSpec.BooleanValue excludedItemsMessage;
        public final ModConfigSpec.BooleanValue includedItemsMessage;
        public final ModConfigSpec.DoubleValue percentageOfItemsKept;


        public CommonConfig(ModConfigSpec.Builder builder) {
            builder.push("additional options");
            saveXP = builder.define("Save xp along with inventory", true);
            transferDurability = builder.define("Transfer lost durability", true);
            itemBlacklist = builder.defineList("Items that cannot be saved via beds or respawn anchors", ImmutableList.of("mekanism:cardboard_box"), obj -> true);
            includedItemsMessage = builder.define("Show saved items message", true);
            excludedItemsMessage = builder.define("Show excluded items message", true);
            percentageOfItemsKept = builder.defineInRange("% of items kept after dying", 1.0F, Double.MIN_VALUE, Double.MAX_VALUE);
            builder.pop();
        }
    }
}