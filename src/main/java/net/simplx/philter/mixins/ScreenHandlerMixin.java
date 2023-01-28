package net.simplx.philter.mixins;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

@Mixin(ScreenHandler.class)
public class ScreenHandlerMixin {
  @Inject(at = @At("RETURN"), method = "internalOnSlotClick(IILnet/minecraft/screen/slot/SlotActionType;Lnet/minecraft/entity/player/PlayerEntity;)V")
  private void after(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo info) {
    System.out.println(info.getId());
  }
  @Inject(at = @At("HEAD"), method = "updateSlotStacks(ILjava/util/List;Lnet/minecraft/item/ItemStack;)V")
  private void before(int revision, List<ItemStack> stacks, ItemStack cursorStack, CallbackInfo info) {
    System.out.println(info.getId());
  }
}
