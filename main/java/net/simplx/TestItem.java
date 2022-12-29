package net.simplx;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class TestItem implements ModInitializer {

  public static final Item CUSTOM_ITEM = new Item(new FabricItemSettings());

  @Override
  public void onInitialize() {
    Registry.register(Registries.ITEM, new Identifier("tutorial", "custom_item"), CUSTOM_ITEM);
  }
}
