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

@Environment(EnvType.CLIENT)
public class PhilterClient implements ClientModInitializer {

  @Override
  public void onInitializeClient() {
    Block filterBlock = new Block(FabricBlockSettings.of(Material.METAL).strength(4.0f));
    Registry.register(Registries.BLOCK, FILTER_ID, filterBlock);
    Registry.register(Registries.ITEM, FILTER_ID,
        new BlockItem(filterBlock, new FabricItemSettings()));
  }
}
