package de.ellpeck.naturesaura.data;

import de.ellpeck.naturesaura.NaturesAura;
import de.ellpeck.naturesaura.blocks.ModBlocks;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.BlockTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.Tags;

public class BlockTagProvider extends BlockTagsProvider {

    public static final Tags.IOptionalNamedTag<Block> NETHER_ALTAR_WOOD = BlockTags.createOptional(new ResourceLocation(NaturesAura.MOD_ID, "nether_altar_wood"));

    public BlockTagProvider(DataGenerator generatorIn) {
        super(generatorIn);
    }

    @Override
    protected void addTags() {
        this.tag(BlockTags.LOGS).add(ModBlocks.ANCIENT_LOG, ModBlocks.ANCIENT_BARK);
        this.tag(BlockTags.PLANKS).add(ModBlocks.ANCIENT_PLANKS);
        this.tag(BlockTags.STAIRS).add(ModBlocks.ANCIENT_STAIRS, ModBlocks.INFUSED_BRICK_STAIRS, ModBlocks.INFUSED_STAIRS);
        this.tag(BlockTags.LEAVES).add(ModBlocks.GOLDEN_LEAVES, ModBlocks.ANCIENT_LEAVES, ModBlocks.DECAYED_LEAVES);
        this.tag(BlockTags.RAILS).add(ModBlocks.DIMENSION_RAIL_END, ModBlocks.DIMENSION_RAIL_NETHER, ModBlocks.DIMENSION_RAIL_OVERWORLD);
        this.tag(BlockTags.SLABS).add(ModBlocks.ANCIENT_SLAB, ModBlocks.INFUSED_SLAB, ModBlocks.INFUSED_BRICK_SLAB);
        this.tag(Tags.Blocks.DIRT).add(ModBlocks.NETHER_GRASS);
        this.tag(BlockTags.SMALL_FLOWERS).add(ModBlocks.END_FLOWER, ModBlocks.AURA_BLOOM);
        this.tag(NETHER_ALTAR_WOOD).add(Blocks.CRIMSON_PLANKS, Blocks.WARPED_PLANKS);
    }
}
