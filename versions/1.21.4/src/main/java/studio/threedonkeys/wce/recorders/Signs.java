package studio.threedonkeys.wce.recorders;

import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import studio.threedonkeys.wce.Wce;
import studio.threedonkeys.wce.WceConfig;
import studio.threedonkeys.wce.pattern.PatternStore;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class Signs {
	private static final Map<String, Watch> watched = new HashMap<>();

	private Signs() {}

	public static void reset() {
		watched.clear();
	}

	public static void watch(ServerWorld world, BlockPos pos, String baseline) {
		String key = Wce.cellKey(Wce.dimId(world), pos.getX(), pos.getY(), pos.getZ());
		int expire = Wce.currentTick() + WceConfig.SIGN_WATCH_SECONDS * 20;
		Watch existing = watched.get(key);
		if (existing != null) {
			existing.expireTick = expire;
			return;
		}
		watched.put(key, new Watch(world, pos.toImmutable(), baseline, expire));
	}

	public static String snapshot(ServerWorld world, BlockPos pos) {
		NbtCompound nbt = PatternStore.captureBlockEntity(world, pos);
		return nbt == null ? "" : nbt.toString();
	}

	public static void tick(MinecraftServer server) {
		if (Wce.paused() || watched.isEmpty()) {
			return;
		}
		int now = server.getTicks();
		Iterator<Map.Entry<String, Watch>> it = watched.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Watch> entry = it.next();
			Watch watch = entry.getValue();
			if (now > watch.expireTick) {
				it.remove();
				continue;
			}
			ServerWorld world = watch.world;
			if (world == null || world.getServer() != server) {
				it.remove();
				continue;
			}
			if (!BlockCats.chunkLoaded(world, watch.pos)) {
				continue;
			}
			BlockState state = world.getBlockState(watch.pos);
			if (!(state.getBlock() instanceof AbstractSignBlock)) {
				it.remove();
				continue;
			}
			String serialized = snapshot(world, watch.pos);
			if (serialized.equals(watch.lastData)) {
				continue;
			}
			watch.lastData = serialized;
			watch.expireTick = now + WceConfig.SIGN_WATCH_SECONDS * 20;
			Wce.store().recordEdit(world, watch.pos, state, PatternStore.captureBlockEntity(world, watch.pos), null);
		}
	}

	private static final class Watch {
		final ServerWorld world;
		final BlockPos pos;
		String lastData;
		int expireTick;

		Watch(ServerWorld world, BlockPos pos, String lastData, int expireTick) {
			this.world = world;
			this.pos = pos;
			this.lastData = lastData == null ? "" : lastData;
			this.expireTick = expireTick;
		}
	}
}
