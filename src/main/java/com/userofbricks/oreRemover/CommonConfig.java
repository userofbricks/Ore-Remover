package com.userofbricks.oreRemover;

import ca.weblite.objc.Proxy;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.google.common.collect.Lists;
import net.minecraftforge.common.ForgeConfigSpec;

import java.nio.file.Path;
import java.util.List;

public class CommonConfig {
    public static final ForgeConfigSpec COMMON_CONFIG;
    private static final ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> ORES_TO_REMOVE;
    public static ForgeConfigSpec.BooleanValue DEBUG_WORLD_GEN;

    private static List<String> defaultRemovedOres = Lists.newArrayList("minecraft:iron_ore", "minecraft:coal_ore");

    static {
        init();
        COMMON_CONFIG = COMMON_BUILDER.build();
    }

    public static void loadConfig(ForgeConfigSpec spec, Path path) {
        final CommentedFileConfig configData = CommentedFileConfig.builder(path).sync().autosave()
                .writingMode(WritingMode.REPLACE).build();

        configData.load();
        spec.setConfig(configData);
    }

    private static void init() {
        ORES_TO_REMOVE = COMMON_BUILDER.comment(
                        "These are the ore blocks you wish to remove from ore gen\n"
                                + "Format: Comma-delimited set of <modid:block> (see default for example)")
                .defineList("oresToRemove",
                        defaultRemovedOres,
                        rawName -> rawName instanceof String);
        DEBUG_WORLD_GEN = COMMON_BUILDER.comment("Output info into the logs when Removing ores")
                .define("debugWorldgen", false);
    }
}
