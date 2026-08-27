package studio.threedonkeys.wce.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import studio.threedonkeys.wce.Wce;
import studio.threedonkeys.wce.WceConfig;
import studio.threedonkeys.wce.pattern.EditRecord;
import studio.threedonkeys.wce.recorders.BlockCats;
import studio.threedonkeys.wce.recorders.Mobs;
import studio.threedonkeys.wce.stamp.ChunkStamper;

public final class WceCommands {
	private WceCommands() {}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
	}

	private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("wce")
			.requires(source -> source.getPermissions().hasPermission(net.minecraft.command.DefaultPermissions.GAMEMASTERS))
			.executes(ctx -> sendHelp(ctx.getSource()))
			.then(CommandManager.literal("help").executes(ctx -> sendHelp(ctx.getSource())))
			.then(CommandManager.literal("pause").executes(ctx -> {
				Wce.setPaused(true);
				reply(ctx.getSource(), WceMessages.paused());
				return 1;
			}))
			.then(CommandManager.literal("resume").executes(ctx -> {
				Wce.setPaused(false);
				reply(ctx.getSource(), WceMessages.resumed());
				return 1;
			}))
			.then(CommandManager.literal("reset").executes(ctx -> {
				int forgotten = Wce.store().reset();
				ChunkStamper.resetSession();
				reply(ctx.getSource(), WceMessages.reset(forgotten));
				return 1;
			}))
			.then(CommandManager.literal("status").executes(ctx -> {
				int portals = 0;
				int endPortals = 0;
				int containers = 0;
				for (EditRecord record : Wce.store().pattern().values()) {
					if (record.dead) {
						continue;
					}
					if (record.state.isOf(net.minecraft.block.Blocks.NETHER_PORTAL)) {
						portals++;
					} else if (record.state.isOf(net.minecraft.block.Blocks.END_PORTAL)) {
						endPortals++;
					} else if (BlockCats.isContainer(record.state.getBlock())) {
						containers++;
					}
				}
				reply(ctx.getSource(), WceMessages.status(
					Wce.paused(),
					Wce.store().size(),
					WceConfig.PATTERN_MAX_EDITS,
					portals,
					endPortals,
					containers,
					Mobs.cloneCount()
				));
				return 1;
			}))
			.then(CommandManager.literal("verify").executes(ctx -> {
				ServerCommandSource source = ctx.getSource();
				ServerPlayerEntity player = source.getPlayer();
				if (player == null) {
					reply(source, WceMessages.verifyNeedPlayer());
					return 0;
				}
				ServerWorld world = player.getEntityWorld();
				BlockPos centre = player.getBlockPos();
				int radius = 16;
				int corrected = ChunkStamper.reconcileBox(
					world,
					centre.add(-radius, -radius, -radius),
					centre.add(radius, radius, radius)
				);
				reply(source, WceMessages.verified(radius, corrected));
				return 1;
			}))
		);
	}

	private static int sendHelp(ServerCommandSource source) {
		ServerPlayerEntity player = source.getPlayer();
		if (player != null) {
			WceMessages.send(player, WceMessages.helpBody());
			return 1;
		}
		for (Text line : WceMessages.helpBody()) {
			reply(source, line);
		}
		return 1;
	}

	private static void reply(ServerCommandSource source, Text message) {
		source.sendFeedback(() -> message, false);
	}

	public static void sendWelcome(ServerPlayerEntity player) {
		WceMessages.send(player, WceMessages.welcome());
	}
}
