package net.simplx.philter;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

public class PhilterMod implements ModInitializer {

  public static final String MOD_ID = "philter";
  public static final Identifier FILTER_ID = id("filter");

  @SuppressWarnings("SameParameterValue")
  @NotNull
  private static Identifier id(String path) {
    return new Identifier(MOD_ID, path);
  }

  public static final Block FILTER_BLOCK = new FilterBlock(
      FabricBlockSettings.of(Material.METAL).strength(4.0f));
  public static final BlockEntityType<FilterBlockEntity> FILTER_BLOCK_ENTITY = Registry.register(
      Registries.BLOCK_ENTITY_TYPE, FILTER_ID,
      FabricBlockEntityTypeBuilder.create(FilterBlockEntity::new, FILTER_BLOCK).build());
  @SuppressWarnings("deprecation")
  public static final ScreenHandlerType<FilterScreenHandler> FILTER_SCREEN_HANDLER = ScreenHandlerRegistry.registerExtended(
      FILTER_ID, FilterScreenHandler::new);

  @Override
  public void onInitialize() {
    Registry.register(Registries.BLOCK, FILTER_ID, FILTER_BLOCK);
    Registry.register(Registries.ITEM, FILTER_ID,
        new BlockItem(FILTER_BLOCK, new FabricItemSettings()));
    ServerPlayNetworking.registerGlobalReceiver(FILTER_ID,
        (server, player, handler, buf, responseSender) -> server.execute(
            () -> FilterBlockEntity.updateEntity(player, buf)));
  }

}
