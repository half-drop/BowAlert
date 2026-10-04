package com.halfdrop.bowalert;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

final class BowAlertHud {
    private static final int ICON_SIZE = 16;

    private BowAlertHud() { }

    static void render(GuiGraphicsExtractor graphics, Object ignored) {
        ThreatSnapshot snapshot = ThreatTracker.snapshot();
        if (snapshot.primary() == null) return;

        Minecraft minecraft = Minecraft.getInstance();
        int x = graphics.guiWidth() / 2 - ICON_SIZE / 2;
        int y = graphics.guiHeight() / 2 - 30 - ICON_SIZE;
        Threat threat = snapshot.primary();
        if (threat.kind() == WeaponKind.BOW) renderBowStage(graphics, x, y, threat.bowPull());
        else graphics.item(threat.stack(), x, y);

        Font font = minecraft.font;
        if (threat.immediate()) graphics.text(font, "!", x + 11, y - 4, 0xFFFF3030, true);
        if (snapshot.count() > 1) graphics.text(font, "×" + snapshot.count(), x + 11, y + 10, 0xFFFFFFFF, true);
    }

    private static void renderBowStage(GuiGraphicsExtractor graphics, int x, int y, float pull) {
        String texture = pull >= 0.85F ? "bow_pulling_2" : pull >= 0.55F ? "bow_pulling_1" : pull > 0.0F ? "bow_pulling_0" : "bow";
        Identifier id = Identifier.withDefaultNamespace("textures/item/" + texture + ".png");
        graphics.blit(RenderPipelines.GUI_TEXTURED, id, x, y, 0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
    }
}
