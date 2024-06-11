package tallestegg.respawn_save_points;

import com.google.common.collect.ImmutableList;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.tuple.Pair;

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
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> itemBlacklist;

        public CommonConfig(ForgeConfigSpec.Builder builder) {
            builder.push("additional options");
            saveXP = builder.define("Save xp along with inventory", true);
            itemBlacklist = builder.defineList("Items that cannot be saved via beds or respawn anchors", ImmutableList.of("mekanism:cardboard_box"), obj -> true);
            builder.pop();
        }
    }
}