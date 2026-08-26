package studio.threedonkeys.wce.stamp;

import net.minecraft.entity.ItemEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import studio.threedonkeys.wce.Wce;
import studio.threedonkeys.wce.WceConfig;

import java.util.HashSet;
import java.util.Set;

public final class MirrorDrops {
	private static Set<String> current = new HashSet<>();
	private static Set<String> previous = new HashSet<>();
	private static int lastSwapTick = 0;

	private MirrorDrops() {}

	public static void reset() {
		current = new HashSet<>();
		previous = new HashSet<>();
		lastSwapTick = 0;
	}

	public static void tick(int serverTick) {
		if (serverTick - lastSwapTick >= WceConfig.MIRROR_DROP_WINDOW) {
			previous = current;
			current = new HashSet<>();
			lastSwapTick = serverTick;
		}
	}

	public static void noteWrite(ServerWorld world, BlockPos pos) {
		current.add(Wce.cellKey(Wce.dimId(world), pos.getX(), pos.getY(), pos.getZ()));
	}

	public static boolean wasWriteNear(ServerWorld world, BlockPos pos) {
		String dim = Wce.dimId(world);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					String key = Wce.cellKey(dim, pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
					if (current.contains(key) || previous.contains(key)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public static boolean playerWithin(ServerWorld world, BlockPos pos, double range) {
		double rangeSq = range * range;
		double x = pos.getX() + 0.5;
		double y = pos.getY() + 0.5;
		double z = pos.getZ() + 0.5;
		for (ServerPlayerEntity player : world.getPlayers()) {
			double dx = player.getX() - x;
			double dy = player.getY() - y;
			double dz = player.getZ() - z;
			if (dx * dx + dy * dy + dz * dz <= rangeSq) {
				return true;
			}
		}
		return false;
	}

	public static ServerPlayerEntity nearestPlayer(ServerWorld world, double x, double y, double z, double range) {
		double best = range * range;
		ServerPlayerEntity found = null;
		for (ServerPlayerEntity player : world.getPlayers()) {
			double dx = player.getX() - x;
			double dz = player.getZ() - z;
			double dist = dx * dx + dz * dz;
			if (dist <= best) {
				best = dist;
				found = player;
			}
		}
		return found;
	}

	public static void onItem(ItemEntity item, ServerWorld world) {
		if (Wce.paused() || Wce.isApplying()) {
			return;
		}
		BlockPos pos = item.getBlockPos();
		if (!wasWriteNear(world, pos)) {
			return;
		}
		if (playerWithin(world, pos, WceConfig.MIRROR_DROP_PLAYER_RANGE)) {
			return;
		}
		item.discard();
	}

	public static void register() {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (entity instanceof ItemEntity item) {
				onItem(item, world);
			}
		});
	}
}
