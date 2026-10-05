package com.umbral;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

public final class UmbralItem extends Item {
    public UmbralItem(Properties properties) { super(properties); }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide() && entity instanceof Player player) {
            var lookup = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var sharpness = lookup.getOrThrow(Enchantments.SHARPNESS);
            if (stack.getEnchantmentLevel(sharpness) < 10) stack.enchant(sharpness, 10);
            lookup.get(net.minecraft.resources.Identifier.withDefaultNamespace("lunge")).ifPresent(e -> {
                if (stack.getEnchantmentLevel(e) < 4) stack.enchant(e, 4);
            });
        }
    }

    @Override
    public InteractionResult use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (!user.isShiftKeyDown()) return InteractionResult.PASS;
        if (user.getCooldowns().isOnCooldown(this)) return InteractionResult.FAIL;
        if (!level.isClientSide() && level instanceof ServerLevel server) {
            UmbralMod.throwUmbral(server, user, stack);
        }
        return InteractionResult.SUCCESS;
    }
}
