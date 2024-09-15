package network.parthenon.noteblockassistant.song;

public class SongGeometry {
    /**
     * Gets the horizontal distance from the marker line of this track.
     * Tracks are placed first in the center of a group (for the repeaters), then the left, then the right, like so:
     * | | |  | | |    |
     * 1 0 2  4 3 5    6
     * @param track The track index for which to get the offset.
     */
    public static int getTrackOffsetFromMarker(int track) {
        return switch (track % 3) {
            // the first track in a group goes in the center
            case 0 -> track + 1;
            // second track in the group goes on the left
            case 1 -> track - 1;
            // third track goes on the right
            default -> track;
        }
        // space out groups of 3, and add 2 extra for marker and initial gap
        + track / 3 + 2;
    }

    /**
     * Gets the horizontal distance from the center of a track's group (always -1, 0, or 1).
     * Tracks are placed first in the center of a group (for the repeaters), then the left, then the right, like so:
     * | | |  | | |    |
     * 1 0 2  4 3 5    6
     * @param track The track index for which to get the offset.
     */
    public static int getTrackOffsetFromCenter(int track) {
       return switch (track % 3) {
           // the first track in a group goes in the center
           case 0 -> 0;
           // second track in the group goes on the left
           case 1 -> -1;
           // third track goes on the right
           default -> 1;
       };
    }

    /**
     * Gets whether this track is in the center of a group.
     * @param track The track index to check.
     */
    public static boolean isCenterTrack(int track) {
       return track % 3 == 0;
    }
}
