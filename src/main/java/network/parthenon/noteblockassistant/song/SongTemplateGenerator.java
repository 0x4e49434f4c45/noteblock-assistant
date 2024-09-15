package network.parthenon.noteblockassistant.song;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RepeaterBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.List;

public class SongTemplateGenerator {
    public static void generate(
            World world,
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
        BlockState repeaterState = Blocks.REPEATER.getDefaultState()
                .with(RepeaterBlock.FACING, direction.getOpposite())
                .with(RepeaterBlock.DELAY, subdivisonDelay);

        for(int measure = 0; measure < measures; measure++) {
            for(int beat = 0; beat < beatsPerMeasure; beat++) {
                for(int subdivision = 0; subdivision < subdivisionsPerBeat; subdivision++) {
                    // set markers
                    world.setBlockState(currentPos, getMarkerBlock(beat, subdivision));
                    world.setBlockState(currentPos.offset(direction), SongBlockStates.FILL);

                    // set tracks
                    for(int track = 0; track < instrBlocks.size(); track++) {
                        BlockPos trackPos = currentPos.offset(direction.rotateClockwise(Direction.Axis.Y), SongGeometry.getTrackOffsetFromMarker(track));
                        // floor blocks
                        world.setBlockState(trackPos.offset(Direction.DOWN), SongBlockStates.FLOOR);
                        world.setBlockState(trackPos.offset(Direction.DOWN).offset(direction), SongBlockStates.FLOOR);
                        // instrument block
                        world.setBlockState(trackPos, instrBlocks.get(track));
                        // filler block; this is where the note block would go if desired
                        world.setBlockState(trackPos.offset(Direction.UP), SongBlockStates.REDSTONE_BASE);
                        if(SongGeometry.isCenterTrack(track)) {
                            // repeater and block it sits on
                            world.setBlockState(trackPos.offset(direction), SongBlockStates.REDSTONE_BASE);
                            world.setBlockState(trackPos.offset(direction).offset(Direction.UP), repeaterState);
                        }
                    }

                    // move to next subdivision
                    currentPos = currentPos.offset(direction, 2);
                }
            }
        }
    }

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
