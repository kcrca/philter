package net.simplx.philter;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class PhilterMod implements ModInitializer {

  public static final String MOD_ID = "philter";
  public static final Identifier FILTER_ID = new Identifier(MOD_ID, "filter");

  public static final Block FILTER_BLOCK =
      new FilterBlock(FabricBlockSettings.copyOf(AbstractBlock.Settings.create()).strength(4.0f));
  public static final BlockEntityType<FilterBlockEntity> FILTER_BLOCK_ENTITY =
      FabricBlockEntityTypeBuilder.create(FilterBlockEntity::new, FILTER_BLOCK).build();
  public static final ScreenHandlerType<FilterScreenHandler> FILTER_SCREEN_HANDLER =
      new ExtendedScreenHandlerType<>(FilterScreenHandler::new, FilterData.PACKET_CODEC);

  @Override
  public void onInitialize() {
    PayloadTypeRegistry.playC2S().register(FilterData.ID, FilterData.PACKET_CODEC);
    ServerPlayNetworking.registerGlobalReceiver(FilterData.ID,
        (payload, context) -> FilterBlockEntity.updateEntity(context.player(), payload));

    Registry.register(Registries.BLOCK, FILTER_ID, FILTER_BLOCK);
    Registry.register(Registries.BLOCK_ENTITY_TYPE, FILTER_ID, FILTER_BLOCK_ENTITY);

    var blockItem = Registry.register(Registries.ITEM, FILTER_ID, new BlockItem(FILTER_BLOCK, new Item.Settings()));
    Registry.register(Registries.SCREEN_HANDLER, FILTER_ID, FILTER_SCREEN_HANDLER);

    ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register(content -> content.addAfter(Items.HOPPER,
        blockItem));
  }
}
