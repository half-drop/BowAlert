package com.halfdrop.bowalert;

import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ThrowablePotionItem;

enum WeaponKind {
    BOW, CROSSBOW, THROWABLE, NONE;

    static WeaponKind of(ItemStack stack) {
        if (stack.isEmpty()) return NONE;
        if (stack.is(Items.BOW)) return BOW;
        if (stack.is(Items.CROSSBOW)) return CROSSBOW;
        if (stack.getItem() instanceof ThrowablePotionItem || stack.getItem() instanceof ProjectileWeaponItem
                || stack.is(Items.SNOWBALL) || stack.is(Items.EGG) || stack.is(Items.ENDER_PEARL)
                || stack.is(Items.EXPERIENCE_BOTTLE) || stack.is(Items.TRIDENT)) {
            return THROWABLE;
        }
        return NONE;
    }

    static boolean isChargedCrossbow(ItemStack stack) {
        return stack.is(Items.CROSSBOW) && CrossbowItem.isCharged(stack);
    }
}
