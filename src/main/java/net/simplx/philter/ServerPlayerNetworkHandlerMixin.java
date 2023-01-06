package net.simplx.philter;

import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayerNetworkHandlerMixin {

  private final FilterBlockEntity entity;

  public ServerPlayerNetworkHandlerMixin(FilterBlockEntity entity) {
    this.entity = entity;
  }

  @Inject(method = "onCustomPayload", at = @At("HEAD"), cancellable = true)
  public void onCustomPayload(CustomPayloadC2SPacket packet, CallbackInfo ci) {
    try {
      entity.applyChange(packet);
    } finally {
      ci.cancel();
    }
  }
}
