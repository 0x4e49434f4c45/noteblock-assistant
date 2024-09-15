package network.parthenon.noteblockassistant.song;

import net.minecraft.block.BlockState;

import java.util.List;

/**
 * Stores the pitches, instruments, rests, and timings that make up a song.
 */
public class Song {
    private List<List<Note>> tracks;

    /**
     * Creates a new Song.
     * @param tracks Parallel lists of Note objects representing a single pitch or rest.
     *               All tracks must be the same length!
     */
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

    /**
     * Gets the length in notes (including rests) of this song.
     */
    public int getLength() {
        return tracks.get(0).size();
    }

    /**
     * Gets the number of tracks in this song.
     */
    public int getTracks() {
        return tracks.size();
    }

    /**
     * Gets the note at the specified track and note index.
     * @param track The track from which to get the note.
     * @param note  The note index to get.
     */
    public Note getNote(int track, int note) {
        return tracks.get(track).get(note);
    }

    /**
     * Represents a single note or rest.
     */
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

        /**
         * Gets whether this note is a rest.
         */
        public boolean isRest() {
            return isRest;
        }

        /**
         * Gets whether this note plays a sound.
         */
        public boolean isPitched() {
            return !isRest;
        }

        /**
         * Gets the pitch of this note. You must call Note.isRest() or Note.isPitched() to determine whether this note
         * actually sounds.
         */
        public int getPitch() {
            return pitch;
        }

        /**
         * Gets the block that determines the instrument for this note.
         */
        public BlockState getInstrumentBlock() {
            return instrumentBlock;
        }

        /**
         * Gets the delay (in repeater ticks) following activation of this note.
         */
        public int getNextDelay() {
            return nextDelay;
        }

        /**
         * Creates a pitched, or sounding, note.
         * @param pitch           The pitch at which this note sounds (0-23)
         * @param instrumentBlock The block placed under the note block to determine the instrument
         * @param nextDelay       The delay (in repeater ticks) following activation of this note.
         */
        public static Note getPitched(int pitch, BlockState instrumentBlock, int nextDelay) {
            return new Note(false, pitch, instrumentBlock, nextDelay);
        }

        /**
         * Creates a rest, or non-sounding note.
         * @param nextDelay The delay (in repeater ticks) following activation of this note.
         */
        public static Note getRest(int nextDelay) {
            return new Note(true, 0, null, nextDelay);
        }
    }
}
