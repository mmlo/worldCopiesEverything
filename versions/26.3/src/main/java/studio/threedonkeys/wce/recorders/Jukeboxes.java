package studio.threedonkeys.wce.recorders;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.item.ItemStack;

import net.minecraft.network.packet.s2c.play.StopSoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import studio.threedonkeys.wce.Wce;
import studio.threedonkeys.wce.WceConfig;

import java.util.HashMap;
import java.util.Map;

public final class Jukeboxes {
	private static final Map<String, Identifier> playing = new HashMap<>();

	private Jukeboxes() {}

	public static void reset() {
		playing.clear();
	}

	public static void onJukeboxChanged(ServerWorld world, BlockPos pos) {
		BlockEntity be = world.getBlockEntity(pos);
		if (!(be instanceof JukeboxBlockEntity jukebox)) {
			return;
		}
		ItemStack stack = jukebox.getStack();
		String key = Wce.cellKey(Wce.dimId(world), pos.getX(), pos.getY(), pos.getZ());
		if (stack.isEmpty()) {
			stopEcho(world, pos);
			return;
		}
		net.minecraft.component.type.JukeboxPlayableComponent component = stack.get(net.minecraft.component.DataComponentTypes.JUKEBOX_PLAYABLE);
		var songPair = component.song();
		if (songPair == null || songPair.entry().isEmpty()) return;
		var song = songPair.entry().get().value();
		if (song == null || song.soundEvent() == null) return;
		SoundEvent sound = song.soundEvent().value();
		if (sound == null) return;
		Identifier id = Registries.SOUND_EVENT.getId(sound);
		if (id == null) {
			return;
		}
		if (id.equals(playing.get(key))) {
			return;
		}
		playing.put(key, id);
		playEcho(world, pos, sound);
	}

	public static void playEcho(ServerWorld world, BlockPos origin, SoundEvent sound) {
		int r = WceConfig.JUKEBOX_ECHO_CHUNKS;
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				if (dx == 0 && dz == 0) {
					continue;
				}
				double x = origin.getX() + dx * WceConfig.COPY_INTERVAL + 0.5;
				double y = origin.getY() + 0.5;
				double z = origin.getZ() + dz * WceConfig.COPY_INTERVAL + 0.5;
				world.playSound(null, x, y, z, sound, SoundCategory.RECORDS, 4.0F, 1.0F);
			}
		}
	}

	public static void stopEcho(ServerWorld world, BlockPos pos) {
		String key = Wce.cellKey(Wce.dimId(world), pos.getX(), pos.getY(), pos.getZ());
		Identifier soundId = playing.remove(key);
		if (soundId == null) {
			return;
		}
		StopSoundS2CPacket packet = new StopSoundS2CPacket(soundId, SoundCategory.RECORDS);
		for (ServerPlayerEntity player : world.getPlayers()) {
			player.networkHandler.sendPacket(packet);
		}
	}
}
