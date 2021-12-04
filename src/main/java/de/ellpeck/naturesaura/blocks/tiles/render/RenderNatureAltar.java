package de.ellpeck.naturesaura.blocks.tiles.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import de.ellpeck.naturesaura.blocks.tiles.BlockEntityNatureAltar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.BlockEntityRenderer;
import net.minecraft.client.renderer.tileentity.BlockEntityRendererDispatcher;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.vector.Vector3f;

public class RenderNatureAltar extends BlockEntityRenderer<BlockEntityNatureAltar> {
    public RenderNatureAltar(BlockEntityRendererDispatcher rendererDispatcherIn) {
        super(rendererDispatcherIn);
    }

    @Override
    public void render(BlockEntityNatureAltar tileEntityIn, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int combinedLightIn, int combinedOverlayIn) {
        ItemStack stack = tileEntityIn.items.getStackInSlot(0);
        if (!stack.isEmpty()) {
            matrixStackIn.push();
            float time = tileEntityIn.bobTimer + partialTicks;
            float bob = (float) Math.sin(time / 10F) * 0.1F;
            matrixStackIn.translate(0.5F, 1.2F + bob, 0.5F);
            matrixStackIn.rotate(Vector3f.YP.rotationDegrees(time * 3 % 360));
            Minecraft.getInstance().getItemRenderer().renderItem(stack, ItemCameraTransforms.TransformType.GROUND, combinedLightIn, combinedOverlayIn, matrixStackIn, bufferIn);
            matrixStackIn.pop();
        }
    }
}
