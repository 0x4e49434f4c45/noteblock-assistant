package network.parthenon.noteblockassistant.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import network.parthenon.noteblockassistant.NoteBlockAssistant;
import network.parthenon.noteblockassistant.song.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class NbaCommand {
    private static final String MEASURES_ARGUMENT = "measures";
    private static final String BEATS_PER_MEASURE_ARGUMENT = "beatsPerMeasure";
    private static final String SUBDIVISIONS_PER_BEAT_ARGUMENT = "subdivisionsPerBeat";
    private static final String SUBDIVISION_DELAY_ARGUMENT = "subdivisionDelay";
    private static final String INSTR_BLOCK_ARGUMENT_PREFIX = "instrumentBlock";
    private static final String NUM_TRACKS_ARGUMENT = "tracks";
    private static final String ROW_LENGTH_ARGUMENT = "rowLength";

    private static final HashMap<ServerPlayer, Song> playerSelections = new HashMap<>();

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext registryAccess,
            Commands.CommandSelection environment
    ) {
        dispatcher.register(
                Commands.literal("nba")
                        .then(Commands.literal("generate")
                                .then(Commands.argument(MEASURES_ARGUMENT, IntegerArgumentType.integer(1))
                                        .then(Commands.argument(BEATS_PER_MEASURE_ARGUMENT, IntegerArgumentType.integer(1))
                                                .then(Commands.argument(SUBDIVISIONS_PER_BEAT_ARGUMENT, IntegerArgumentType.integer(1))
                                                        .then(Commands.argument(SUBDIVISION_DELAY_ARGUMENT, IntegerArgumentType.integer(1, 4))
                                                                .then(NbaCommand.buildArgv(BlockStateArgument.block(registryAccess), INSTR_BLOCK_ARGUMENT_PREFIX, 9, NbaCommand::runGenerateCommand))))))
                        )
                        .then(Commands.literal("select")
                                .then(Commands.argument(NUM_TRACKS_ARGUMENT, IntegerArgumentType.integer(1))
                                        .executes(NbaCommand::runSelectCommand))
                        )
                        .then(Commands.literal("placeCompact")
                                .executes(ctx -> NbaCommand.runPlaceCompactCommand(ctx, 0))
                                .then(Commands.argument(ROW_LENGTH_ARGUMENT, IntegerArgumentType.integer(1))
                                        .executes(ctx -> NbaCommand.runPlaceCompactCommand(ctx, IntegerArgumentType.getInteger(ctx, ROW_LENGTH_ARGUMENT))))
                        )

        );
    }

    private static <T> RequiredArgumentBuilder<CommandSourceStack, T> buildArgv(
            ArgumentType<T> argumentType,
            String namePrefix,
            int limit,
            Command<CommandSourceStack> command
    ) {
        RequiredArgumentBuilder<CommandSourceStack, T> nextArgument = null;
        // build backwards!
        for(int argNum = limit; argNum > 0; argNum--) {
            RequiredArgumentBuilder<CommandSourceStack, T> argument = Commands.argument(namePrefix + argNum, argumentType)
                    .executes(command);
            if(nextArgument != null) {
                argument.then(nextArgument);
            }
            nextArgument = argument;
        }
        return nextArgument;
    }

    private static <T> List<T> yoinkArgv(
            CommandContext<CommandSourceStack> ctx,
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

    private static int runGenerateCommand(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        try {
            ServerPlayer player = source.getPlayer();
            if(player == null) {
                source.sendFailure(Component.literal("This command must be executed by a player."));
                return 1;
            }

            List<BlockInput> instrBlockArgs = yoinkArgv(ctx, INSTR_BLOCK_ARGUMENT_PREFIX, BlockInput.class);

            SongTemplateGenerator.generate(
                    player.level(),
                    player.blockPosition().relative(player.getDirection()),
                    player.getDirection(),
                    IntegerArgumentType.getInteger(ctx, MEASURES_ARGUMENT),
                    IntegerArgumentType.getInteger(ctx, BEATS_PER_MEASURE_ARGUMENT),
                    IntegerArgumentType.getInteger(ctx, SUBDIVISIONS_PER_BEAT_ARGUMENT),
                    IntegerArgumentType.getInteger(ctx, SUBDIVISION_DELAY_ARGUMENT),
                    instrBlockArgs.stream().map(BlockInput::getState).toList()
            );

            return 0;
        }
        catch(Exception e) {
            source.sendFailure(Component.nullToEmpty("There was an unexpected error running that command."));
            NoteBlockAssistant.LOGGER.error("Error while executing /nba generate", e);
            return 3;
        }
    }

    private static int runSelectCommand(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        try {
            ServerPlayer player = source.getPlayer();
            if(player == null) {
                source.sendFailure(Component.literal("This command must be executed by a player."));
                return 1;
            }

            Song song;
            try {
                song = SongLoader.loadFromWorld(
                        player.level(),
                        player.blockPosition().relative(Direction.DOWN),
                        player.getDirection(),
                        IntegerArgumentType.getInteger(ctx, NUM_TRACKS_ARGUMENT));
            }
            catch(SongDetectionException e) {
                source.sendFailure(Component.literal("Could not load a song at %s, facing %s: %s".formatted(e.getStartPos().toShortString(), e.getDirection(), e.getMessage())));
                source.sendFailure(Component.literal("Note: You must be standing on the first diamond block marker and facing the direction the song proceeds."));
                return 2;
            }

            source.sendSystemMessage(Component.literal("Selected a song with %d track(s) and length of %d note(s)".formatted(song.getTracks(), song.getLength())));
            playerSelections.put(player, song);
            return 0;
        }
        catch(Exception e) {
            source.sendFailure(Component.nullToEmpty("There was an unexpected error running that command."));
            NoteBlockAssistant.LOGGER.error("Error while executing /nba select", e);
            return 3;
        }
    }

    private static int runPlaceCompactCommand(CommandContext<CommandSourceStack> ctx, int customRowLength) {
        CommandSourceStack source = ctx.getSource();
        try {
            ServerPlayer player = source.getPlayer();
            if(player == null) {
                source.sendFailure(Component.literal("This command must be executed by a player."));
                return 1;
            }

            if(customRowLength % 2 != 0) {
                source.sendFailure(Component.literal("Row length must be an even number if specified."));
                return 2;
            }

            Song song = playerSelections.get(player);
            if(song == null) {
                source.sendFailure(Component.literal("You must select a song before you can compact it."));
                return 2;
            }

            if(customRowLength > 0) {
                SongCompactor.putCompact(
                        player.level(),
                        player.blockPosition().relative(player.getDirection()),
                        player.getDirection(),
                        customRowLength,
                        song
                );
            }
            else {
                SongCompactor.putCompact(
                        player.level(),
                        player.blockPosition().relative(player.getDirection()),
                        player.getDirection(),
                        song
                );
            }
            return 0;
        }
        catch(Exception e) {
            source.sendFailure(Component.nullToEmpty("There was an unexpected error running that command."));
            NoteBlockAssistant.LOGGER.error("Error while executing /nba placeCompact", e);
            return 3;
        }
    }
}
