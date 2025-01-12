/*
 * Copyright (c) 2019.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.minecraftforge.lex.afd;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.lex.afd.Config.Server.Tier;

import static net.minecraftforge.lex.afd.AndesiteForDays.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AndesiteGenTile extends BlockEntity {
    private final ConfigCache config;
    private final LazyOptional<IItemHandler> inventory = LazyOptional.of(Inventory::new);
    private LazyOptional<IItemHandler> cache = null;
    private int count = 0;
    private int timer = 20;
    private int configTimer = 200;

    public AndesiteGenTile(Tier tier, BlockEntityType<?> tileType, BlockPos blockPos, BlockState blockState) {
        super(tileType, blockPos, blockState);
        this.config = new ConfigCache(tier);
        this.timer = tier.interval.get();
    }

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
       if (!this.remove && cap == ForgeCapabilities.ITEM_HANDLER)
          return inventory.cast();
       return super.getCapability(cap, side);
    }

    @Override
    public void setRemoved() {
        inventory.invalidate();
        super.setRemoved();
    }


    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        count = nbt.contains("count") ? nbt.getInt("count") : 0;
        timer = nbt.contains("timer") ? nbt.getInt("timer") : 0;
    }

    @Override
    public void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putInt("count", count);
        nbt.putInt("timer", timer);
    }

    public void updateCache() {
        BlockEntity tileEntity = level != null && level.isLoaded(worldPosition.above()) ? level.getBlockEntity(worldPosition.above()) : null;
        if (tileEntity != null){
            LazyOptional<IItemHandler> lazyOptional = tileEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN);
            if (lazyOptional.isPresent()) {
                if (this.cache != lazyOptional) {
                    this.cache = lazyOptional;
                    cache.addListener(l -> updateCache());
                }
            }
            else cache = LazyOptional.empty();
        }
        else cache = LazyOptional.empty();
    }

    private LazyOptional<IItemHandler> getCache() {
        if (cache == null)
            updateCache();
        return cache;
    }

    private void push() {
        ItemStack stack = new ItemStack(Items.ANDESITE, count);
        ItemStack result = getCache()
                .map(iItemHandler -> ItemHandlerHelper.insertItemStacked(iItemHandler, stack, false))
                .orElse(stack);

        if (result.isEmpty()) {
            count = 0;
            setChanged();
        } else if (result.getCount() != count) {
            count = result.getCount();
            setChanged();
        }
    }

    public static class Ticker implements BlockEntityTicker<AndesiteGenTile> {
        @Override
        public void tick(Level level, BlockPos blockPos, BlockState blockState, AndesiteGenTile andesiteGen) {
            if(level.isClientSide) return;
            if(--andesiteGen.timer <= 0) {
                andesiteGen.count += andesiteGen.config.count;
                andesiteGen.timer = andesiteGen.config.interval;

                if(andesiteGen.count > andesiteGen.config.max) andesiteGen.count = andesiteGen.config.max;
                if(andesiteGen.count < 0) andesiteGen.count = 0;

                andesiteGen.setChanged();
            }

            if(andesiteGen.config.pushes && andesiteGen.count > 0 && andesiteGen.getCache().isPresent()) {
                andesiteGen.push();
            }

            if(--andesiteGen.configTimer <= 0) {
                andesiteGen.config.update();
                andesiteGen.configTimer = 200;
            }

        }
    }

    private static class ConfigCache {
        private final Tier tier;
        private int interval;
        private int count;
        private int max;
        private boolean pushes;

        private ConfigCache(Tier tier) {
            this.tier = tier;
            update();
        }

        private void update() {
            this.interval = this.tier.interval.get();
            this.count = this.tier.count.get();
            this.max = this.tier.max.get();
            this.pushes = this.tier.pushes.get();
        }
    }

    private class Inventory implements IItemHandler {
        private final ItemStack stack = new ItemStack(Items.ANDESITE, 0);
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            stack.setCount(count);
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (count == 0 || amount == 0)
                return ItemStack.EMPTY;
            int ret = Math.min(count, amount);
            if (!simulate) {
                count -= ret;
                AndesiteGenTile.this.setChanged();
            }
            return new ItemStack(Items.ANDESITE, ret);
        }

        @Override
        public int getSlotLimit(int slot) {
            return Integer.MAX_VALUE;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    }

    public static AndesiteGenTile create(int tier, BlockPos blockPos, BlockState blockState) {
        switch (tier) {
            case 1: return new AndesiteGenTile(Config.SERVER.tier1, TIER1_TILE.get(), blockPos, blockState);
            case 2: return new AndesiteGenTile(Config.SERVER.tier2, TIER2_TILE.get(), blockPos, blockState);
            case 3: return new AndesiteGenTile(Config.SERVER.tier3, TIER3_TILE.get(), blockPos, blockState);
            case 4: return new AndesiteGenTile(Config.SERVER.tier4, TIER4_TILE.get(), blockPos, blockState);
            case 5: return new AndesiteGenTile(Config.SERVER.tier5, TIER5_TILE.get(), blockPos, blockState);
            default: throw new IllegalArgumentException("Unknown Tier: " + tier);
        }
    }

    public static BlockEntityType.BlockEntitySupplier<AndesiteGenTile> createSupplier(int tier) {
        return switch(tier) {
            case 1 -> (blockPos, blockState) -> new AndesiteGenTile(Config.SERVER.tier1, TIER1_TILE.get(), blockPos, blockState);
            case 2 -> (blockPos, blockState) -> new AndesiteGenTile(Config.SERVER.tier2, TIER2_TILE.get(), blockPos, blockState);
            case 3 -> (blockPos, blockState) -> new AndesiteGenTile(Config.SERVER.tier3, TIER3_TILE.get(), blockPos, blockState);
            case 4 -> (blockPos, blockState) -> new AndesiteGenTile(Config.SERVER.tier4, TIER4_TILE.get(), blockPos, blockState);
            case 5 -> (blockPos, blockState) -> new AndesiteGenTile(Config.SERVER.tier5, TIER5_TILE.get(), blockPos, blockState);
            default -> throw new IllegalArgumentException("Unknown Tier: " + tier);
        };
    }
}
