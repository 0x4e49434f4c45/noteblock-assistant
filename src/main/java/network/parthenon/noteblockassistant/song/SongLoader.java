package network.parthenon.noteblockassistant.song;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.RepeaterBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class SongLoader {
    /**
     * Attempts to read a song in the format created by SongTemplateGenerator.
     * @param world     The world from which to load the song.
     * @param startPos  The position of the initial downbeat marker.
     * @param direction The direction the song proceeds.
     * @param numTracks The number of tracks to attempt to read.
     * @throws SongDetectionException If an invalid block is found.
     */
    public static Song loadFromWorld(World world, BlockPos startPos, Direction direction, int numTracks) throws SongDetectionException {
        if(direction == Direction.DOWN || direction == Direction.UP) {
            throw new IllegalArgumentException("Invalid direction: Song can only be loaded in a horizontal direction.");
        }

        if(world.getBlockState(startPos) != SongBlockStates.MEASURE) {
            throw new SongDetectionException(startPos, direction, numTracks, "Failed to find measure marker (%s) at %s".formatted(SongBlockStates.MEASURE, startPos.toShortString()));
        }
        // scan marker line for length
        BlockPos markerPos = startPos;
        BlockState currentBlock;
        while(true) {
            currentBlock = world.getBlockState(markerPos);
            if(currentBlock != SongBlockStates.MEASURE && currentBlock != SongBlockStates.BEAT && currentBlock != SongBlockStates.FILL) {
                break;
            }
            markerPos = markerPos.offset(direction);
        }

        int length = markerPos.getManhattanDistance(startPos);

        // validate and load the song
        ArrayList<List<Song.Note>> tracks = new ArrayList<>();
        Direction widthDirection = direction.rotateClockwise(Direction.Axis.Y);
        for(int trackNum = 0; trackNum < numTracks; trackNum++) {
            ArrayList<Song.Note> track = new ArrayList<>(length / 2);
            int trackOffset = SongGeometry.getTrackOffsetFromMarker(trackNum);
            for(int subdivisionOffset = 0; subdivisionOffset < length; subdivisionOffset += 2) {
                BlockPos subdivisionMarkerPos = startPos.offset(direction, subdivisionOffset);
                BlockPos noteBlockPos = subdivisionMarkerPos.offset(widthDirection, trackOffset).offset(Direction.UP);
                BlockState noteBlockState = world.getBlockState(noteBlockPos);
                BlockPos supportingBlockPos = noteBlockPos.offset(Direction.DOWN);
                BlockState supportingBlockState = world.getBlockState(supportingBlockPos);
                BlockPos forwardBlockPos = noteBlockPos.offset(direction);
                BlockState forwardBlockState = world.getBlockState(forwardBlockPos);
                BlockPos forwardLowerBlockPos = forwardBlockPos.offset(Direction.DOWN);
                BlockState forwardLowerBlockState = world.getBlockState(forwardLowerBlockPos);
                if(SongGeometry.isCenterTrack(trackNum)) {
                    if(!noteBlockState.isSolidBlock(world, noteBlockPos)) {
                        throw new SongDetectionException(startPos, direction, numTracks, "Missing/incorrect block at %s (must be solid block)".formatted(noteBlockPos.toShortString()));
                    }
                    if(forwardBlockState.getBlock() != Blocks.REPEATER || forwardBlockState.get(RepeaterBlock.FACING) != direction.getOpposite()) {
                        throw new SongDetectionException(startPos, direction, numTracks, "Missing/incorrect block at %s (must be %s)".formatted(forwardBlockPos.toShortString(), Blocks.REPEATER.getDefaultState().with(RepeaterBlock.FACING, direction.getOpposite())));
                    }

                    if(noteBlockState.getBlock() == Blocks.NOTE_BLOCK) {
                        track.add(Song.Note.getPitched(
                                noteBlockState.get(NoteBlock.NOTE),
                                supportingBlockState,
                                forwardBlockState.get(RepeaterBlock.DELAY)));
                    }
                    else {
                        track.add(Song.Note.getRest(forwardBlockState.get(RepeaterBlock.DELAY)));
                    }

                }
                else {
                    if(!forwardBlockState.isAir()) {
                        throw new SongDetectionException(startPos, direction, numTracks, "Block at %s must be air!".formatted(forwardBlockPos.toShortString()));
                    }
                    if(!forwardLowerBlockState.isAir()) {
                        throw new SongDetectionException(startPos, direction, numTracks, "Block at %s must be air!".formatted(forwardLowerBlockPos.toShortString()));
                    }
                    if(noteBlockState.getBlock() == Blocks.NOTE_BLOCK) {
                        // only center track notes have nextDelay as side track note blocks do not have repeaters
                        track.add(Song.Note.getPitched(noteBlockState.get(NoteBlock.NOTE), supportingBlockState, 0));
                    }
                    else {
                        track.add(Song.Note.getRest(0));
                    }
                }
            }
            tracks.add(track);
        }
        return new Song(tracks);
    }
}
