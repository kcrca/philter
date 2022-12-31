package net.simplx.philter;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class Philter implements ModInitializer {

  public static final Block FILTER_BLOCK = new FilterBlock(
      FabricBlockSettings.of(Material.METAL).strength(4.0f));
  public static final BlockEntityType<FilterBlockEntity> FILTER_BLOCK_ENTITY = Registry.register(
      Registries.BLOCK_ENTITY_TYPE, new Identifier("philter", "filter"),
      FabricBlockEntityTypeBuilder.create(FilterBlockEntity::new, FILTER_BLOCK).build()
  );

  @Override
  public void onInitialize() {
    Registry.register(Registries.BLOCK, new Identifier("philter", "filter"), FILTER_BLOCK);
    Registry.register(Registries.ITEM, new Identifier("philter", "filter"),
        new BlockItem(FILTER_BLOCK, new FabricItemSettings()));

  }
}
