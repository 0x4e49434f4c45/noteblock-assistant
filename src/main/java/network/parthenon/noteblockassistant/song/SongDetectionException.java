package network.parthenon.noteblockassistant.song;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class SongDetectionException extends Exception {
    private final BlockPos startPos;
    private final Direction direction;
    private final int tracks;

    public SongDetectionException(BlockPos startPos, Direction direction, int tracks, String msg) {
        super(msg);

        this.startPos = startPos;
        this.direction = direction;
        this.tracks = tracks;
    }

    public BlockPos getStartPos() {
        return startPos;
    }

    public Direction getDirection() {
        return direction;
    }

    public int getTracks() {
        return tracks;
    }
}
