package net.simplx.philter.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.simplx.philter.FilterBlock;
import net.simplx.philter.FilterBlockEntity;
import net.simplx.philter.FilterDesc;
import net.simplx.philter.FilterMode;
import net.simplx.philter.PhilterMod;

import java.util.List;

public class PhilterGameTest {

    // filter at (2,1,2); normal output SOUTH at (2,1,3); filter output EAST at (3,1,2)
    private static final BlockPos FILTER_POS = new BlockPos(2, 1, 2);
    private static final BlockPos NORMAL_OUT_POS = new BlockPos(2, 1, 3);
    private static final BlockPos FILTER_OUT_POS = new BlockPos(3, 1, 2);

    private static FilterBlockEntity setup(GameTestHelper helper, FilterDesc desc) {
        BlockState filterState = PhilterMod.FILTER_BLOCK.defaultBlockState()
            .setValue(FilterBlock.FACING, Direction.SOUTH)
            .setValue(FilterBlock.FILTER, Direction.EAST)
            .setValue(FilterBlock.ENABLED, true)
            .setValue(FilterBlock.FILTERED, 0);
        helper.setBlock(FILTER_POS, filterState);
        helper.setBlock(NORMAL_OUT_POS, Blocks.CHEST);
        helper.setBlock(FILTER_OUT_POS, Blocks.CHEST);
        FilterBlockEntity entity = helper.getBlockEntity(FILTER_POS, FilterBlockEntity.class);
        entity.setFilterDesc(desc);
        return entity;
    }

    // MATCHES mode: item matching pattern goes to filter output, non-matching to normal
    @GameTest(maxTicks = 100)
    public void matchingItemRoutedToFilterOutput(GameTestHelper helper) {
        FilterBlockEntity entity = setup(helper,
            new FilterDesc(FilterMode.MATCHES, List.of("minecraft:iron_ingot"), false));
        entity.setItem(0, new ItemStack(Items.IRON_INGOT));
        entity.setItem(1, new ItemStack(Items.GOLD_INGOT));
        helper.succeedWhen(() -> {
            helper.assertContainerContains(FILTER_OUT_POS, Items.IRON_INGOT);
            helper.assertContainerContains(NORMAL_OUT_POS, Items.GOLD_INGOT);
        });
    }

    // MATCHES mode: multiple patterns match multiple items, unmatched goes to normal
    @GameTest(maxTicks = 150)
    public void matchesMultiplePatternsAny(GameTestHelper helper) {
        FilterBlockEntity entity = setup(helper,
            new FilterDesc(FilterMode.MATCHES, List.of("iron_ingot", "gold_ingot"), false));
        entity.setItem(0, new ItemStack(Items.IRON_INGOT));
        entity.setItem(1, new ItemStack(Items.GOLD_INGOT));
        entity.setItem(2, new ItemStack(Items.DIAMOND));
        helper.succeedWhen(() -> {
            helper.assertContainerContains(FILTER_OUT_POS, Items.IRON_INGOT);
            helper.assertContainerContains(FILTER_OUT_POS, Items.GOLD_INGOT);
            helper.assertContainerContains(NORMAL_OUT_POS, Items.DIAMOND);
        });
    }

    // MATCHES mode: tag pattern routes matching item to filter, non-tag item to normal
    @GameTest(maxTicks = 100)
    public void matchesTagRoutes(GameTestHelper helper) {
        FilterBlockEntity entity = setup(helper,
            new FilterDesc(FilterMode.MATCHES, List.of("#minecraft:coals"), false));
        entity.setItem(0, new ItemStack(Items.COAL));
        entity.setItem(1, new ItemStack(Items.IRON_INGOT));
        helper.succeedWhen(() -> {
            helper.assertContainerContains(FILTER_OUT_POS, Items.COAL);
            helper.assertContainerContains(NORMAL_OUT_POS, Items.IRON_INGOT);
        });
    }

    // NONE mode: all items bypass filter and go to normal output
    @GameTest(maxTicks = 100)
    public void noneModeAllGoesToNormal(GameTestHelper helper) {
        FilterBlockEntity entity = setup(helper,
            new FilterDesc(FilterMode.NONE, List.of(), false));
        entity.setItem(0, new ItemStack(Items.IRON_INGOT));
        helper.succeedWhen(() -> {
            helper.assertContainerContains(NORMAL_OUT_POS, Items.IRON_INGOT);
            helper.assertContainerEmpty(FILTER_OUT_POS);
        });
    }

    // SAME_AS mode: items matching the filter-output chest contents go to filter output
    @GameTest(maxTicks = 100)
    public void sameAsWithTargetInventoryRoutes(GameTestHelper helper) {
        FilterBlockEntity entity = setup(helper,
            new FilterDesc(FilterMode.SAME_AS, List.of(), false));
        helper.getBlockEntity(FILTER_OUT_POS, ChestBlockEntity.class)
            .setItem(0, new ItemStack(Items.IRON_INGOT));
        entity.setItem(0, new ItemStack(Items.IRON_INGOT));
        entity.setItem(1, new ItemStack(Items.GOLD_INGOT));
        helper.succeedWhen(() -> {
            helper.assertContainerContains(FILTER_OUT_POS, Items.IRON_INGOT);
            helper.assertContainerContains(NORMAL_OUT_POS, Items.GOLD_INGOT);
        });
    }

    // SAME_AS mode: example slots override target-inventory matching
    @GameTest(maxTicks = 100)
    public void sameAsWithExampleSlotsRoutes(GameTestHelper helper) {
        FilterBlockEntity entity = setup(helper,
            new FilterDesc(FilterMode.SAME_AS, List.of(), false));
        entity.setItem(FilterBlockEntity.EXAMPLES_START, new ItemStack(Items.IRON_INGOT));
        entity.setItem(0, new ItemStack(Items.IRON_INGOT));
        entity.setItem(1, new ItemStack(Items.GOLD_INGOT));
        helper.succeedWhen(() -> {
            helper.assertContainerContains(FILTER_OUT_POS, Items.IRON_INGOT);
            helper.assertContainerContains(NORMAL_OUT_POS, Items.GOLD_INGOT);
        });
    }

    // exact=true: same item type AND same components required to match
    @GameTest(maxTicks = 100)
    public void sameAsExactAcceptsSameComponents(GameTestHelper helper) {
        FilterBlockEntity entity = setup(helper,
            new FilterDesc(FilterMode.SAME_AS, List.of(), true));
        entity.setItem(FilterBlockEntity.EXAMPLES_START, new ItemStack(Items.IRON_INGOT));
        entity.setItem(0, new ItemStack(Items.IRON_INGOT));
        helper.succeedWhen(() -> {
            helper.assertContainerContains(FILTER_OUT_POS, Items.IRON_INGOT);
            helper.assertContainerEmpty(NORMAL_OUT_POS);
        });
    }

    // exact=true: item with different components (custom name) does not match plain example
    @GameTest(maxTicks = 100)
    public void sameAsExactRejectsDifferentComponents(GameTestHelper helper) {
        FilterBlockEntity entity = setup(helper,
            new FilterDesc(FilterMode.SAME_AS, List.of(), true));
        entity.setItem(FilterBlockEntity.EXAMPLES_START, new ItemStack(Items.IRON_INGOT));
        ItemStack named = new ItemStack(Items.IRON_INGOT);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("special"));
        entity.setItem(0, named);
        helper.succeedWhen(() -> {
            helper.assertContainerContains(NORMAL_OUT_POS, Items.IRON_INGOT);
            helper.assertContainerEmpty(FILTER_OUT_POS);
        });
    }

    // exact=false: same item type is sufficient; component differences are ignored
    @GameTest(maxTicks = 100)
    public void sameAsNonExactAcceptsDifferentComponents(GameTestHelper helper) {
        FilterBlockEntity entity = setup(helper,
            new FilterDesc(FilterMode.SAME_AS, List.of(), false));
        entity.setItem(FilterBlockEntity.EXAMPLES_START, new ItemStack(Items.IRON_INGOT));
        ItemStack named = new ItemStack(Items.IRON_INGOT);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("special"));
        entity.setItem(0, named);
        helper.succeedWhen(() -> {
            helper.assertContainerContains(FILTER_OUT_POS, Items.IRON_INGOT);
            helper.assertContainerEmpty(NORMAL_OUT_POS);
        });
    }

    // routing is independent of slot order: non-matching item in earlier slot, matching in later slot
    @GameTest(maxTicks = 100)
    public void matchesNonMatchingSlotBeforeMatching(GameTestHelper helper) {
        FilterBlockEntity entity = setup(helper,
            new FilterDesc(FilterMode.MATCHES, List.of("iron_ingot"), false));
        entity.setItem(0, new ItemStack(Items.GOLD_INGOT));  // non-matching, processed first
        entity.setItem(1, new ItemStack(Items.IRON_INGOT));  // matching, processed second
        helper.succeedWhen(() -> {
            helper.assertContainerContains(FILTER_OUT_POS, Items.IRON_INGOT);
            helper.assertContainerContains(NORMAL_OUT_POS, Items.GOLD_INGOT);
        });
    }

    @GameTest(maxTicks = 100)
    public void sameAsNonMatchingSlotBeforeMatching(GameTestHelper helper) {
        FilterBlockEntity entity = setup(helper,
            new FilterDesc(FilterMode.SAME_AS, List.of(), false));
        entity.setItem(FilterBlockEntity.EXAMPLES_START, new ItemStack(Items.IRON_INGOT));
        entity.setItem(0, new ItemStack(Items.GOLD_INGOT));  // non-matching, processed first
        entity.setItem(1, new ItemStack(Items.IRON_INGOT));  // matching, processed second
        helper.succeedWhen(() -> {
            helper.assertContainerContains(FILTER_OUT_POS, Items.IRON_INGOT);
            helper.assertContainerContains(NORMAL_OUT_POS, Items.GOLD_INGOT);
        });
    }

    // interleaved slots: matching and non-matching alternating across four slots
    @GameTest(maxTicks = 200)
    public void matchesInterleavedSlots(GameTestHelper helper) {
        FilterBlockEntity entity = setup(helper,
            new FilterDesc(FilterMode.MATCHES, List.of("iron_ingot"), false));
        entity.setItem(0, new ItemStack(Items.GOLD_INGOT));
        entity.setItem(1, new ItemStack(Items.IRON_INGOT));
        entity.setItem(2, new ItemStack(Items.GOLD_INGOT));
        entity.setItem(3, new ItemStack(Items.IRON_INGOT));
        helper.succeedWhen(() -> {
            helper.assertContainerContains(FILTER_OUT_POS, Items.IRON_INGOT);
            helper.assertContainerContains(NORMAL_OUT_POS, Items.GOLD_INGOT);
        });
    }

    // SAME_AS mode: empty target inventory and no example slots routes everything to normal
    @GameTest(maxTicks = 100)
    public void sameAsEmptyExamplesAllToNormal(GameTestHelper helper) {
        FilterBlockEntity entity = setup(helper,
            new FilterDesc(FilterMode.SAME_AS, List.of(), false));
        entity.setItem(0, new ItemStack(Items.GOLD_INGOT));
        helper.succeedWhen(() -> {
            helper.assertContainerContains(NORMAL_OUT_POS, Items.GOLD_INGOT);
            helper.assertContainerEmpty(FILTER_OUT_POS);
        });
    }

    // ---- directional tests ----
    // filter at (2,2,2) so DOWN has room below; output positions are computed from facing/filter directions

    private record Routing(FilterBlockEntity entity, BlockPos normalOutPos, BlockPos filterOutPos) {}

    private static Routing setupDirectional(GameTestHelper helper, FilterDesc desc,
                                            Direction facing, Direction filterDir) {
        BlockPos filterPos = new BlockPos(2, 2, 2);
        BlockPos normalOutPos = filterPos.relative(facing);
        BlockPos filterOutPos = filterPos.relative(filterDir);
        BlockState filterState = PhilterMod.FILTER_BLOCK.defaultBlockState()
            .setValue(FilterBlock.FACING, facing)
            .setValue(FilterBlock.FILTER, filterDir)
            .setValue(FilterBlock.ENABLED, true)
            .setValue(FilterBlock.FILTERED, 0);
        helper.setBlock(filterPos, filterState);
        helper.setBlock(normalOutPos, Blocks.CHEST);
        helper.setBlock(filterOutPos, Blocks.CHEST);
        FilterBlockEntity entity = helper.getBlockEntity(filterPos, FilterBlockEntity.class);
        entity.setFilterDesc(desc);
        return new Routing(entity, normalOutPos, filterOutPos);
    }

    private void matchesRouting(GameTestHelper helper, Direction facing, Direction filterDir) {
        Routing r = setupDirectional(helper,
            new FilterDesc(FilterMode.MATCHES, List.of("iron_ingot"), false),
            facing, filterDir);
        r.entity().setItem(0, new ItemStack(Items.IRON_INGOT));
        r.entity().setItem(1, new ItemStack(Items.GOLD_INGOT));
        helper.succeedWhen(() -> {
            helper.assertContainerContains(r.filterOutPos(), Items.IRON_INGOT);
            helper.assertContainerContains(r.normalOutPos(), Items.GOLD_INGOT);
        });
    }

    @GameTest(maxTicks = 100)
    public void routingNormalDownFilterSouth(GameTestHelper helper) {
        matchesRouting(helper, Direction.DOWN, Direction.SOUTH);
    }

    @GameTest(maxTicks = 100)
    public void routingNormalSouthFilterDown(GameTestHelper helper) {
        matchesRouting(helper, Direction.SOUTH, Direction.DOWN);
    }

    @GameTest(maxTicks = 100)
    public void routingNormalDownFilterEast(GameTestHelper helper) {
        matchesRouting(helper, Direction.DOWN, Direction.EAST);
    }

    @GameTest(maxTicks = 100)
    public void routingNormalEastFilterDown(GameTestHelper helper) {
        matchesRouting(helper, Direction.EAST, Direction.DOWN);
    }

    @GameTest(maxTicks = 100)
    public void routingNormalSouthFilterEast(GameTestHelper helper) {
        matchesRouting(helper, Direction.SOUTH, Direction.EAST);
    }

    @GameTest(maxTicks = 100)
    public void routingNormalNorthFilterWest(GameTestHelper helper) {
        matchesRouting(helper, Direction.NORTH, Direction.WEST);
    }

    @GameTest(maxTicks = 100)
    public void routingNormalDownFilterUp(GameTestHelper helper) {
        matchesRouting(helper, Direction.DOWN, Direction.UP);
    }

    // ---- redstone tests ----
    // ENABLED=false (redstone signal) must block both filtered and unfiltered movement

    @GameTest(maxTicks = 60)
    public void redstoneDisablesBothFilteredAndUnfilteredMovement(GameTestHelper helper) {
        Routing r = setupDirectional(helper,
            new FilterDesc(FilterMode.MATCHES, List.of("iron_ingot"), false),
            Direction.SOUTH, Direction.EAST);
        // filterPos is always (2,2,2) in setupDirectional; disable it before items can move
        BlockPos filterPos = new BlockPos(2, 2, 2);
        helper.setBlock(filterPos,
            helper.getBlockState(filterPos).setValue(FilterBlock.ENABLED, false));
        r.entity().setItem(0, new ItemStack(Items.IRON_INGOT));  // would route to filter output
        r.entity().setItem(1, new ItemStack(Items.GOLD_INGOT));  // would route to normal output
        helper.succeedOnTickWhen(50, () -> {
            helper.assertContainerEmpty(r.filterOutPos());
            helper.assertContainerEmpty(r.normalOutPos());
        });
    }
}
