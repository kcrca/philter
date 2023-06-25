package net.simplx.mcguidoc;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class MoodleMod implements ModInitializer {

  public static final Identifier DOC_ID = new Identifier("minecraft", "grass_block");
  public static final Block DOC_BLOCK = new DocBlock(FabricBlockSettings.create().strength(4));
  public static final BlockItem DOC_BLOCK_ITEM = Registry.register(Registries.ITEM, DOC_ID, new BlockItem(DOC_BLOCK,
      new Item.Settings()));

  public static final ScreenHandlerType<DocScreenHandler> DOC_SCREEN_HANDLER =
      ScreenHandlerRegistry.registerSimple(DOC_ID, DocScreenHandler::new);
  public static final BlockEntityType<?> DOC_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, DOC_ID,
      BlockEntityType.Builder.create(DocBlockEntity::new, DOC_BLOCK).build(null));

  @Override
  public void onInitialize() {

  }
}
