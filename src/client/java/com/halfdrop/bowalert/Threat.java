package com.halfdrop.bowalert;

import net.minecraft.world.item.ItemStack;

record Threat(ItemStack stack, WeaponKind kind, float bowPull, boolean immediate, double distance) {
    int priority() {
        if (immediate) return 3;
        if (kind == WeaponKind.BOW && bowPull > 0.0F) return 2;
        return 1;
    }
}
