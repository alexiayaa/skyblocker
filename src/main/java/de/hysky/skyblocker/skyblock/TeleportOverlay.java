package de.hysky.skyblocker.skyblock;

import com.mojang.blaze3d.systems.RenderSystem;
import de.hysky.skyblocker.config.SkyblockerConfigManager;
import de.hysky.skyblocker.skyblock.item.PriceInfoTooltip;
import de.hysky.skyblocker.utils.ItemUtils;
import de.hysky.skyblocker.utils.Utils;
import de.hysky.skyblocker.utils.render.RenderHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public class TeleportOverlay {
    private static final float[] COLOR_COMPONENTS = {118f / 255f, 21f / 255f, 148f / 255f};
    private static final MinecraftClient client = MinecraftClient.getInstance();

    public static void init() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(TeleportOverlay::render);
    }

    private static void render(WorldRenderContext wrc) {
        if (Utils.isOnSkyblock() && SkyblockerConfigManager.get().general.teleportOverlay.enableTeleportOverlays && client.player != null && client.world != null) {
            ItemStack heldItem = client.player.getMainHandStack();
            String itemId = PriceInfoTooltip.getInternalNameFromNBT(heldItem, true);
            NbtCompound extraAttributes = ItemUtils.getExtraAttributes(heldItem);

            if (itemId != null) {
                switch (itemId) {
                    case "ASPECT_OF_THE_LEECH_1" -> {
                        if (SkyblockerConfigManager.get().general.teleportOverlay.enableWeirdTransmission) {
                            render(wrc, 3);
                        }
                    }
                    case "ASPECT_OF_THE_LEECH_2" -> {
                        if (SkyblockerConfigManager.get().general.teleportOverlay.enableWeirdTransmission) {
                            render(wrc, 4);
                        }
                    }
                    case "ASPECT_OF_THE_END", "ASPECT_OF_THE_VOID" -> {
                        if (SkyblockerConfigManager.get().general.teleportOverlay.enableEtherTransmission && client.options.sneakKey.isPressed() && extraAttributes != null && extraAttributes.getInt("ethermerge") == 1) {
                            render(wrc, extraAttributes, 57);
                        } else if (SkyblockerConfigManager.get().general.teleportOverlay.enableInstantTransmission) {
                            render(wrc, extraAttributes, 8);
                        }
                    }
                    case "ETHERWARP_CONDUIT" -> {
                        if (SkyblockerConfigManager.get().general.teleportOverlay.enableEtherTransmission) {
                            render(wrc, extraAttributes, 57);
                        }
                    }
                    case "SINSEEKER_SCYTHE" -> {
                        if (SkyblockerConfigManager.get().general.teleportOverlay.enableSinrecallTransmission) {
                            render(wrc, extraAttributes, 4);
                        }
                    }
                    case "NECRON_BLADE", "ASTRAEA", "HYPERION", "SCYLLA", "VALKYRIE" -> {
                        if (SkyblockerConfigManager.get().general.teleportOverlay.enableWitherImpact) {
                            render(wrc, 10);
                        }
                    }
                }
            }
        }
    }

    /**
     * Renders the teleport overlay with a given base range and the tuned transmission stat.
     */
    private static void render(WorldRenderContext wrc, NbtCompound extraAttributes, int baseRange) {
        render(wrc, extraAttributes != null && extraAttributes.contains("tuned_transmission") ? baseRange + extraAttributes.getInt("tuned_transmission") : baseRange);
    }

    /**
     * Renders the teleport overlay with a given range. Uses {@link MinecraftClient#crosshairTarget} if it is a block and within range. Otherwise, raycasts from the player with the given range.
     *
     * @implNote {@link MinecraftClient#player} and {@link MinecraftClient#world} must not be null when calling this method.
     */
    private static void render(WorldRenderContext wrc, int range) {
        if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.BLOCK && client.crosshairTarget instanceof BlockHitResult blockHitResult && client.crosshairTarget.squaredDistanceTo(client.player) < range * range) {
            render(wrc, blockHitResult);
        } else if (client.interactionManager != null && range > client.interactionManager.getReachDistance()) {
            @SuppressWarnings("DataFlowIssue")
            HitResult result = client.player.raycast(range, wrc.tickDelta(), false);
            if (result.getType() == HitResult.Type.BLOCK && result instanceof BlockHitResult blockHitResult) {
                render(wrc, blockHitResult);
            }
        }
    }

    /**
     * Renders the teleport overlay at the given {@link BlockHitResult}.
     *
     * @implNote {@link MinecraftClient#world} must not be null when calling this method.
     */
    private static void render(WorldRenderContext wrc, BlockHitResult blockHitResult) {
        BlockPos pos = blockHitResult.getBlockPos();
        @SuppressWarnings("DataFlowIssue")
        BlockState state = client.world.getBlockState(pos);
        if (!state.isAir() && client.world.getBlockState(pos.up()).isAir() && client.world.getBlockState(pos.up(2)).isAir()) {
            RenderSystem.polygonOffset(-1f, -10f);
            RenderSystem.enablePolygonOffset();

            RenderHelper.renderFilledIfVisible(wrc, pos, COLOR_COMPONENTS, 0.5f);

            RenderSystem.polygonOffset(0f, 0f);
            RenderSystem.disablePolygonOffset();
        }
    }
}
