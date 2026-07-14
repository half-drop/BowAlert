package com.halfdrop.bowalert;

import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class ThreatTracker {
    private static final double SCAN_RANGE = 50.0D;
    private static volatile ThreatSnapshot snapshot = ThreatSnapshot.EMPTY;

    private ThreatTracker() { }

    static ThreatSnapshot snapshot() {
        return snapshot;
    }

    static void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.isPaused()) {
            snapshot = ThreatSnapshot.EMPTY;
            return;
        }

        AABB scanBox = player.getBoundingBox().inflate(SCAN_RANGE);
        List<Threat> threats = client.level.getEntitiesOfClass(LivingEntity.class, scanBox,
                entity -> entity != player && entity.isAlive() && isThreatening(entity, player))
                .stream().map(ThreatTracker::toThreat).filter(threat -> threat.kind() != WeaponKind.NONE).toList();

        Threat primary = threats.stream().max(Comparator
                .comparingInt(Threat::priority)
                .thenComparingDouble(threat -> -threat.bowPull())
                .thenComparingDouble(threat -> -threat.distance()))
                .orElse(null);
        snapshot = primary == null ? ThreatSnapshot.EMPTY : new ThreatSnapshot(primary, threats.size());
    }

    private static boolean isThreatening(LivingEntity entity, LocalPlayer player) {
        ItemStack held = relevantHeldItem(entity);
        if (WeaponKind.of(held) == WeaponKind.NONE) return false;

        Vec3 source = entity.getEyePosition();
        double distance = source.distanceTo(player.getEyePosition());
        if (distance > SCAN_RANGE) return false;

        // A broad, distance-adaptive target zone: close threats are forgiving without
        // treating a fixed five-block cube as the player at every distance.
        double allowance = 1.5D + Math.min(1.0D, Math.max(0.0D, (distance - 3.0D) / 47.0D)) * 3.5D;
        AABB target = player.getBoundingBox().inflate(allowance);
        Vec3 end = source.add(entity.getViewVector(1.0F).normalize().scale(distance + allowance + 2.0D));
        return target.clip(source, end).isPresent();
    }

    private static Threat toThreat(LivingEntity entity) {
        ItemStack stack = relevantHeldItem(entity).copy();
        WeaponKind kind = WeaponKind.of(stack);
        float bowPull = kind == WeaponKind.BOW && entity.isUsingItem() ? Math.min(1.0F, entity.getTicksUsingItem() / 20.0F) : 0.0F;
        boolean immediate = (kind == WeaponKind.BOW && bowPull >= 1.0F)
                || (kind == WeaponKind.CROSSBOW && WeaponKind.isChargedCrossbow(stack))
                || kind == WeaponKind.THROWABLE;
        return new Threat(stack, kind, bowPull, immediate, entity.distanceTo(Minecraft.getInstance().player));
    }

    private static ItemStack relevantHeldItem(LivingEntity entity) {
        ItemStack main = entity.getMainHandItem();
        return WeaponKind.of(main) != WeaponKind.NONE ? main : entity.getOffhandItem();
    }
}
