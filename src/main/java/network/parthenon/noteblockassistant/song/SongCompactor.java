package network.parthenon.noteblockassistant.song;

import net.minecraft.block.*;
import net.minecraft.block.enums.WireConnection;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.world.World;

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
    public static void putCompact(World world, BlockPos startPos, Direction direction, Song song) {
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
    public static void putCompact(World world, BlockPos startPos, Direction direction, int rowLength, Song song) {
        if(rowLength % 2 == 1) {
            throw new IllegalArgumentException("Row length must be even.");
        }

        int numTracks = song.getTracks();
        int numNotes = song.getLength();
        Direction widthDirection = direction.rotateClockwise(Axis.Y);

        for(int trackNum = 0; trackNum < song.getTracks(); trackNum++) {
            Direction currentDirection = direction;
            BlockPos currentPos = startPos.offset(widthDirection, 1 + SongGeometry.getTrackOffsetFromCenter(trackNum))
                    // each group of three tracks goes 4 blocks down
                    .offset(Direction.DOWN, (trackNum / 3) * 4);
            int currentRowLength = 0;
            for(int noteNum = 0; noteNum < song.getLength(); noteNum++) {
                // place a supporting floor
                world.setBlockState(currentPos.offset(Direction.DOWN, 2), SongBlockStates.FLOOR);
                world.setBlockState(currentPos.offset(Direction.DOWN, 2).offset(currentDirection), SongBlockStates.FLOOR);

                // place note or rest
                Song.Note note = song.getNote(trackNum, noteNum);
                if(note.isPitched()) {
                    world.setBlockState(currentPos, Blocks.NOTE_BLOCK.getDefaultState().with(NoteBlock.NOTE, note.getPitch()));
                    world.setBlockState(
                            currentPos.offset(Direction.DOWN),
                            currentDirection == direction ?
                                    note.getInstrumentBlock() :
                                    note.getInstrumentBlock().rotate(BlockRotation.CLOCKWISE_180)
                    );
                }
                else if(SongGeometry.isCenterTrack(trackNum)) {
                    // rest only needs a block if it's on the center track and therefore carrying redstone power
                    world.setBlockState(currentPos, SongBlockStates.REDSTONE_BASE);
                }

                // place repeater if needed
                if(SongGeometry.isCenterTrack(trackNum) && note.getNextDelay() > 0) {
                    world.setBlockState(currentPos.offset(Direction.DOWN).offset(currentDirection), SongBlockStates.REDSTONE_BASE);
                    world.setBlockState(
                            currentPos.offset(currentDirection),
                            Blocks.REPEATER.getDefaultState()
                                    .with(RepeaterBlock.DELAY, note.getNextDelay())
                                    .with(RepeaterBlock.FACING, currentDirection.getOpposite())
                    );
                }

                if(currentRowLength < rowLength) {
                    currentPos = currentPos.offset(currentDirection, 2);
                    currentRowLength += 2;
                }
                else if(noteNum < song.getLength() - 1) {
                    // if we've reached the end of the row and there's still song left,
                    // place the redstone dust to turn us around
                    if(SongGeometry.isCenterTrack(trackNum)) {
                        placeTurnAround(world, currentPos.offset(currentDirection, 2), currentDirection, widthDirection);
                    }

                    // offset by 1 in the current direction before turning around, because the
                    // note blocks now go next to where the repeaters were
                    currentPos = currentPos.offset(widthDirection, 2).offset(currentDirection);
                    currentDirection = currentDirection.getOpposite();
                    currentRowLength = 0;
                }
            }
        }
    }

    private static void placeTurnAround(World world, BlockPos nextPos, Direction currentDirection, Direction widthDirection) {
        world.setBlockState(nextPos.offset(Direction.DOWN), SongBlockStates.REDSTONE_BASE);
        world.setBlockState(nextPos.offset(Direction.DOWN).offset(currentDirection), SongBlockStates.REDSTONE_BASE);
        world.setBlockState(nextPos.offset(Direction.DOWN).offset(currentDirection).offset(widthDirection), SongBlockStates.REDSTONE_BASE);
        world.setBlockState(nextPos.offset(Direction.DOWN).offset(currentDirection).offset(widthDirection, 2), SongBlockStates.REDSTONE_BASE);
        world.setBlockState(nextPos.offset(Direction.DOWN).offset(widthDirection, 2), SongBlockStates.REDSTONE_BASE);

        // "up" is the currentDirection, assume right is the widthDirection

        //  |
        //  |
        //  |
        world.setBlockState(nextPos,
                getRedstoneWire(EnumSet.of(currentDirection, currentDirection.getOpposite())));
        //
        //  ┌--
        //  |
        world.setBlockState(nextPos.offset(currentDirection),
                getRedstoneWire(EnumSet.of(currentDirection.getOpposite(), widthDirection)));

        //
        // ----
        //
        world.setBlockState(nextPos.offset(currentDirection).offset(widthDirection),
                getRedstoneWire(EnumSet.of(widthDirection.getOpposite(), widthDirection)));

        //
        // --┐
        //   |
        world.setBlockState(nextPos.offset(currentDirection).offset(widthDirection, 2),
                getRedstoneWire(EnumSet.of(widthDirection.getOpposite(), currentDirection.getOpposite())));

        //  |
        //  |
        //  |
        world.setBlockState(nextPos.offset(widthDirection, 2),
                getRedstoneWire(EnumSet.of(currentDirection.getOpposite(), currentDirection)));
    }

    private static BlockState getRedstoneWire(EnumSet<Direction> connections) {
        BlockState wireState = Blocks.REDSTONE_WIRE.getDefaultState();
        for(Direction d : connections) {
            wireState = wireState.with(RedstoneWireBlock.DIRECTION_TO_WIRE_CONNECTION_PROPERTY.get(d), WireConnection.SIDE);
        }
        return wireState;
    }
}
