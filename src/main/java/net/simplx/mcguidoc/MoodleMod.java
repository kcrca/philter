package net.simplx.mcguidoc;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.inventory.MenuType;

public class MoodleMod implements ModInitializer {
  public record DummyData(Integer dummy) {
    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, DummyData> STREAM_CODEC =
        StreamCodec.composite(ByteBufCodecs.VAR_INT, DummyData::dummy, DummyData::new);
  }

  public static final Identifier DOC_ID = Identifier.fromNamespaceAndPath("minecraft", "grass_block");
  public static final Block DOC_BLOCK = new DocBlock(BlockBehaviour.Properties.of().strength(4));
  public static final BlockItem DOC_BLOCK_ITEM = Registry.register(BuiltInRegistries.ITEM, DOC_ID,
      new BlockItem(DOC_BLOCK, new Item.Properties()));

  public static final MenuType<DocScreenHandler> DOC_SCREEN_HANDLER =
      new ExtendedMenuType<>(DocScreenHandler::new, DummyData.STREAM_CODEC);
  public static final BlockEntityType<DocBlockEntity> DOC_BLOCK_ENTITY = Registry.register(
      BuiltInRegistries.BLOCK_ENTITY_TYPE, DOC_ID,
      FabricBlockEntityTypeBuilder.create(DocBlockEntity::new, DOC_BLOCK).build());

  @Override
  public void onInitialize() {

  }
}
