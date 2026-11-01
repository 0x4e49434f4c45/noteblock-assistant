package network.parthenon.noteblockassistant.song;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.RedstoneWireBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import java.util.EnumSet;

/**
 * Implements the song compaction algorithm, which attempts to fold a song into the least practical space.
 * The compacted song has unnecessary non-sounding blocks removed.
 */
public class SongCompactor {
    /**
     * Places the song into the world in compact form, attempting to place the note blocks
     * in as close to a square shape as possible (to minimize the distance to the player
     * when standing in the center).
     * @param world     World in which to place the compacted song.
     * @param startPos  Position at which to place the note block in the upper corner.
     * @param direction Direction the song initially travels away from the start position.
     * @param song      The song to place.
     */
    public static void putCompact(Level world, BlockPos startPos, Direction direction, Song song) {
        // Each row (excluding the first) adds 2 blocks to the width of the built song.
        // So, ideally, we would have one row for every 2 blocks of length; i.e. the length of each row would be
        // twice the number of rows.
        // rowLength * numRows = songLengthBlocks * 2
        // rowLength = numRows * 2
        // by substitution, rowLength2 * (rowLength / 2) = songLength * 2
        // then rowLength^2 / 2 = songLength * 2
        // rowLength = sqrt(songLength * 4) = sqrt(songLength) * 2
        // Use the ceiling function to ensure we always choose a row length above the square root if it's not an
        // integer - this makes up for the extra line of blocks in the first row when we allocate 2 blocks of width
        // per group.
        // It doesn't have to be perfect, it just has to be reasonably close.
        putCompact(world, startPos, direction, (int) Math.ceil(Math.sqrt(song.getLength())) * 2, song);
    }

    /**
     * Places the song into the world in compact form, using the specified row length in blocks.
     * @param world     World in which to place the compacted song.
     * @param startPos  Position at which to place the note block in the upper corner.
     * @param direction Direction the song initially travels away from the start position.
     * @param rowLength The length of each row. Must be an even number to allow for a note block and repeater per note..
     * @param song      The song to place.
     */
    public static void putCompact(Level world, BlockPos startPos, Direction direction, int rowLength, Song song) {
        if(rowLength % 2 == 1) {
            throw new IllegalArgumentException("Row length must be even.");
        }

        int numTracks = song.getTracks();
        int numNotes = song.getLength();
        Direction widthDirection = direction.getClockWise(Axis.Y);

        for(int trackNum = 0; trackNum < song.getTracks(); trackNum++) {
            Direction currentDirection = direction;
            BlockPos currentPos = startPos.relative(widthDirection, 1 + SongGeometry.getTrackOffsetFromCenter(trackNum))
                    // each group of three tracks goes 4 blocks down
                    .relative(Direction.DOWN, (trackNum / 3) * 4);
            int currentRowLength = 0;
            for(int noteNum = 0; noteNum < song.getLength(); noteNum++) {
                // place a supporting floor
                world.setBlockAndUpdate(currentPos.relative(Direction.DOWN, 2), SongBlockStates.FLOOR);
                world.setBlockAndUpdate(currentPos.relative(Direction.DOWN, 2).relative(currentDirection), SongBlockStates.FLOOR);

                // place note or rest
                Song.Note note = song.getNote(trackNum, noteNum);
                if(note.isPitched()) {
                    world.setBlockAndUpdate(currentPos, Blocks.NOTE_BLOCK.defaultBlockState().setValue(NoteBlock.NOTE, note.getPitch()));
                    world.setBlockAndUpdate(
                            currentPos.relative(Direction.DOWN),
                            currentDirection == direction ?
                                    note.getInstrumentBlock() :
                                    note.getInstrumentBlock().rotate(Rotation.CLOCKWISE_180)
                    );
                }
                else if(SongGeometry.isCenterTrack(trackNum)) {
                    // rest only needs a block if it's on the center track and therefore carrying redstone power
                    world.setBlockAndUpdate(currentPos, SongBlockStates.REDSTONE_BASE);
                }

                // place repeater if needed
                if(SongGeometry.isCenterTrack(trackNum) && note.getNextDelay() > 0) {
                    world.setBlockAndUpdate(currentPos.relative(Direction.DOWN).relative(currentDirection), SongBlockStates.REDSTONE_BASE);
                    world.setBlockAndUpdate(
                            currentPos.relative(currentDirection),
                            Blocks.REPEATER.defaultBlockState()
                                    .setValue(RepeaterBlock.DELAY, note.getNextDelay())
                                    .setValue(RepeaterBlock.FACING, currentDirection.getOpposite())
                    );
                }

                if(currentRowLength < rowLength) {
                    currentPos = currentPos.relative(currentDirection, 2);
                    currentRowLength += 2;
                }
                else if(noteNum < song.getLength() - 1) {
                    // if we've reached the end of the row and there's still song left,
                    // place the redstone dust to turn us around
                    if(SongGeometry.isCenterTrack(trackNum)) {
                        placeTurnAround(world, currentPos.relative(currentDirection, 2), currentDirection, widthDirection);
                    }

                    // offset by 1 in the current direction before turning around, because the
                    // note blocks now go next to where the repeaters were
                    currentPos = currentPos.relative(widthDirection, 2).relative(currentDirection);
                    currentDirection = currentDirection.getOpposite();
                    currentRowLength = 0;
                }
            }
        }
    }

    private static void placeTurnAround(Level world, BlockPos nextPos, Direction currentDirection, Direction widthDirection) {
        world.setBlockAndUpdate(nextPos.relative(Direction.DOWN), SongBlockStates.REDSTONE_BASE);
        world.setBlockAndUpdate(nextPos.relative(Direction.DOWN).relative(currentDirection), SongBlockStates.REDSTONE_BASE);
        world.setBlockAndUpdate(nextPos.relative(Direction.DOWN).relative(currentDirection).relative(widthDirection), SongBlockStates.REDSTONE_BASE);
        world.setBlockAndUpdate(nextPos.relative(Direction.DOWN).relative(currentDirection).relative(widthDirection, 2), SongBlockStates.REDSTONE_BASE);
        world.setBlockAndUpdate(nextPos.relative(Direction.DOWN).relative(widthDirection, 2), SongBlockStates.REDSTONE_BASE);

        // "up" is the currentDirection, assume right is the widthDirection

        //  |
        //  |
        //  |
        world.setBlockAndUpdate(nextPos,
                getRedstoneWire(EnumSet.of(currentDirection, currentDirection.getOpposite())));
        //
        //  ┌--
        //  |
        world.setBlockAndUpdate(nextPos.relative(currentDirection),
                getRedstoneWire(EnumSet.of(currentDirection.getOpposite(), widthDirection)));

        //
        // ----
        //
        world.setBlockAndUpdate(nextPos.relative(currentDirection).relative(widthDirection),
                getRedstoneWire(EnumSet.of(widthDirection.getOpposite(), widthDirection)));

        //
        // --┐
        //   |
        world.setBlockAndUpdate(nextPos.relative(currentDirection).relative(widthDirection, 2),
                getRedstoneWire(EnumSet.of(widthDirection.getOpposite(), currentDirection.getOpposite())));

        //  |
        //  |
        //  |
        world.setBlockAndUpdate(nextPos.relative(widthDirection, 2),
                getRedstoneWire(EnumSet.of(currentDirection.getOpposite(), currentDirection)));
    }

    private static BlockState getRedstoneWire(EnumSet<Direction> connections) {
        BlockState wireState = Blocks.REDSTONE_WIRE.defaultBlockState();
        for(Direction d : connections) {
            wireState = wireState.setValue(RedstoneWireBlock.PROPERTY_BY_DIRECTION.get(d), RedstoneSide.SIDE);
        }
        return wireState;
    }
}
