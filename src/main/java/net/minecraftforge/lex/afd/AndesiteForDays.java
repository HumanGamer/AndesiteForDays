/*
 * Copyright (c) 2019.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.minecraftforge.lex.afd;

import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(AndesiteForDays.MODID)
public class AndesiteForDays {
    public static final String MODID = "andesitefordays";
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LogManager.getLogger();

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);

    private static final Block.Properties blockProps = Block.Properties.of(Material.GLASS).strength(3.5F).lightLevel(state -> 15);
    private static final Item.Properties  itemProps  = new Item.Properties().tab(CreativeModeTab.TAB_MISC);

    public static final RegistryObject<Block> TIER1_BLOCK = BLOCKS.register("tier_1", () -> new AndesiteGenBlock(1, blockProps));
    public static final RegistryObject<Block> TIER2_BLOCK = BLOCKS.register("tier_2", () -> new AndesiteGenBlock(2, blockProps));
    public static final RegistryObject<Block> TIER3_BLOCK = BLOCKS.register("tier_3", () -> new AndesiteGenBlock(3, blockProps));
    public static final RegistryObject<Block> TIER4_BLOCK = BLOCKS.register("tier_4", () -> new AndesiteGenBlock(4, blockProps));
    public static final RegistryObject<Block> TIER5_BLOCK = BLOCKS.register("tier_5", () -> new AndesiteGenBlock(5, blockProps));
    public static final RegistryObject<Item>  TIER1_ITEM  = ITEMS .register("tier_1", () -> new BlockItem(TIER1_BLOCK.get(), itemProps));
    public static final RegistryObject<Item>  TIER2_ITEM  = ITEMS .register("tier_2", () -> new BlockItem(TIER2_BLOCK.get(), itemProps));
    public static final RegistryObject<Item>  TIER3_ITEM  = ITEMS .register("tier_3", () -> new BlockItem(TIER3_BLOCK.get(), itemProps));
    public static final RegistryObject<Item>  TIER4_ITEM  = ITEMS .register("tier_4", () -> new BlockItem(TIER4_BLOCK.get(), itemProps));
    public static final RegistryObject<Item>  TIER5_ITEM  = ITEMS .register("tier_5", () -> new BlockItem(TIER5_BLOCK.get(), itemProps));
    public static final RegistryObject<BlockEntityType<AndesiteGenTile>> TIER1_TILE = TILES.register("tier_1", () -> BlockEntityType.Builder.of(AndesiteGenTile.createSupplier(1), TIER1_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<AndesiteGenTile>> TIER2_TILE = TILES.register("tier_2", () -> BlockEntityType.Builder.of(AndesiteGenTile.createSupplier(2), TIER2_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<AndesiteGenTile>> TIER3_TILE = TILES.register("tier_3", () -> BlockEntityType.Builder.of(AndesiteGenTile.createSupplier(3), TIER3_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<AndesiteGenTile>> TIER4_TILE = TILES.register("tier_4", () -> BlockEntityType.Builder.of(AndesiteGenTile.createSupplier(4), TIER4_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<AndesiteGenTile>> TIER5_TILE = TILES.register("tier_5", () -> BlockEntityType.Builder.of(AndesiteGenTile.createSupplier(5), TIER5_BLOCK.get()).build(null));
    private static final int PLAINS = 4159204;

    public AndesiteForDays() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        TILES.register(modBus);
        modBus.addListener(this::setup);
        modBus.addListener(this::setupClient);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            modBus.addListener(this::colorGeneratorBlockWater);
            modBus.addListener(this::colorGeneratorItemWater);
        });

        MinecraftForge.EVENT_BUS.register(this);

        //ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ForgeConfig.clientSpec);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.serverSpec);
    }

    private void setupClient(final FMLClientSetupEvent event) {}

    private void setup(final FMLCommonSetupEvent event) {}

    public void colorGeneratorBlockWater(RegisterColorHandlersEvent.Block event) {
        event.register(
            (state, env, pos, index) -> index == 1 ? env != null && pos != null ? BiomeColors.getAverageWaterColor(env, pos) : PLAINS : -1,
            BLOCKS.getEntries().stream().filter(RegistryObject::isPresent).map(RegistryObject::get).toArray(Block[]::new)
        );
    }

    public void colorGeneratorItemWater(RegisterColorHandlersEvent.Item event) {
        event.register(
            (stack, index) -> index == 1 ? PLAINS : -1,
            ITEMS.getEntries().stream().filter(RegistryObject::isPresent).map(RegistryObject::get).toArray(Item[]::new)
        );
    }
}
