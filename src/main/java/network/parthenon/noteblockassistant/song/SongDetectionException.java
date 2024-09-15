package network.parthenon.noteblockassistant.song;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Indicates a problem validating the song at the specified location.
 */
public class SongDetectionException extends Exception {
    private final BlockPos startPos;
    private final Direction direction;
    private final int tracks;

    /**
     * Creates a new SongDetectionException.
     * @param startPos  The position at which song detection started. This is not necessarily the position of the error!
     * @param direction The direction in which song detection was attempted.
     * @param tracks    The number of tracks that were to be detected.
     * @param msg       Description of the error.
     */
    public SongDetectionException(BlockPos startPos, Direction direction, int tracks, String msg) {
        super(msg);

        this.startPos = startPos;
        this.direction = direction;
        this.tracks = tracks;
    }

    /**
     * Gets the position at which song detection started. This is not necessarily the position of the error!
     */
    public BlockPos getStartPos() {
        return startPos;
    }

    /**
     * Gets the direction in which song detection was attempted.
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * Gets the number of tracks that were to be detected.
     */
    public int getTracks() {
        return tracks;
    }
}
