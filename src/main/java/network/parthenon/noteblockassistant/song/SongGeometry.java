package network.parthenon.noteblockassistant.song;

public class SongGeometry {
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

   public static boolean isCenterTrack(int track) {
       return track % 3 == 0;
   }
}
