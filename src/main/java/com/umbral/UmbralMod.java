package com.umbral;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevel;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.Registry;

import java.util.UUID;

public final class UmbralMod implements ModInitializer {
    public static final String MOD_ID = "umbral";
    public static final ResourceKey<Item> UMBRAL_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "umbral"));
    public static final UmbralItem UMBRAL = (UmbralItem) Registry.register(
            BuiltInRegistries.ITEM, UMBRAL_KEY,
            new UmbralItem(new Item.Properties().setId(UMBRAL_KEY).stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC))
    );

    private static final int COOLDOWN_TICKS = 60;
    private static final double SPEED = 1.65D;
    private static final double MAX_DISTANCE = 48.0D;
    private static final String PROJECTILE_TAG = "umbral_projectile";
    private static final String OWNER_PREFIX = "umbral_owner:";

    @Override
    public void onInitialize() {
        ItemGroupEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(tab -> tab.accept(makeStack(null)));
        ServerTickEvents.END_WORLD_TICK.register(UmbralMod::tickProjectiles);
    }

    public static ItemStack makeStack(Level level) {
        ItemStack stack = new ItemStack(UMBRAL);
        if (level != null) {
            var lookup = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var sharpness = lookup.getOrThrow(Enchantments.SHARPNESS);
            var lunge = lookup.get(Identifier.withDefaultNamespace("lunge"));
            stack.enchant(sharpness, 10);
            lunge.ifPresent(e -> stack.enchant(e, 4));
        }
        return stack;
    }

    public static void throwUmbral(ServerLevel level, Player player, ItemStack held) {
        Vec3 start = player.getEyePosition().add(player.getViewVector(1.0F).scale(0.55D));
        Vec3 direction = player.getViewVector(1.0F).normalize();
        ItemStack projectileStack = held.copyWithCount(1);
        ItemEntity projectile = new ItemEntity(level, start.x, start.y, start.z, projectileStack);
        projectile.setNoGravity(true);
        projectile.setPickUpDelay(Integer.MAX_VALUE);
        projectile.addTag(PROJECTILE_TAG);
        projectile.addTag(OWNER_PREFIX + player.getUUID());
        projectile.setDeltaMovement(direction.scale(SPEED));
        level.addFreshEntity(projectile);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, player.getSoundSource(), 0.7F, 1.25F);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, start.x, start.y, start.z, 18, 0.18, 0.18, 0.18, 0.12);
        player.getCooldowns().add(UMBRAL, COOLDOWN_TICKS);
    }

    private static void tickProjectiles(ServerLevel level) {
        for (ItemEntity projectile : level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(
                level.getWorldBorder().getMinX(), level.getMinBuildHeight(), level.getWorldBorder().getMinZ(),
                level.getWorldBorder().getMaxX(), level.getMaxBuildHeight(), level.getWorldBorder().getMaxZ()),
                e -> e.getTags().contains(PROJECTILE_TAG))) {
            UUID ownerId = null;
            for (String tag : projectile.getTags()) {
                if (tag.startsWith(OWNER_PREFIX)) {
                    try { ownerId = UUID.fromString(tag.substring(OWNER_PREFIX.length())); } catch (IllegalArgumentException ignored) {}
                    break;
                }
            }
            if (ownerId == null) { projectile.discard(); continue; }
            Player owner = level.getPlayerByUUID(ownerId);
            if (owner == null || !owner.isAlive()) { projectile.discard(); continue; }

            Vec3 from = projectile.position();
            Vec3 velocity = projectile.getDeltaMovement();
            Vec3 to = from.add(velocity);
            BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, projectile));
            boolean hitBlock = hit.getType() != HitResult.Type.MISS;
            double travelled = from.distanceTo(owner.getEyePosition());

            if (hitBlock) {
                Vec3 target = hit.getLocation().subtract(velocity.normalize().scale(0.15D));
                teleport(owner, level, target);
                projectile.discard();
                continue;
            }

            projectile.setPos(to.x, to.y, to.z);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, to.x, to.y, to.z, 2, 0.05, 0.05, 0.05, 0.02);
            if (travelled > MAX_DISTANCE) {
                teleport(owner, level, to);
                projectile.discard();
            }
        }
    }

    private static void teleport(Player player, ServerLevel level, Vec3 target) {
        Vec3 safe = findSafeTarget(level, target);
        player.teleportTo(safe.x, safe.y, safe.z);
        player.resetFallDistance();
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, player.getSoundSource(), 1.0F, 0.85F);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, safe.x, safe.y + 0.8D, safe.z, 40, 0.45, 0.7, 0.45, 0.16);
        level.sendParticles(ParticleTypes.PORTAL, safe.x, safe.y + 0.8D, safe.z, 25, 0.35, 0.6, 0.35, 0.1);
    }

    private static Vec3 findSafeTarget(ServerLevel level, Vec3 target) {
        for (int dy = 0; dy <= 2; dy++) {
            Vec3 candidate = target.add(0, dy, 0);
            net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(candidate.x - 0.3, candidate.y, candidate.z - 0.3, candidate.x + 0.3, candidate.y + 1.8, candidate.z + 0.3);
            if (level.noCollision(box)) return candidate;
        }
        return target.add(0, 1, 0);
    }

    public static boolean isUmbral(ItemStack stack) { return stack.is(UMBRAL); }
}
