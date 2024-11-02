# Note Block Assistant

Note Block Assistant is a utility mod for making note block songs. It's designed to automate the manual steps in the way
I personally make note block songs, particularly the compaction step. If you also find it helpful, wonderful!

Note Block Assistant provides three commands:
- `/nba generate` generates a template with the specified parameters for length and meter, as well as instrument blocks
  (the blocks that go underneath the note blocks).
- `/nba select` loads the completed song and validates it, preparing it for compaction.
- `/nba placeCompact` places a compacted version of the song into the world. You can specify the length of the compacted
  version, or let the command choose a length that gets the result as close to square as possible for optimal listening.

Note Block Assistant requires Fabric API to work. It doesn't need any configuration; you can just add it to your client
(for single-player) or server and use it!

## How to use
Note: It's a good idea to have WorldEdit installed alongside Note Block Assistant. Litematica is also useful 
(practically necessary) to build the final song in survival.

Note Block Assistant works best in a superflat-style world.

### Generating a template
To start a new song, use the `/nba generate` command to generate a template. This command takes the form: `/nba generate
<measures> <beatsPerMeasure> <subdivisionsPerBeat> <subdivisionDelay> <instrumentBlock> [additional instrument blocks...]`

The timing parameters are used as follows:
- `measures`: This controls how many musical measures/bars to generate in your song template.
- `beatsPerMeasure`: This controls how many beats to generate for each measure. This is generally the top number of the
  time signature in music notation; for example, if your song is in 3/4 time, you would enter `3`. For certain time
  signatures it may be useful to choose a different number -- it's traditional in 6/8 time, for example, to use two
  beats per measure with three subdivisions per beat.
- `subdivisionsPerBeat`: This controls how many times to divide each beat of the measure. It represents the *shortest*
  note that you can have in your song. For example, in 3/4 time, if your shortest note is an 8th note, you would use
  `2`; if your song has 16th notes, you would use `4` instead. If you're using a compound time signature like 6/8,
  you'll need to adjust your subdivisions accordingly: with 2 beats per measure, if you want to include 16th notes,
  you would use `subdivisionsPerBeat` of `6`.
- `subdivisionDelay`: This controls the length of each subdivision -- and therefore the tempo of your song. Note Block
  Assistant generates songs that are rows of note blocks (or placeholder blocks) with repeaters in between controlling
  the time between each note; therefore, this number is the number of ticks each generated repeater is set to. This can
  be set to `1`, `2`, `3`, or `4`, with each repeater tick equal to 0.1 seconds. Examples:
  - With `subdivisonsPerBeat` of `2` and `subdivisionDelay` of `2`, each subidivion is 0.2s long and each beat is 0.4s,
    so the final tempo is 150 bpm.
  - With `subdivisionsPerBeat` of `3` and `subdivisionDelay` of `1`, each subdivision is 0.1s long and each beat is
    0.3s, so the final tempo is 200 bpm.

  This system does limit the tempos that can be achieved, but that is generally unavoidable. You'll need to choose your
  subdivision delay to get as close as possible to the desired tempo.

Although Note Block Assistant only generates templates with a single subdivision delay, the selection and compaction
system will preserve modifications that you make to the repeater timings. So, if your song has predominantly eighth
notes as the subdivision and only a couple of shorter notes, it may be possible to generate a template based on the
eighth note and then adjust in one spot to add the shorter notes. Remember that if you shorten one repeater, you'll
need to either lengthen the next one to make up for it, or shift the remainder of your template over (WorldEdit is
useful for this sort of change).

After the timing parameters, you can enter up to nine instrument block parameters. These are Minecraft blocks that will
be generated under the places where the note blocks go. If you wanted a song with a xylophone and flute sound, you could
use `minecraft:bone_block minecraft:clay` to generate two tracks, one for xylophone and one for flute. Note, each track
can only play one note, so if you wanted the xylophone to play chords, you might need to add additional
`minecraft:bone_block` to get more tracks. You can always use `minecraft:air` to generate a track without any instrument
block.

Although the template generator is limited to one instrument block per track, you can switch out the instrument blocks
to temporarily change the sound, and the selection/compaction system will preserve these changes. So if you want to
have percussion, for example, you may be able to use only one or two tracks even if you use several instruments. A beat
that alternates between bass drum and snare drum could be done on one track: generate the track using `minecraft:stone`
for the bass drum, then replace the snare drum beats with sand, then repeat the pattern to the end of the song
(WorldEdit's `//stack` command is excellent for repeating patterns).

### Creating your song
After you generate your template, a row of blocks will appear in front of you. This is your *marker line*. It functions
as a timeline that shows you where the measures and beats line up, to make it easy to keep track. Diamond blocks mark
the first note of each measure, and gold blocks mark the first note of each subsequent beat within that measure.

To the right of the marker line, your tracks are generated. Tracks are generated in groups of three, with repeaters
along the center track to carry a redstone signal. The smooth stone blocks on the second layer are the note blanks;
replace these with note blocks to add your notes. Note that within each group, the first track is generated in the
center, then the second track is generated on the left, and the third is generated on the right. This is because if
a group has only one track, it needs to be in the center where the repeaters are. So, if you generated a template
with nine tracks, the layout will be:

```
M   2 1 3   5 4 6   8 7 9
```

This might be surprising at first, but it gives the most efficient final layout after compaction.

To listen to your song, simply use redstone wire to connect a button to the first note block or blank of each center
track. Press the button, then fly along above the song to stay where you can hear it. (When you're done, the compaction
feature will generate a version of the song that you can listen to standing in one place.)

### Compacting your song
When you have everything just the way you want it, Note Block Assistant can take your completed song and generate
a compact version of it by folding the tracks together so that the note blocks interlock, and placing each group of
three tracks on a separate layer. This way, your song goes from a long, unwieldy (but easy to edit) line to a 3D build
that takes the smallest space practical and can be built in survival mode if desired. (This is possible to do with
WorldEdit, but it takes some math and a great deal of trouble, which is why this mod was originally created.)

To compact your song, first stand on top of the first diamond block on the marker line, and face the other end of the
marker line. Then, use the command `/nba select <tracks>`, where `tracks` is the number of tracks in your song. Note
Block Assistant will scan the marker line in the direction you're facing to calculate the length of the song, then 
scan and store all the note blocks, instrument blocks, repeaters, and blanks.

> For this reason, while you can swap out instrument blocks and shorten/lengthen the song while you're creating it,
your marker line must remain unbroken and you *cannot* change the locations of the tracks relative to the marker line.

Note Block Assistant will tell you the length of the song it found. If that's correct, then fly up to a nearby location
and run `/nba placeCompact` to generate the compact version. To reduce the resources needed to build in survival, Note
Block Assistant will automatically remove any unneeded note blanks and instrument blocks from the compact version, so
there's no need to do this manually beforehand. By default, a glass floor will be generated for each level, to make
building more practical and to hold any falling instrument blocks like sand or gravel. You can use WorldEdit to replace
the floors with something else or remove them if you like. You can also replace the smooth stone blocks with any other
solid block that can be powered at this stage.

By default, Note Block Assistant generates a compacted song that is as close to square as possible, so that you can
stand in the center and all the note blocks will be at a minimum distance and can be heard. If you have a specific area
the song needs to fit into, however, it may be useful to generate a different rectangular shape instead. If this is the
case, you can use `/nba placeCompact <length>` to specify the length in blocks of one side of the rectangle, and the
song will be automatically made as wide as it needs to be to accommodate that length. Note that extra length is needed
in one direction for the redstone wire to loop around at the end of each row, so while the note blocks will be fit into
the dimension you specify, the final shape will be slightly different based on which side of the rectangle you choose
for `<length>`.

Once you're happy with the shape of your compacted song, you can use it as a reference to build in survival (the 
Litematica mod is by far the best way to do this). Then, hook up a redstone circuit to power the first note block on
each layer simultaneously. There are many ways of doing this, and it depends on how you want the song to be triggered
as well as the place you're building it in, so Note Block Assistant doesn't generate this part.
