package com.userofbricks.oreRemover;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.ReplaceBlockConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.world.BiomeGenerationSettingsBuilder;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.userofbricks.oreRemover.Constants.LOGGER;
import static com.userofbricks.oreRemover.Constants.MODID;

@Mod(MODID)
public class OreRemover {

    public OreRemover() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::setup);

        MinecraftForge.EVENT_BUS.register(this);

        this.configSetuo();
    }

    private void configSetuo() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CommonConfig.COMMON_CONFIG);
        CommonConfig.loadConfig(CommonConfig.COMMON_CONFIG, FMLPaths.CONFIGDIR.get().resolve("geolosys-common.toml"));
    }

    private void setup(final FMLCommonSetupEvent event) {
    }

    ///OreRemoval///
    //much of this is taken and then modified from Geolosys

    private static final List<GenerationStep.Decoration> decorations = new LinkedList<>();
    static {
        decorations.add(GenerationStep.Decoration.UNDERGROUND_ORES);
        decorations.add(GenerationStep.Decoration.UNDERGROUND_DECORATION);
    }

    @SubscribeEvent
    public void onBiomesLoaded(BiomeLoadingEvent evt) {
        BiomeGenerationSettingsBuilder gen = evt.getGeneration();

        for (GenerationStep.Decoration stage : decorations) {
            List<Supplier<PlacedFeature>> feats = gen.getFeatures(stage);
            List<Supplier<PlacedFeature>> filtered = OreRemover.filterFeatures(feats);
            for (Supplier<PlacedFeature> feature : filtered) {
                feats.remove(feature);
            }
        }
    }

    private static final List<Block> toRm = CommonConfig.ORES_TO_REMOVE.get().stream().map(string -> {
        return ForgeRegistries.BLOCKS.getValue(new ResourceLocation(string));
    }).toList();

    // Validates, removes and logs each feature
    private static List<ConfiguredFeature<?, ?>> featureRemover(Block targetBlock, ConfiguredFeature<?, ?> targetFeature) {
        List<ConfiguredFeature<?, ?>> removed = new LinkedList<>();

        if (targetBlock != null) {
            if (toRm.contains(targetBlock)) {
                removed.add(targetFeature);
                if (CommonConfig.DEBUG_WORLD_GEN.get()) {
                    LOGGER.info("{} removed from worldgen", targetBlock.getRegistryName());
                }
            }
        }
        return removed;
    }

    // Filters the features before sending em to the featureRemover()
    public static List<Supplier<PlacedFeature>> filterFeatures(List<Supplier<PlacedFeature>> features) {
        List<Supplier<PlacedFeature>> removed = new LinkedList<Supplier<PlacedFeature>>();
        for (Supplier<PlacedFeature> feature : features) {
            feature.get().getFeatures().forEach((confFeat) -> {
                List<OreConfiguration.TargetBlockState> targets = null;
                if (confFeat.config instanceof OreConfiguration) {
                    targets = ((OreConfiguration) confFeat.config).targetStates;
                } else if (confFeat.config instanceof ReplaceBlockConfiguration) {
                    targets = ((ReplaceBlockConfiguration) confFeat.config).targetStates;
                }

                if (targets != null) {
                    List<Boolean> mapped = targets.parallelStream()
                            .map(t -> Boolean.valueOf(featureRemover(t.state.getBlock(), confFeat).size() > 0))
                            .collect(Collectors.toList());

                    if (mapped.contains(Boolean.valueOf(true))) {
                        removed.add(feature);
                    }
                }
            });
        }

        return removed;
    }
}
