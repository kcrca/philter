package net.simplx.philter.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.simplx.philter.FilterBlock;

@Environment(EnvType.CLIENT)
public class PhilterClient implements ClientModInitializer {

  public static final Block FILTER_BLOCK = new FilterBlock(
      FabricBlockSettings.of(Material.METAL).strength(4.0f));

  @Override
  public void onInitializeClient() {
    Registry.register(Registries.BLOCK, new Identifier("philter", "filter"), FILTER_BLOCK);
    Registry.register(Registries.ITEM, new Identifier("philter", "filter"),
        new BlockItem(FILTER_BLOCK, new FabricItemSettings()));
  }
}
