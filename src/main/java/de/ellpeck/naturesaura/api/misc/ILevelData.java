package de.ellpeck.naturesaura.api.misc;

import de.ellpeck.naturesaura.api.NaturesAuraAPI;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.IItemHandlerModifiable;

public interface ILevelData extends ICapabilityProvider, INBTSerializable<CompoundTag> {

    static ILevelData getLevelData(Level level) {
        return level.getCapability(NaturesAuraAPI.capLevelData, null).orElse(null);
    }

    static ILevelData getOverworldData(Level level) {
        if (!level.isClientSide)
            return getLevelData(level.getServer().getLevel(Level.OVERWORLD));
        return getLevelData(level);
    }

    IItemHandlerModifiable getEnderStorage(String name);

    boolean isEnderStorageLocked(String name);
}