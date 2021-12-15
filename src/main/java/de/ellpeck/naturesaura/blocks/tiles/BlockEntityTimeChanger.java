package de.ellpeck.naturesaura.blocks.tiles;

import de.ellpeck.naturesaura.Helper;
import de.ellpeck.naturesaura.api.NaturesAuraAPI;
import de.ellpeck.naturesaura.api.aura.chunk.IAuraChunk;
import de.ellpeck.naturesaura.api.aura.type.IAuraType;
import de.ellpeck.naturesaura.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class BlockEntityTimeChanger extends BlockEntityImpl implements ITickableBlockEntity {

    private long goalTime;

    public BlockEntityTimeChanger(BlockPos pos, BlockState state) {
        super(ModTileEntities.TIME_CHANGER, pos, state);
    }

    @Override
    public void tick() {
        if (!this.level.isClientSide) {
            List<ItemFrame> frames = Helper.getAttachedItemFrames(this.level, this.worldPosition);
            for (ItemFrame frame : frames) {
                ItemStack frameStack = frame.getItem();
                if (frameStack.isEmpty() || frameStack.getItem() != ModItems.CLOCK_HAND)
                    continue;

                if (this.goalTime > 0) {
                    long current = this.level.getDayTime();
                    long toAdd = Math.min(75, this.goalTime - current);
                    if (toAdd <= 0) {
                        this.goalTime = 0;
                        this.sendToClients();
                        return;
                    }
                    ((ServerLevelData) this.level.getLevelData()).setDayTime(current + toAdd);

                    BlockPos spot = IAuraChunk.getHighestSpot(this.level, this.worldPosition, 35, this.worldPosition);
                    IAuraChunk.getAuraChunk(this.level, spot).drainAura(spot, (int) toAdd * 20);

                    if (this.level instanceof ServerLevel) {
                        PlayerList list = this.level.getServer().getPlayerList();
                        list.broadcastAll(new ClientboundSetTimePacket(
                                this.level.getGameTime(), this.level.getDayTime(),
                                this.level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)));
                    }
                    return;
                }

                if (this.level.getGameTime() % 20 != 0)
                    return;

                List<ItemEntity> items = this.level.getEntitiesOfClass(ItemEntity.class, new AABB(this.worldPosition).inflate(1), Entity::isAlive);
                for (ItemEntity item : items) {
                    if (item.hasPickUpDelay())
                        continue;
                    ItemStack stack = item.getItem();
                    if (stack.isEmpty() || stack.getItem() != Items.CLOCK)
                        continue;

                    int dayGoal = Mth.floor((frame.getRotation() / 8F) * 24000F) + 18000;
                    long current = this.level.getDayTime();
                    long toMove = (24000 - current % 24000 + dayGoal) % 24000;
                    this.goalTime = current + toMove;
                    this.sendToClients();

                    if (stack.getCount() <= 1) {
                        item.kill();
                    } else {
                        stack.shrink(1);
                        item.setItem(stack);
                    }
                    return;
                }
            }
            if (this.goalTime > 0) {
                this.goalTime = 0;
                this.sendToClients();
            }
        } else if (this.goalTime > 0 && this.level.random.nextFloat() >= 0.25F) {
            double angle = Math.toRadians(this.level.getDayTime() * 5F % 360);
            double x = this.worldPosition.getX() + 0.5 + Math.sin(angle) * 3F;
            double z = this.worldPosition.getZ() + 0.5 + Math.cos(angle) * 3F;
            int color = this.goalTime % 24000 > 12000 ? 0xe2e2e2 : 0xffe926;
            NaturesAuraAPI.instance().spawnMagicParticle(
                    x, this.worldPosition.getY() + 0.1F, z,
                    0F, 0.12F, 0F,
                    color, 1F + this.level.random.nextFloat() * 2F,
                    this.level.random.nextInt(100) + 100, 0, false, true);
            NaturesAuraAPI.instance().spawnMagicParticle(
                    x, this.worldPosition.getY() + 0.1F, z,
                    0F, 0F, 0F,
                    IAuraType.forLevel(this.level).getColor(), 1F + this.level.random.nextFloat(),
                    150, 0, false, true);
        }
    }

    @Override
    public void writeNBT(CompoundTag compound, SaveType type) {
        super.writeNBT(compound, type);
        if (type != SaveType.BLOCK)
            compound.putLong("goal", this.goalTime);
    }

    @Override
    public void readNBT(CompoundTag compound, SaveType type) {
        super.readNBT(compound, type);
        if (type != SaveType.BLOCK)
            this.goalTime = compound.getLong("goal");
    }
}