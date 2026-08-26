package studio.threedonkeys.wce.command;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import studio.threedonkeys.wce.Wce;
import studio.threedonkeys.wce.WceConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Player-facing chat. Copy lives in lang/en_us.json and lang/pt_br.json.
 * The client translates the keys: Brazilian Portuguese if the game is pt_BR,
 * otherwise English (Minecraft falls back to en_us).
 */
public final class WceMessages {
	private WceMessages() {}

	private static final Formatting ACCENT = Formatting.AQUA;
	private static final Formatting MUTED = Formatting.DARK_GRAY;
	private static final Formatting LABEL = Formatting.GRAY;
	private static final Formatting VALUE = Formatting.WHITE;
	private static final Formatting WARN = Formatting.GOLD;
	private static final Formatting OK = Formatting.GREEN;

	private static final String RULE = "· · · · · · · · · · · · · · · · · · · ·";

	public static List<Text> welcome() {
		List<Text> lines = new ArrayList<>();
		lines.add(Text.literal(RULE).formatted(MUTED));
		lines.add(Text.translatable("wce.welcome.title").formatted(ACCENT, Formatting.BOLD));
		lines.add(Text.translatable(
			"wce.welcome.tagline",
			Text.literal(String.valueOf(WceConfig.COPY_INTERVAL)).formatted(VALUE)
		).formatted(LABEL));
		int edits = Wce.ready() ? Wce.store().size() : 0;
		if (edits == 0) {
			lines.add(Text.translatable("wce.welcome.empty").formatted(MUTED));
		} else {
			lines.add(Text.translatable(
				"wce.welcome.loaded",
				Text.literal(String.valueOf(edits)).formatted(VALUE),
				Text.literal(String.valueOf(WceConfig.PATTERN_MAX_EDITS)).formatted(LABEL)
			).formatted(LABEL));
		}
		lines.add(Text.literal(RULE).formatted(MUTED));
		lines.addAll(helpBody());
		return lines;
	}

	public static List<Text> helpBody() {
		List<Text> lines = new ArrayList<>();
		lines.add(Text.translatable("wce.help.header").formatted(WARN));
		lines.add(cmdLine("/wce help", "wce.help.help"));
		lines.add(cmdLine("/wce pause", "wce.help.pause"));
		lines.add(cmdLine("/wce resume", "wce.help.resume"));
		lines.add(cmdLine("/wce reset", "wce.help.reset"));
		lines.add(cmdLine("/wce status", "wce.help.status"));
		lines.add(cmdLine("/wce verify", "wce.help.verify"));
		lines.add(Text.literal(RULE).formatted(MUTED));
		return lines;
	}

	public static Text paused() {
		return prefix().append(Text.translatable("wce.cmd.pause").formatted(WARN));
	}

	public static Text resumed() {
		return prefix().append(Text.translatable("wce.cmd.resume").formatted(OK));
	}

	public static Text reset(int forgotten) {
		return prefix().append(Text.translatable(
			"wce.cmd.reset",
			Text.literal(String.valueOf(forgotten)).formatted(VALUE)
		).formatted(LABEL));
	}

	public static Text status(boolean paused, int edits, int max, int nether, int end, int containers, int clones) {
		Text mode = Text.translatable(paused ? "wce.status.paused" : "wce.status.active")
			.formatted(paused ? WARN : OK);
		return prefix().append(Text.translatable(
			"wce.status.line",
			mode,
			edits,
			max,
			nether,
			end,
			containers,
			clones
		).formatted(LABEL));
	}

	public static Text verifyNeedPlayer() {
		return prefix().append(Text.translatable("wce.cmd.verify.player").formatted(LABEL));
	}

	public static Text verified(int radius, int corrected) {
		return prefix().append(Text.translatable(
			"wce.cmd.verify.done",
			Text.literal(String.valueOf(radius)).formatted(VALUE),
			Text.literal(String.valueOf(corrected)).formatted(corrected > 0 ? WARN : OK)
		).formatted(LABEL));
	}

	public static Text unknownCommand() {
		return prefix().append(Text.translatable("wce.cmd.unknown").formatted(WARN));
	}

	public static void send(ServerPlayerEntity player, List<Text> lines) {
		for (Text line : lines) {
			player.sendMessage(line);
		}
	}

	private static MutableText prefix() {
		return Text.literal("WCE ").formatted(ACCENT, Formatting.BOLD)
			.append(Text.literal("› ").formatted(MUTED));
	}

	private static MutableText cmdLine(String command, String descriptionKey) {
		return Text.literal("    " + command).formatted(ACCENT)
			.append(Text.literal("  "))
			.append(Text.translatable(descriptionKey).formatted(LABEL));
	}
}
