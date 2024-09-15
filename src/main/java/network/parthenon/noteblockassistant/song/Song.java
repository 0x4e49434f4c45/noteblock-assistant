package network.parthenon.noteblockassistant.song;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RepeaterBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.List;

public class Song {
    private List<List<Note>> tracks;

    public Song(List<List<Note>> tracks) {
        if(tracks.size() == 0) {
            throw new IllegalArgumentException("Cannot create a song with no tracks.");
        }

        for(List<Note> track : tracks) {
            if(track.size() != tracks.get(0).size()) {
                throw new IllegalArgumentException(("Tracks must be the same length!"));
            }
        }

        this.tracks = tracks;
    }

    public int getLength() {
        return tracks.get(0).size();
    }

    public int getTracks() {
        return tracks.size();
    }

    public Note getNote(int track, int note) {
        return tracks.get(track).get(note);
    }

    public static class Note {
        private final boolean isRest;

        private final int pitch;

        private final BlockState instrumentBlock;

        private final int nextDelay;

        private Note(boolean isRest, int pitch, BlockState instrumentBlock, int nextDelay) {
            this.isRest = isRest;
            this.pitch = pitch;
            this.instrumentBlock = instrumentBlock;
            this.nextDelay = nextDelay;
        }

        public boolean isRest() {
            return isRest;
        }

        public boolean isPitched() {
            return !isRest;
        }

        public int getPitch() {
            return pitch;
        }

        public BlockState getInstrumentBlock() {
            return instrumentBlock;
        }

        public int getNextDelay() {
            return nextDelay;
        }

        public static Note getPitched(int pitch, BlockState instrumentBlock, int nextDelay) {
            return new Note(false, pitch, instrumentBlock, nextDelay);
        }

        public static Note getRest(int nextDelay) {
            return new Note(true, 0, null, nextDelay);
        }
    }
}
