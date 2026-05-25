package net.simplx.philter.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.simplx.philter.FilterBlock;
import net.simplx.philter.FilterBlockEntity;
import net.simplx.philter.FilterDesc;
import net.simplx.philter.FilterMode;
import net.simplx.philter.PhilterMod;

import java.util.List;

public class PhilterGameTest {

    // filter at (2,1,2), normal output SOUTH at (2,1,3), filter output EAST at (3,1,2)
    private static final BlockPos FILTER_POS = new BlockPos(2, 1, 2);
    private static final BlockPos NORMAL_OUT_POS = new BlockPos(2, 1, 3);
    private static final BlockPos FILTER_OUT_POS = new BlockPos(3, 1, 2);

    @GameTest(maxTicks = 100)
    public void matchingItemRoutedToFilterOutput(GameTestHelper helper) {
        BlockState filterState = PhilterMod.FILTER_BLOCK.defaultBlockState()
            .setValue(FilterBlock.FACING, Direction.SOUTH)
            .setValue(FilterBlock.FILTER, Direction.EAST)
            .setValue(FilterBlock.ENABLED, true)
            .setValue(FilterBlock.FILTERED, 0);
        helper.setBlock(FILTER_POS, filterState);

        FilterBlockEntity entity = helper.getBlockEntity(FILTER_POS, FilterBlockEntity.class);
        entity.setFilterDesc(new FilterDesc(FilterMode.MATCHES, List.of("minecraft:iron_ingot"), false));
        entity.setItem(0, new ItemStack(Items.IRON_INGOT));
        entity.setItem(1, new ItemStack(Items.GOLD_INGOT));

        helper.setBlock(NORMAL_OUT_POS, Blocks.CHEST);
        helper.setBlock(FILTER_OUT_POS, Blocks.CHEST);

        helper.succeedWhen(() -> {
            helper.assertContainerContains(FILTER_OUT_POS, Items.IRON_INGOT);
            helper.assertContainerContains(NORMAL_OUT_POS, Items.GOLD_INGOT);
        });
    }
}
