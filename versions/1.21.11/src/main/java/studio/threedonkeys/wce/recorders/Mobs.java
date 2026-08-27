package studio.threedonkeys.wce.recorders;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import studio.threedonkeys.wce.Wce;
import studio.threedonkeys.wce.WceConfig;
import studio.threedonkeys.wce.stamp.MirrorDrops;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class Mobs {
	public static boolean spawningClone;
	private static final List<Clone> clones = new ArrayList<>();
	private static final List<int[]> MOB_OFFSETS = buildOffsets();

	private Mobs() {}

	private static List<int[]> buildOffsets() {
		List<int[]> list = new ArrayList<>();
		int r = WceConfig.MOB_COPY_RADIUS;
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				if (dx != 0 || dz != 0) {
					list.add(new int[] {dx, dz});
				}
			}
		}
		return List.copyOf(list);
	}

	public static void reset() {
		clones.clear();
		spawningClone = false;
	}

	public static void register() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (entity.getCommandTags().contains(WceConfig.CLONE_TAG) && !spawningClone) {
				entity.discard();
				return;
			}
			if (entity instanceof TntEntity tnt) {
				Explosions.onTntSpawn(world, tnt);
			}
		});
	}

	public static void onMobInitialize(MobEntity entity, ServerWorld world, SpawnReason reason) {
		if (!WceConfig.MIRROR_MOBS || Wce.paused() || !Wce.ready() || Wce.isApplying() || spawningClone) {
			return;
		}
		if (entity.getCommandTags().contains(WceConfig.CLONE_TAG)) {
			return;
		}
		if (!shouldMirror(reason)) {
			return;
		}
		if (clones.size() >= WceConfig.MAX_MOB_CLONES) {
			return;
		}
		ServerPlayerEntity trigger = MirrorDrops.nearestPlayer(
			world,
			entity.getX(),
			entity.getY(),
			entity.getZ(),
			WceConfig.MOB_TRIGGER_RANGE
		);
		if (trigger == null) {
			return;
		}
		int expire = Wce.currentTick() + WceConfig.MOB_CLONE_SECONDS * 20;
		spawningClone = true;
		try {
			for (int[] offset : MOB_OFFSETS) {
				double tx = entity.getX() + offset[0] * (double) WceConfig.COPY_INTERVAL;
				double ty = entity.getY();
				double tz = entity.getZ() + offset[1] * (double) WceConfig.COPY_INTERVAL;
				double dx = trigger.getX() - tx;
				double dz = trigger.getZ() - tz;
				if (dx * dx + dz * dz > (double) WceConfig.MOB_LOCAL_RANGE * WceConfig.MOB_LOCAL_RANGE) {
					continue;
				}
				try {
					Entity clone = entity.getType().create(world);
					if (!(clone instanceof MobEntity mob)) {
						if (clone != null) {
							clone.discard();
						}
						continue;
					}
					mob.refreshPositionAndAngles(tx, ty, tz, entity.getYaw(), entity.getPitch());
					mob.addCommandTag(WceConfig.CLONE_TAG);
					world.spawnEntity(mob);
					clones.add(new Clone(mob, expire));
				} catch (Exception ignored) {
				}
			}
		} finally {
			spawningClone = false;
		}
	}

	public static void tickClones(MinecraftServer server) {
		if (clones.isEmpty()) {
			return;
		}
		int now = server.getTicks();
		Iterator<Clone> it = clones.iterator();
		while (it.hasNext()) {
			Clone clone = it.next();
			if (now < clone.expireTick) {
				continue;
			}
			try {
				if (clone.entity.isAlive() && !clone.entity.isRemoved()) {
					clone.entity.discard();
				}
			} catch (Exception ignored) {
			}
			it.remove();
		}
	}

	public static int cloneCount() {
		return clones.size();
	}

	private static boolean shouldMirror(SpawnReason reason) {
		return reason == SpawnReason.NATURAL
			|| reason == SpawnReason.BREEDING
			|| reason == SpawnReason.SPAWNER
			|| reason == SpawnReason.SPAWN_EGG
			|| reason == SpawnReason.PATROL
			|| reason == SpawnReason.REINFORCEMENT
			|| reason == SpawnReason.MOB_SUMMONED;
	}

	private record Clone(MobEntity entity, int expireTick) {}
}
