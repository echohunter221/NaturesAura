package de.ellpeck.naturesaura.events;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import de.ellpeck.naturesaura.Helper;
import de.ellpeck.naturesaura.ModConfig;
import de.ellpeck.naturesaura.NaturesAura;
import de.ellpeck.naturesaura.api.NaturesAuraAPI;
import de.ellpeck.naturesaura.api.aura.chunk.IAuraChunk;
import de.ellpeck.naturesaura.api.misc.ILevelData;
import de.ellpeck.naturesaura.chunk.AuraChunk;
import de.ellpeck.naturesaura.chunk.AuraChunkProvider;
import de.ellpeck.naturesaura.commands.CommandAura;
import de.ellpeck.naturesaura.gen.ModFeatures;
import de.ellpeck.naturesaura.misc.LevelData;
import de.ellpeck.naturesaura.packet.PacketHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.Player;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.level.Level;
import net.minecraft.level.chunk.Chunk;
import net.minecraft.level.chunk.IChunk;
import net.minecraft.level.gen.GenerationStage.Decoration;
import net.minecraft.level.server.ChunkHolder;
import net.minecraft.level.server.ChunkManager;
import net.minecraft.level.server.ServerChunkProvider;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BiomeLoadingEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

public class CommonEvents {

    private static final Method GET_LOADED_CHUNKS_METHOD = ObfuscationReflectionHelper.findMethod(ChunkManager.class, "func_223491_f");
    private static final ListMultimap<UUID, ChunkPos> PENDING_AURA_CHUNKS = ArrayListMultimap.create();

    @SubscribeEvent
    public void onBiomeLoad(BiomeLoadingEvent event) {
        if (ModConfig.instance.auraBlooms.get()) {
            event.getGeneration().func_242513_a(Decoration.VEGETAL_DECORATION, ModFeatures.Configured.AURA_BLOOM);
            switch (event.getCategory()) {
                case DESERT:
                    event.getGeneration().func_242513_a(Decoration.VEGETAL_DECORATION, ModFeatures.Configured.AURA_CACTUS);
                    break;
                case NETHER:
                    event.getGeneration().func_242513_a(Decoration.VEGETAL_DECORATION, ModFeatures.Configured.CRIMSON_AURA_MUSHROOM);
                    event.getGeneration().func_242513_a(Decoration.VEGETAL_DECORATION, ModFeatures.Configured.WARPED_AURA_MUSHROOM);
                    break;
                case MUSHROOM:
                    event.getGeneration().func_242513_a(Decoration.VEGETAL_DECORATION, ModFeatures.Configured.AURA_MUSHROOM);
                    break;
            }
        }
    }

    @SubscribeEvent
    public void onChunkCapsAttach(AttachCapabilitiesEvent<Chunk> event) {
        Chunk chunk = event.getObject();
        event.addCapability(new ResourceLocation(NaturesAura.MOD_ID, "aura"), new AuraChunkProvider(chunk));
    }

    @SubscribeEvent
    public void onLevelCapsAttach(AttachCapabilitiesEvent<Level> event) {
        event.addCapability(new ResourceLocation(NaturesAura.MOD_ID, "data"), new LevelData());
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        IChunk iChunk = event.getChunk();
        if (iChunk instanceof Chunk) {
            Chunk chunk = (Chunk) iChunk;
            IAuraChunk auraChunk = chunk.getCapability(NaturesAuraAPI.capAuraChunk).orElse(null);
            if (auraChunk instanceof AuraChunk) {
                LevelData data = (LevelData) ILevelData.getLevelData(chunk.getLevel());
                data.auraChunksWithSpots.remove(chunk.getPos().asLong());
            }
        }
    }

    @SubscribeEvent
    public void onItemUse(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getPlayer();
        if (player.level.isClientSide)
            return;
        ItemStack held = event.getItemStack();
        if (!held.isEmpty() && held.getItem().getRegistryName().getPath().contains("chisel")) {
            BlockState state = player.level.getBlockState(event.getPos());
            if (NaturesAuraAPI.BOTANIST_PICKAXE_CONVERSIONS.containsKey(state)) {
                LevelData data = (LevelData) ILevelData.getLevelData(player.level);
                data.addMossStone(event.getPos());
            }
        }
    }

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (!event.level.isClientSide && event.phase == TickEvent.Phase.END) {
            if (event.level.getGameTime() % 20 == 0) {
                event.level.getProfiler().startSection(NaturesAura.MOD_ID + ":onLevelTick");
                try {
                    ChunkManager manager = ((ServerChunkProvider) event.level.getChunkProvider()).chunkManager;
                    Iterable<ChunkHolder> chunks = (Iterable<ChunkHolder>) GET_LOADED_CHUNKS_METHOD.invoke(manager);
                    for (ChunkHolder holder : chunks) {
                        Chunk chunk = holder.getChunkIfComplete();
                        if (chunk == null)
                            continue;
                        AuraChunk auraChunk = (AuraChunk) chunk.getCapability(NaturesAuraAPI.capAuraChunk, null).orElse(null);
                        if (auraChunk != null)
                            auraChunk.update();
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    NaturesAura.LOGGER.fatal(e);
                }
                event.level.getProfiler().endSection();
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!event.player.level.isClientSide && event.phase == TickEvent.Phase.END) {
            if (event.player.level.getGameTime() % 10 == 0) {
                List<ChunkPos> pending = PENDING_AURA_CHUNKS.get(event.player.getUniqueID());
                pending.removeIf(p -> this.handleChunkWatchDeferred(event.player, p));
            }

            if (event.player.level.getGameTime() % 200 != 0)
                return;

            int aura = IAuraChunk.triangulateAuraInArea(event.player.level, event.player.getPosition(), 25);
            if (aura <= 0)
                Helper.addAdvancement(event.player, new ResourceLocation(NaturesAura.MOD_ID, "negative_imbalance"), "triggered_in_code");
            else if (aura >= 1500000)
                Helper.addAdvancement(event.player, new ResourceLocation(NaturesAura.MOD_ID, "positive_imbalance"), "triggered_in_code");
        }
    }

    @SubscribeEvent
    public void onChunkWatch(ChunkWatchEvent.Watch event) {
        PENDING_AURA_CHUNKS.put(event.getPlayer().getUniqueID(), event.getPos());
    }

    private boolean handleChunkWatchDeferred(Player player, ChunkPos pos) {
        Chunk chunk = Helper.getLoadedChunk(player.level, pos.x, pos.z);
        if (chunk == null)
            return false;
        AuraChunk auraChunk = (AuraChunk) chunk.getCapability(NaturesAuraAPI.capAuraChunk, null).orElse(null);
        if (auraChunk == null)
            return false;
        PacketHandler.sendTo(player, auraChunk.makePacket());
        return true;
    }

    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        CommandAura.register(event.getServer().getCommandManager().getDispatcher());
    }
}
