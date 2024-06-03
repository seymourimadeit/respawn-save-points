package tallestegg.better_respawn_options;

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
        public final ModConfigSpec.ConfigValue<List<? extends String>> blockEntitiesAcceptableAsSavePoints;

        public CommonConfig(ModConfigSpec.Builder builder) {
            builder.push("save points");
            blockEntitiesAcceptableAsSavePoints = builder.defineList("Block entities acceptable as save points", List.of("minecraft:bed", "better_respawn_options:respawn_anchor_block_entity"), obj -> true);
            builder.pop();
        }
    }
}