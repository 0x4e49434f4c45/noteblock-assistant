package network.parthenon.noteblockassistant.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.command.argument.BlockStateArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;
import network.parthenon.noteblockassistant.NoteBlockAssistant;
import network.parthenon.noteblockassistant.song.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NbaCommand {
    private static final String MEASURES_ARGUMENT = "measures";
    private static final String BEATS_PER_MEASURE_ARGUMENT = "beatsPerMeasure";
    private static final String SUBDIVISIONS_PER_BEAT_ARGUMENT = "subdivisionsPerBeat";
    private static final String SUBDIVISION_DELAY_ARGUMENT = "subdivisionDelay";
    private static final String INSTR_BLOCK_ARGUMENT_PREFIX = "instrumentBlock";
    private static final String NUM_TRACKS_ARGUMENT = "tracks";
    private static final String ROW_LENGTH_ARGUMENT = "rowLength";

    private static final HashMap<ServerPlayerEntity, Song> playerSelections = new HashMap<>();

    public static void register(
            CommandDispatcher<ServerCommandSource> dispatcher,
            CommandRegistryAccess registryAccess,
            CommandManager.RegistrationEnvironment environment
    ) {
        dispatcher.register(
                CommandManager.literal("nba")
                        .then(CommandManager.literal("generate")
                                .then(CommandManager.argument(MEASURES_ARGUMENT, IntegerArgumentType.integer(1))
                                        .then(CommandManager.argument(BEATS_PER_MEASURE_ARGUMENT, IntegerArgumentType.integer(1))
                                                .then(CommandManager.argument(SUBDIVISIONS_PER_BEAT_ARGUMENT, IntegerArgumentType.integer(1))
                                                        .then(CommandManager.argument(SUBDIVISION_DELAY_ARGUMENT, IntegerArgumentType.integer(1, 4))
                                                                .then(NbaCommand.buildArgv(BlockStateArgumentType.blockState(registryAccess), INSTR_BLOCK_ARGUMENT_PREFIX, 9, NbaCommand::runGenerateCommand))))))
                        )
                        .then(CommandManager.literal("select")
                                .then(CommandManager.argument(NUM_TRACKS_ARGUMENT, IntegerArgumentType.integer(1))
                                        .executes(NbaCommand::runSelectCommand))
                        )
                        .then(CommandManager.literal("placeCompact")
                                .executes(ctx -> NbaCommand.runPlaceCompactCommand(ctx, 0))
                                .then(CommandManager.argument(ROW_LENGTH_ARGUMENT, IntegerArgumentType.integer(1))
                                        .executes(ctx -> NbaCommand.runPlaceCompactCommand(ctx, IntegerArgumentType.getInteger(ctx, ROW_LENGTH_ARGUMENT))))
                        )

        );
    }

    private static <T> RequiredArgumentBuilder<ServerCommandSource, T> buildArgv(
            ArgumentType<T> argumentType,
            String namePrefix,
            int limit,
            Command<ServerCommandSource> command
    ) {
        RequiredArgumentBuilder<ServerCommandSource, T> nextArgument = null;
        // build backwards!
        for(int argNum = limit; argNum > 0; argNum--) {
            RequiredArgumentBuilder<ServerCommandSource, T> argument = CommandManager.argument(namePrefix + argNum, argumentType)
                    .executes(command);
            if(nextArgument != null) {
                argument.then(nextArgument);
            }
            nextArgument = argument;
        }
        return nextArgument;
    }

    private static <T> List<T> yoinkArgv(
            CommandContext<ServerCommandSource> ctx,
            String prefix,
            Class<T> argType
    ) {
        ArrayList<T> values = new ArrayList<>();
        T value;
        int argNum = 1;
        try {
            // unfortunately, CommandContext does not provide any way to check whether an argument exists
            // our only option is to catch the IllegalArgumentException when we hit one that doesn't
            while(true) {
                values.add(ctx.getArgument(prefix + argNum, argType));
                argNum++;
            }
        }
        catch(IllegalArgumentException e) {
            // We have all the arguments now
            // I know not to use exceptions for flow control, I promise
            // but Mojang left me very little choice :(
        }
        return values;
    }

    private static int runGenerateCommand(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        try {
            ServerPlayerEntity player = source.getPlayer();
            if(player == null) {
                source.sendError(Text.literal("This command must be executed by a player."));
                return 1;
            }

            List<BlockStateArgument> instrBlockArgs = yoinkArgv(ctx, INSTR_BLOCK_ARGUMENT_PREFIX, BlockStateArgument.class);

            SongTemplateGenerator.generate(
                    player.getWorld(),
                    player.getBlockPos().offset(player.getHorizontalFacing()),
                    player.getHorizontalFacing(),
                    IntegerArgumentType.getInteger(ctx, MEASURES_ARGUMENT),
                    IntegerArgumentType.getInteger(ctx, BEATS_PER_MEASURE_ARGUMENT),
                    IntegerArgumentType.getInteger(ctx, SUBDIVISIONS_PER_BEAT_ARGUMENT),
                    IntegerArgumentType.getInteger(ctx, SUBDIVISION_DELAY_ARGUMENT),
                    instrBlockArgs.stream().map(BlockStateArgument::getBlockState).toList()
            );

            return 0;
        }
        catch(Exception e) {
            source.sendError(Text.of("There was an unexpected error running that command."));
            NoteBlockAssistant.LOGGER.error("Error while executing /nba generate", e);
            return 3;
        }
    }

    private static int runSelectCommand(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        try {
            ServerPlayerEntity player = source.getPlayer();
            if(player == null) {
                source.sendError(Text.literal("This command must be executed by a player."));
                return 1;
            }

            Song song;
            try {
                song = SongLoader.loadFromWorld(
                        player.getWorld(),
                        player.getBlockPos().offset(Direction.DOWN),
                        player.getHorizontalFacing(),
                        IntegerArgumentType.getInteger(ctx, NUM_TRACKS_ARGUMENT));
            }
            catch(SongDetectionException e) {
                source.sendError(Text.literal("Could not load a song at %s, facing %s: %s".formatted(e.getStartPos().toShortString(), e.getDirection(), e.getMessage())));
                source.sendError(Text.literal("Note: You must be standing on the first diamond block marker and facing the direction the song proceeds."));
                return 2;
            }

            source.sendMessage(Text.literal("Selected a song with %d track(s) and length of %d note(s)".formatted(song.getTracks(), song.getLength())));
            playerSelections.put(player, song);
            return 0;
        }
        catch(Exception e) {
            source.sendError(Text.of("There was an unexpected error running that command."));
            NoteBlockAssistant.LOGGER.error("Error while executing /nba select", e);
            return 3;
        }
    }

    private static int runPlaceCompactCommand(CommandContext<ServerCommandSource> ctx, int customRowLength) {
        ServerCommandSource source = ctx.getSource();
        try {
            ServerPlayerEntity player = source.getPlayer();
            if(player == null) {
                source.sendError(Text.literal("This command must be executed by a player."));
                return 1;
            }

            if(customRowLength % 2 != 0) {
                source.sendError(Text.literal("Row length must be an even number if specified."));
                return 2;
            }

            Song song = playerSelections.get(player);
            if(song == null) {
                source.sendError(Text.literal("You must select a song before you can compact it."));
                return 2;
            }

            if(customRowLength > 0) {
                SongCompactor.putCompact(
                        player.getWorld(),
                        player.getBlockPos().offset(player.getHorizontalFacing()),
                        player.getHorizontalFacing(),
                        customRowLength,
                        song
                );
            }
            else {
                SongCompactor.putCompact(
                        player.getWorld(),
                        player.getBlockPos().offset(player.getHorizontalFacing()),
                        player.getHorizontalFacing(),
                        song
                );
            }
            return 0;
        }
        catch(Exception e) {
            source.sendError(Text.of("There was an unexpected error running that command."));
            NoteBlockAssistant.LOGGER.error("Error while executing /nba placeCompact", e);
            return 3;
        }
    }
}
