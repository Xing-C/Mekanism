package mekanism.client.render.tileentity;

import mekanism.client.model.ModelChemicalDissolutionChamber;
import mekanism.common.tile.TileEntityChemicalDissolutionChamber;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderChemicalDissolutionChamber extends TileEntitySpecialRenderer<TileEntityChemicalDissolutionChamber> {

    private ModelChemicalDissolutionChamber model = new ModelChemicalDissolutionChamber();

    @Override
    public void render(TileEntityChemicalDissolutionChamber tileEntity, double x, double y, double z, float partialTick, int destroyStage, float alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x + 0.5F, (float) y + 1.5F, (float) z + 0.5F);
        bindTexture(MekanismUtils.getResource(ResourceType.RENDER, "ChemicalDissolutionChamber.png"));

        switch (tileEntity.facing.ordinal()) /*TODO: switch the enum*/ {
            case 2:
                GlStateManager.rotate(0, 0.0F, 1.0F, 0.0F);
                break;
            case 3:
                GlStateManager.rotate(180, 0.0F, 1.0F, 0.0F);
                break;
            case 4:
                GlStateManager.rotate(90, 0.0F, 1.0F, 0.0F);
                break;
            case 5:
                GlStateManager.rotate(270, 0.0F, 1.0F, 0.0F);
                break;
        }

        GlStateManager.rotate(180F, 0.0F, 0.0F, 1.0F);
        model.render(0.0625F);
        GlStateManager.popMatrix();
    }
}