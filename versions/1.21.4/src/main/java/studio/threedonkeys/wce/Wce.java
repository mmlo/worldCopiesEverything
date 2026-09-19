package studio.threedonkeys.wce;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.threedonkeys.wce.pattern.PatternStore;
import studio.threedonkeys.wce.pattern.WcePersistentState;
import studio.threedonkeys.wce.recorders.ItemFrames;
import studio.threedonkeys.wce.recorders.Jukeboxes;
import studio.threedonkeys.wce.recorders.Mobs;
import studio.threedonkeys.wce.recorders.NaturalChanges;
import studio.threedonkeys.wce.recorders.Signs;
import studio.threedonkeys.wce.stamp.ChunkStamper;
import studio.threedonkeys.wce.stamp.MirrorDrops;
import studio.threedonkeys.wce.util.WceScheduler;

public final class Wce {
	public static final String MOD_ID = "world_copies_everything";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final ThreadLocal<Integer> APPLYING = ThreadLocal.withInitial(() -> 0);

	private static MinecraftServer server;
	private static WcePersistentState persistent;
	private static boolean paused;
	private static final WceScheduler scheduler = new WceScheduler();

	private Wce() {}

	public static void onServerStarted(MinecraftServer started) {
		server = started;
		persistent = WcePersistentState.get(started);
		paused = false;
		ChunkStamper.resetSession();
		MirrorDrops.reset();
		Mobs.reset();
		Signs.reset();
		NaturalChanges.reset();
		Jukeboxes.reset();
		ItemFrames.reset();
		LOGGER.info("[WCE] Loaded {} pattern edits.", store().size());
	}

	public static void onServerStopped() {
		scheduler.clear();
		persistent = null;
		server = null;
		paused = false;
		ChunkStamper.resetSession();
		MirrorDrops.reset();
		Mobs.reset();
		Signs.reset();
		NaturalChanges.reset();
		Jukeboxes.reset();
		ItemFrames.reset();
	}

	public static void tick(MinecraftServer ticking) {
		if (ticking != server || persistent == null) {
			return;
		}
		int ticks = ticking.getTicks();
		scheduler.tick(ticking);
		if (!paused) {
			if (ticks % WceConfig.SIGN_CHECK_INTERVAL == 0) {
				Signs.tick(ticking);
			}
			if (ticks % WceConfig.WATCH_POLL_INTERVAL == 0) {
				NaturalChanges.tick(ticking);
			}
			if (ticks % WceConfig.PATCH_INTERVAL == 0) {
				ChunkStamper.patrol(ticking);
			}
			if (ticks % 20 == 0) {
				Mobs.tickClones(ticking);
			}
		}
		if (ticks % WceConfig.SAVE_INTERVAL == 0) {
			store().flushDirty();
		}
		MirrorDrops.tick(ticks);
	}

	public static MinecraftServer server() {
		return server;
	}

	public static boolean ready() {
		return server != null && persistent != null;
	}

	public static PatternStore store() {
		return persistent == null ? PatternStore.EMPTY : persistent.store;
	}

	public static WcePersistentState persistent() {
		return persistent;
	}

	public static WceScheduler scheduler() {
		return scheduler;
	}

	public static boolean paused() {
		return paused;
	}

	public static void setPaused(boolean value) {
		paused = value;
	}

	public static boolean isApplying() {
		return APPLYING.get() > 0;
	}

	public static void pushApplying() {
		APPLYING.set(APPLYING.get() + 1);
	}

	public static void popApplying() {
		int depth = APPLYING.get() - 1;
		if (depth <= 0) {
			APPLYING.remove();
		} else {
			APPLYING.set(depth);
		}
	}

	public static int currentTick() {
		return server == null ? 0 : server.getTicks();
	}

	public static String dimId(ServerWorld world) {
		return world.getRegistryKey().getValue().toString();
	}

	public static ServerWorld worldOf(String dimId) {
		if (server == null) {
			return null;
		}
		Identifier id = Identifier.tryParse(dimId);
		if (id == null) {
			return null;
		}
		return server.getWorld(RegistryKey.of(RegistryKeys.WORLD, id));
	}

	public static ServerWorld overworld() {
		return server == null ? null : server.getWorld(World.OVERWORLD);
	}

	public static int floorMod(int value, int m) {
		return Math.floorMod(value, m);
	}

	public static String patternKey(String dimId, int x, int y, int z) {
		return dimId + "|" + floorMod(x, WceConfig.COPY_INTERVAL) + "," + y + "," + floorMod(z, WceConfig.COPY_INTERVAL);
	}

	public static String cellKey(String dimId, int x, int y, int z) {
		return dimId + "|" + x + "," + y + "," + z;
	}

	public static String chunkKey(String dimId, int chunkX, int chunkZ) {
		return dimId + "|" + chunkX + "," + chunkZ;
	}
}
