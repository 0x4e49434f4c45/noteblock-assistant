package network.parthenon.noteblockassistant.song;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class SongBlockStates {
    /**
     * Marker block used to indicate the downbeat of a measure.
     */
    public static final BlockState MEASURE = Blocks.DIAMOND_BLOCK.defaultBlockState();

    /**
     * Marker block used to indicate the upbeat(s) of a measure.
     */
    public static final BlockState BEAT = Blocks.GOLD_BLOCK.defaultBlockState();

    /**
     * Marker block used to indicate beat subdivisions.
     */
    public static final BlockState SUBDIVISION = Blocks.SMOOTH_STONE.defaultBlockState();

    /**
     * Marker block in between beat subdivisions, where no note blocks can be placed.
     */
    public static final BlockState FILL = Blocks.SMOOTH_STONE.defaultBlockState();

    /**
     * Redstone base solid block. This is used where needed to support Redstone components
     * or carry power.
     */
    public static final BlockState REDSTONE_BASE = Blocks.SMOOTH_STONE.defaultBlockState();

    /**
     * Block used to create a floor, for accessibility as well as because certain instrument blocks are gravity-affected.
     */
    public static final BlockState FLOOR = Blocks.GLASS.defaultBlockState();
}
