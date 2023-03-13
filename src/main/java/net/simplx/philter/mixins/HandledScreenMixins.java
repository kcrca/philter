package net.simplx.philter.mixins;

import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixins extends Screen {

  protected HandledScreenMixins(Text title) {
    super(title);
  }

  @Inject(method = "keyPressed(III)Z", at = @At("HEAD"), cancellable = true)
  public void keyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
    if (keyCode == 256) {
      // Just in case...
      if (client != null && client.player != null) {
        client.player.closeHandledScreen();
      }
    }

    Element focused = getFocused();
    if (focused instanceof TextFieldWidget textField) {
      if (!textField.keyPressed(keyCode, scanCode, modifiers) && !textField.isActive()) {
        cir.setReturnValue(super.keyPressed(keyCode, scanCode, modifiers));
      }
      cir.setReturnValue(true);
    }
  }
}
