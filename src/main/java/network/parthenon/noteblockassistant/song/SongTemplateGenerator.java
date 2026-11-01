package network.parthenon.noteblockassistant.song;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.state.BlockState;

public class SongTemplateGenerator {
    /**
     * Generates a song template, ready to be populated with note blocks.
     * @param world               The world in which to generate the template.
     * @param cornerPos           Where to place the initial downbeat marker.
     * @param direction           The direction to generate the template in, starting from cornerPos.
     * @param measures            The number of measures to generate.
     * @param beatsPerMeasure     The number of beats per measure to generate.
     * @param subdivisionsPerBeat The number of subdivisions to generate per beat.
     * @param subdivisonDelay     The span, in repeater ticks, of each subdivision.
     * @param instrBlocks         A list of instrument-determining blocks. A track will be generated for each one.
     */
    public static void generate(
            Level world,
            BlockPos cornerPos,
            Direction direction,
            int measures,
            int beatsPerMeasure,
            int subdivisionsPerBeat,
            int subdivisonDelay,
            List<BlockState> instrBlocks
    ) {
        if(direction == Direction.DOWN || direction == Direction.UP) {
            throw new IllegalArgumentException("Invalid direction: Song template can only be generated in a horizontal direction.");
        }

        BlockPos currentPos = cornerPos;
        BlockState repeaterState = Blocks.REPEATER.defaultBlockState()
                .setValue(RepeaterBlock.FACING, direction.getOpposite())
                .setValue(RepeaterBlock.DELAY, subdivisonDelay);

        for(int measure = 0; measure < measures; measure++) {
            for(int beat = 0; beat < beatsPerMeasure; beat++) {
                for(int subdivision = 0; subdivision < subdivisionsPerBeat; subdivision++) {
                    // set markers
                    world.setBlockAndUpdate(currentPos, getMarkerBlock(beat, subdivision));
                    world.setBlockAndUpdate(currentPos.relative(direction), SongBlockStates.FILL);

                    // set tracks
                    for(int track = 0; track < instrBlocks.size(); track++) {
                        BlockPos trackPos = currentPos.relative(direction.getClockWise(Direction.Axis.Y), SongGeometry.getTrackOffsetFromMarker(track));
                        // floor blocks
                        world.setBlockAndUpdate(trackPos.relative(Direction.DOWN), SongBlockStates.FLOOR);
                        world.setBlockAndUpdate(trackPos.relative(Direction.DOWN).relative(direction), SongBlockStates.FLOOR);
                        // instrument block
                        world.setBlockAndUpdate(trackPos, instrBlocks.get(track));
                        // filler block; this is where the note block would go if desired
                        world.setBlockAndUpdate(trackPos.relative(Direction.UP), SongBlockStates.REDSTONE_BASE);
                        if(SongGeometry.isCenterTrack(track)) {
                            // repeater and block it sits on
                            world.setBlockAndUpdate(trackPos.relative(direction), SongBlockStates.REDSTONE_BASE);
                            world.setBlockAndUpdate(trackPos.relative(direction).relative(Direction.UP), repeaterState);
                        }
                    }

                    // move to next subdivision
                    currentPos = currentPos.relative(direction, 2);
                }
            }
        }
    }

    /**
     * Gets the appropriate marker block for the specified beat and subdivision within a measure.
     * @param beat        Beat number
     * @param subdivision Subdivision number
     */
    private static BlockState getMarkerBlock(int beat, int subdivision) {
        if(subdivision != 0) {
            return SongBlockStates.SUBDIVISION;
        }
        else if(beat != 0) {
            return SongBlockStates.BEAT;
        }
        else {
            return SongBlockStates.MEASURE;
        }
    }
}
