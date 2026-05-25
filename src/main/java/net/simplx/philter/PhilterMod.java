package net.simplx.philter;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class PhilterMod implements ModInitializer {

  public static final String MOD_ID = "philter";
  public static final Identifier FILTER_ID = Identifier.fromNamespaceAndPath(MOD_ID, "filter");
  public static final ResourceKey<Block> FILTER_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, FILTER_ID);

  public static final Block FILTER_BLOCK = new FilterBlock(
      BlockBehaviour.Properties.of().setId(FILTER_BLOCK_KEY).strength(4.0f));
  public static final BlockEntityType<FilterBlockEntity> FILTER_BLOCK_ENTITY =
      Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, FILTER_ID,
          FabricBlockEntityTypeBuilder.create(FilterBlockEntity::new, FILTER_BLOCK).build());
  public static final MenuType<FilterScreenHandler> FILTER_SCREEN_HANDLER =
      new ExtendedMenuType<>(FilterScreenHandler::new, FilterData.STREAM_CODEC);

  @Override
  public void onInitialize() {
    Registry.register(BuiltInRegistries.BLOCK, FILTER_ID, FILTER_BLOCK);

    var blockItem = Registry.register(BuiltInRegistries.ITEM, FILTER_ID,
        new BlockItem(FILTER_BLOCK, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, FILTER_ID))));
    Registry.register(BuiltInRegistries.MENU, FILTER_ID, FILTER_SCREEN_HANDLER);

    // Add to redstone creative tab after hopper
    net.minecraft.resources.ResourceKey<net.minecraft.world.item.CreativeModeTab> redstoneTab =
        net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath("minecraft", "redstone_blocks"));
    net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(redstoneTab)
        .register(output -> output.insertAfter(Items.HOPPER, blockItem));
  }
}
