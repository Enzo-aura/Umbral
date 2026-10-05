package com.umbral.mixin;

import com.umbral.UmbralMod;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin {
    private boolean umbralHeld() {
        Player player = (Player)(Object)this;
        return UmbralMod.isUmbral(player.getMainHandItem()) || UmbralMod.isUmbral(player.getOffhandItem());
    }

    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void umbral$noFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        if (umbralHeld()) cir.setReturnValue(false);
    }

    @Inject(method = "causeFoodExhaustion", at = @At("HEAD"), cancellable = true)
    private void umbral$noHunger(float exhaustion, CallbackInfo ci) {
        if (umbralHeld()) cir.cancel();
    }
}
