package de.ellpeck.naturesaura.items;

import de.ellpeck.naturesaura.api.NaturesAuraAPI;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.player.Player;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.InteractionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.level.LightType;
import net.minecraft.level.Level;

public class ItemCaveFinder extends ItemImpl {
    public ItemCaveFinder() {
        super("cave_finder", new Properties().maxStackSize(1));
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(Level levelIn, Player playerIn, Hand handIn) {
        ItemStack stack = playerIn.getHeldItem(handIn);
        NaturesAuraAPI.IInternalHooks inst = NaturesAuraAPI.instance();
        if (!inst.extractAuraFromPlayer(playerIn, 20000, levelIn.isClientSide))
            return new ActionResult<>(InteractionResult.FAIL, stack);
        if (levelIn.isClientSide) {
            inst.setParticleDepth(false);
            inst.setParticleSpawnRange(64);
            inst.setParticleCulling(false);
            BlockPos pos = playerIn.getPosition();
            int range = 30;
            for (int x = -range; x <= range; x++)
                for (int y = -range; y <= range; y++)
                    for (int z = -range; z <= range; z++) {
                        BlockPos offset = pos.add(x, y, z);
                        BlockState state = levelIn.getBlockState(offset);
                        try {
                            if (!state.getBlock().canCreatureSpawn(state, levelIn, offset, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, null))
                                continue;
                        } catch (Exception e) {
                            continue;
                        }

                        BlockPos offUp = offset.up();
                        BlockState stateUp = levelIn.getBlockState(offUp);
                        if (stateUp.isNormalCube(levelIn, offUp) || stateUp.getMaterial().isLiquid())
                            continue;

                        int sky = levelIn.getLightFor(LightType.SKY, offUp);
                        int block = levelIn.getLightFor(LightType.BLOCK, offUp);
                        if (sky > 7 || block > 7)
                            continue;

                        inst.spawnMagicParticle(
                                offset.getX() + 0.5F, offset.getY() + 1.5F, offset.getZ() + 0.5F,
                                0F, 0F, 0F, 0x992101, 2.5F, 20 * 30, 0F, false, true);
                    }
            inst.setParticleDepth(true);
            inst.setParticleSpawnRange(32);
            inst.setParticleCulling(true);

            playerIn.swingArm(handIn);
        }
        playerIn.getCooldownTracker().setCooldown(this, 20 * 30);
        return new ActionResult<>(InteractionResult.SUCCESS, stack);
    }
}
