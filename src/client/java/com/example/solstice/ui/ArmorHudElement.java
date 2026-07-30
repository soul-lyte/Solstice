package com.example.solstice.ui;

import com.example.solstice.mixin.armor.InGameHudArmorAccessor;
import com.example.solstice.mixin.armor.PlayerScreenHandlerAccessor;
import com.example.solstice.qol.armor.ArmorHudModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.AttackIndicator;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the real worn armor pieces next to the hotbar - a faithful port of
 * uku3lig/armor-hud's own {@code MixinHud}/{@code ArmorHudMod} (MIT, see
 * NOTICE.md), not a reimplementation from scratch. Reuses vanilla's own
 * per-slot hotbar item renderer (icon, count, durability bar, pickup pop
 * animation) via {@link InGameHudArmorAccessor} - the same technique the
 * original mod itself uses - and vanilla's own hotbar background sprites
 * for the Hotbar/Rounded styles, so the art matches vanilla exactly instead
 * of a custom-drawn box.
 *
 * <p>Not a {@code HudElement} - the real mod positions itself via an
 * anchor/side/offset model relative to the hotbar or screen edges, not a
 * free-drag box, so it isn't part of Solstice's {@code HudLayoutManager}
 * drag-resize system. Driven from {@code InGameHudArmorHudMixin}'s TAIL
 * inject into {@code InGameHud.renderHotbar} - the same real hook point
 * ({@code Hud.extractItemHotbar}) the original mod uses.</p>
 */
public final class ArmorHudElement {

    private static final ArmorHudElement INSTANCE = new ArmorHudElement();

    private static final int STEP = 20;
    private static final int SIZE = 22;
    private static final int HOTBAR_OFFSET = 98;
    private static final int OFFHAND_OFFSET = 29;
    private static final int ATTACK_INDICATOR_OFFSET = 23;
    private static final int WARNING_SIZE = 8;

    private static final Identifier HOTBAR_SPRITE = Identifier.ofVanilla("hud/hotbar");
    private static final Identifier HOTBAR_OFFHAND_LEFT_SPRITE = Identifier.ofVanilla("hud/hotbar_offhand_left");
    private static final Identifier WARNING_TEXTURE = Identifier.of("solstice", "textures/gui/armor_hud_warning.png");

    private static final Random RANDOM = Random.create();

    private ArmorHudElement() {}

    public static ArmorHudElement getInstance() { return INSTANCE; }

    private record Rect(int x, int y, int width, int height) {}

    public void render(DrawContext context, RenderTickCounter tickCounter) {
        ArmorHudModule module = ArmorHudModule.getInstance();
        if (!module.isEnabled()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null) {
            return;
        }

        List<ItemStack> armorItems = getArmorItems(player);
        if (armorItems.isEmpty()) {
            return;
        }
        if (ArmorHudModule.reversed) {
            List<ItemStack> reversedItems = new ArrayList<>(armorItems);
            java.util.Collections.reverse(reversedItems);
            armorItems = reversedItems;
        }

        Rect rect = getWidgetRect(context, client, player, armorItems);
        boolean vertical = ArmorHudModule.orientation == 1;
        int textureWidth = SIZE + (armorItems.size() - 1) * STEP;
        int color = 0xFFFFFFFF;

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(rect.x(), rect.y());
        if (vertical) {
            context.getMatrices().rotate(MathHelper.HALF_PI);
            context.getMatrices().translate(0, -SIZE);
        }
        drawBackground(context, ArmorHudModule.style, armorItems.size(), textureWidth, color);
        context.getMatrices().popMatrix();

        InGameHudArmorAccessor accessor = (InGameHudArmorAccessor) client.inGameHud;
        int anchor = ArmorHudModule.anchor;
        int side = ArmorHudModule.side;
        int extrasSide = anchor == 0 ? side : (side == 0 ? 1 : 0);

        for (int i = 0; i < armorItems.size(); i++) {
            ItemStack stack = armorItems.get(i);
            int x = rect.x();
            int y = rect.y();
            if (vertical) {
                y += STEP * i;
            } else {
                x += STEP * i;
            }

            if (ArmorHudModule.iconsShown && shouldDrawEmptySlot(ArmorHudModule.widgetShown) && stack.isEmpty()) {
                int slotIndex = ArmorHudModule.reversed ? 3 - i : i;
                EquipmentSlot[] slotOrder = PlayerScreenHandlerAccessor.solstice$getEquipmentSlotOrder();
                Identifier emptyIcon = PlayerScreenHandlerAccessor.solstice$getEmptyArmorSlotTextures().get(slotOrder[slotIndex]);
                if (emptyIcon != null) {
                    context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, emptyIcon, x + 3, y + 3, 16, 16);
                }
            }

            accessor.solstice$renderHotbarItem(context, x + 3, y + 3, tickCounter, player, stack, i + 1);

            boolean anchorTop = anchor == 2 || anchor == 3;
            if (anchorTop && !vertical) {
                y += SIZE;
            } else if (extrasSide == 1 && vertical) {
                x += SIZE;
            }

            int durabilityDisplay = ArmorHudModule.durabilityDisplay;
            if (durabilityDisplay != 0 && !stack.isEmpty()) {
                String dura = switch (durabilityDisplay) {
                    case 1 -> String.valueOf(stack.getMaxDamage() - stack.getDamage());
                    case 2 -> {
                        if (stack.getDamage() == 0) yield "";
                        double percentage = 1 - (double) stack.getDamage() / stack.getMaxDamage();
                        yield (int) Math.floor(percentage * 100) + "%";
                    }
                    default -> "";
                };
                int textHeight = client.textRenderer.fontHeight;

                if (!vertical) {
                    if (!anchorTop) y -= textHeight;
                    int textWidth = client.textRenderer.getWidth(dura);
                    context.drawTextWithShadow(client.textRenderer, dura, x + (SIZE - textWidth) / 2, y, 0xFF000000 | stack.getItemBarColor());
                    if (anchorTop) y += textHeight;
                } else {
                    int textWidth = client.textRenderer.getWidth(dura) + 2;
                    int textY = (SIZE - textHeight) / 2;
                    if (extrasSide == 0) x -= textWidth;
                    context.drawTextWithShadow(client.textRenderer, dura, x + 1, y + textY, 0xFF000000 | stack.getItemBarColor());
                    if (extrasSide == 1) x += textWidth;
                }
            }

            if (ArmorHudModule.warningShown && shouldShowWarning(stack)) {
                int intensity = ArmorHudModule.warningBobIntensity;
                if (intensity != 0) {
                    y += RANDOM.nextInt(intensity) - (int) Math.ceil(intensity / 2.0);
                }

                if (!vertical) {
                    if (!anchorTop) y -= WARNING_SIZE + 2;
                    int warnX = (SIZE - WARNING_SIZE) / 2;
                    context.drawTexture(RenderPipelines.GUI_TEXTURED, WARNING_TEXTURE, x + warnX, y + 1, 0.0F, 0.0F, WARNING_SIZE, WARNING_SIZE, WARNING_SIZE, WARNING_SIZE);
                } else {
                    if (extrasSide == 0) x -= WARNING_SIZE + 2;
                    int warnY = (SIZE - WARNING_SIZE) / 2;
                    context.drawTexture(RenderPipelines.GUI_TEXTURED, WARNING_TEXTURE, x + 1, y + warnY, 0.0F, 0.0F, WARNING_SIZE, WARNING_SIZE, WARNING_SIZE, WARNING_SIZE);
                }
            }
        }
    }

    private void drawBackground(DrawContext context, int style, int itemCount, int textureWidth, int color) {
        switch (style) {
            case 0 -> { // Hotbar
                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, 182, 22, 0, 0, 0, 0, textureWidth - 3, SIZE, color);
                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, 182, 22, 182 - 3, 0, textureWidth - 3, 0, 3, SIZE, color);
            }
            case 1 -> { // Rounded Corners
                if (itemCount > 1) {
                    context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_SPRITE, 29, 24, 0, 1, 0, 0, 3, SIZE, color);
                    context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, 182, 22, 3, 0, 3, 0, textureWidth - 6, SIZE, color);
                    context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_SPRITE, 29, 24, SIZE - 3, 1, textureWidth - 3, 0, 3, SIZE, color);
                } else {
                    context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_SPRITE, 29, 24, 0, 1, 0, 0, SIZE, SIZE, color);
                }
            }
            case 2 -> { // Rounded
                if (itemCount > 1) {
                    int borderWidth = (SIZE - STEP) / 2;
                    context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_SPRITE, 29, 24, 0, 1, 0, 0, SIZE - borderWidth, SIZE, color);
                    for (int i = 1; i < itemCount - 1; i++) {
                        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_SPRITE, 29, 24, borderWidth, 1, borderWidth + i * STEP, 0, STEP, SIZE, color);
                    }
                    context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_SPRITE, 29, 24, 1, 1, textureWidth - STEP - borderWidth, 0, SIZE - borderWidth, SIZE, color);
                } else {
                    context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_SPRITE, 29, 24, 0, 1, 0, 0, SIZE, SIZE, color);
                }
            }
            default -> { /* None - nothing to draw */ }
        }
    }

    private Rect getWidgetRect(DrawContext context, MinecraftClient client, PlayerEntity player, List<ItemStack> armorItems) {
        int anchor = ArmorHudModule.anchor;
        int side = ArmorHudModule.side;
        int offhandBehavior = ArmorHudModule.offhandBehavior;
        boolean vertical = ArmorHudModule.orientation == 1;

        int sideMultiplier;
        int sideOffsetMultiplier;
        if ((anchor == 0 && side == 0) || (anchor != 0 && side == 1)) {
            sideMultiplier = -1;
            sideOffsetMultiplier = -1;
        } else {
            sideMultiplier = 1;
            sideOffsetMultiplier = 0;
        }

        Arm sideArm = side == 0 ? Arm.LEFT : Arm.RIGHT;
        int addedHotbarOffset = switch (offhandBehavior) {
            case 0 -> 0; // Always Ignore
            case 2 -> player.getMainArm() == sideArm ? ATTACK_INDICATOR_OFFSET : OFFHAND_OFFSET; // Always Leave Space
            default -> { // Adhere
                if (player.getMainArm() == sideArm) {
                    if (client.options.getAttackIndicator().getValue() == AttackIndicator.HOTBAR
                            && player.getAttackCooldownProgress(0.0F) < 1.0F) {
                        yield ATTACK_INDICATOR_OFFSET;
                    }
                } else if (!player.getOffHandStack().isEmpty()) {
                    yield OFFHAND_OFFSET;
                }
                yield 0;
            }
        };

        int textureWidth = SIZE + (armorItems.size() - 1) * STEP;
        int widgetWidth = vertical ? SIZE : textureWidth;
        int widgetHeight = vertical ? textureWidth : SIZE;

        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();

        int armorWidgetX = ArmorHudModule.offsetX * sideMultiplier + switch (anchor) {
            case 3 -> (screenWidth - widgetWidth) / 2; // Top Center
            case 1, 2 -> (widgetWidth - screenWidth) * sideOffsetMultiplier; // Bottom, Top
            default -> screenWidth / 2 + (HOTBAR_OFFSET + addedHotbarOffset) * sideMultiplier + widgetWidth * sideOffsetMultiplier; // Hotbar
        };

        int armorWidgetY = switch (anchor) {
            case 1, 0 -> screenHeight - widgetHeight - ArmorHudModule.offsetY; // Bottom, Hotbar
            default -> ArmorHudModule.offsetY; // Top, Top Center
        };

        return new Rect(armorWidgetX, armorWidgetY, widgetWidth, widgetHeight);
    }

    private static List<ItemStack> getArmorItems(PlayerEntity player) {
        EquipmentSlot[] slots = PlayerScreenHandlerAccessor.solstice$getEquipmentSlotOrder();
        List<ItemStack> items = new ArrayList<>();
        for (EquipmentSlot slot : slots) {
            items.add(player.getEquippedStack(slot));
        }

        return switch (ArmorHudModule.widgetShown) {
            case 0 -> items; // Always
            case 1 -> items.stream().allMatch(ItemStack::isEmpty) ? List.of() : items; // If Any Present
            case 3 -> items.stream().filter(ArmorHudElement::shouldShowWarning).toList(); // Damaged Pieces
            default -> items.stream().filter(s -> !s.isEmpty()).toList(); // Not Empty
        };
    }

    private static boolean shouldDrawEmptySlot(int widgetShown) {
        return widgetShown == 0 || widgetShown == 1;
    }

    private static boolean shouldShowWarning(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageable()) {
            return false;
        }
        int damage = stack.getDamage();
        int maxDamage = stack.getMaxDamage();
        double percentage = 1.0 - (double) damage / maxDamage;
        return percentage <= ArmorHudModule.minDurabilityPercentage
                || maxDamage - damage <= ArmorHudModule.minDurabilityValue;
    }
}
