package studio.threedonkeys.wce.stamp;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import studio.threedonkeys.wce.Wce;
import studio.threedonkeys.wce.WceConfig;
import studio.threedonkeys.wce.pattern.EditRecord;
import studio.threedonkeys.wce.pattern.PatternStore;
import studio.threedonkeys.wce.recorders.BlockCats;
import studio.threedonkeys.wce.recorders.ItemFrames;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ChunkStamper {
	private static final List<int[]> PATCH_OFFSETS = buildOffsets();
	private static final Map<String, Long> patchedChunks = new LinkedHashMap<>(256, 0.75f, true);
	private static final Map<String, Integer> chunkProbeBackoff = new LinkedHashMap<>();
	private static int patrolRotation;

	private ChunkStamper() {}

	public static void resetSession() {
		patchedChunks.clear();
		chunkProbeBackoff.clear();
		patrolRotation = 0;
	}

	private static List<int[]> buildOffsets() {
		List<int[]> list = new ArrayList<>();
		int r = WceConfig.PATCH_CHUNK_RADIUS;
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				list.add(new int[] {dx, dz});
			}
		}
		list.sort(Comparator.comparingInt(a -> a[0] * a[0] + a[1] * a[1]));
		return List.copyOf(list);
	}

	private static void touchPatched(String chunkKey, long seq) {
		patchedChunks.put(chunkKey, seq);
		while (patchedChunks.size() > WceConfig.PATCHED_LRU_MAX) {
			String first = patchedChunks.keySet().iterator().next();
			patchedChunks.remove(first);
		}
	}

	public static void patrol(MinecraftServer server) {
		PatternStore store = Wce.store();
		if (Wce.paused() || store.isEmpty()) {
			return;
		}
		List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
		if (players.isEmpty()) {
			return;
		}
		int perPlayer = Math.max(20, WceConfig.PATCH_WRITES_PER_SWEEP / players.size());
		patrolRotation = (patrolRotation + 1) % players.size();
		int now = server.getTicks();
		for (int i = 0; i < players.size(); i++) {
			ServerPlayerEntity player = players.get((patrolRotation + i) % players.size());
			int budget = perPlayer;
			try {
				ServerWorld world = player.getEntityWorld();
				int pcx = player.getBlockPos().getX() >> 4;
				int pcz = player.getBlockPos().getZ() >> 4;
				String dim = Wce.dimId(world);
				for (int[] offset : PATCH_OFFSETS) {
					if (budget <= 0) {
						break;
					}
					int cx = pcx + offset[0];
					int cz = pcz + offset[1];
					String chunkKey = Wce.chunkKey(dim, cx, cz);
					Integer backoffUntil = chunkProbeBackoff.get(chunkKey);
					if (backoffUntil != null) {
						if (now < backoffUntil) {
							continue;
						}
						chunkProbeBackoff.remove(chunkKey);
					}
					budget -= patchChunk(world, cx, cz, budget);
				}
			} catch (Exception ignored) {
			}
		}
	}

	public static int patchChunk(ServerWorld world, int chunkX, int chunkZ, int budget) {
		PatternStore store = Wce.store();
		String chunkKey = Wce.chunkKey(Wce.dimId(world), chunkX, chunkZ);
		long appliedSeq = patchedChunks.getOrDefault(chunkKey, -1L);
		if (appliedSeq >= store.lastSeq()) {
			return 0;
		}
		int minX = chunkX * 16;
		int minZ = chunkZ * 16;
		int writes = 0;
		long currentGroup = Long.MIN_VALUE;
		List<EditRecord> log = store.editLog();
		for (int i = store.lowerBound(appliedSeq + 1); i < log.size(); i++) {
			EditRecord record = log.get(i);
			if (record.dead || !record.dimId.equals(Wce.dimId(world))) {
				appliedSeq = record.seq;
				continue;
			}
			if (writes >= budget && record.group != currentGroup) {
				break;
			}
			boolean chunkLoaded = true;
			int firstX = minX + Math.floorMod(record.cx - minX, WceConfig.COPY_INTERVAL);
			int firstZ = minZ + Math.floorMod(record.cz - minZ, WceConfig.COPY_INTERVAL);
			for (int x = firstX; x < minX + 16 && chunkLoaded; x += WceConfig.COPY_INTERVAL) {
				for (int z = firstZ; z < minZ + 16; z += WceConfig.COPY_INTERVAL) {
					BlockPos pos = new BlockPos(x, record.y, z);
					if (!world.isInBuildLimit(pos) || !BlockCats.chunkLoaded(world, chunkX, chunkZ)) {
						chunkLoaded = false;
						break;
					}
					applyRecord(world, pos, record);
					writes++;
				}
			}
			if (!chunkLoaded) {
				chunkProbeBackoff.put(chunkKey, Wce.currentTick() + WceConfig.PROBE_BACKOFF_TICKS);
				if (chunkProbeBackoff.size() > 20000) {
					chunkProbeBackoff.clear();
				}
				touchPatched(chunkKey, appliedSeq);
				return writes;
			}
			appliedSeq = record.seq;
			currentGroup = record.group;
		}
		touchPatched(chunkKey, appliedSeq);
		return writes;
	}

	public static void applyRecord(ServerWorld world, BlockPos pos, EditRecord record) {
		Wce.pushApplying();
		try {
			if (record.isEntity()) {
				ItemFrames.stamp(world, pos, record);
				return;
			}
			ItemFrames.removeAt(world, pos);
			BlockState current = world.getBlockState(pos);
			BlockState wanted = record.state;
			boolean permutationSaysItAll = !record.needsExactData();
			if (permutationSaysItAll && BlockCats.isSame(current, wanted)) {
				return;
			}
			if (!wanted.isAir() && BlockCats.isColumnDependent(wanted.getBlock())) {
				BlockState below = world.getBlockState(pos.down());
				if (below.isAir()) {
					return;
				}
			}
			boolean unchanged = permutationSaysItAll && BlockCats.isSame(current, wanted);
			if (!unchanged) {
				if (BlockCats.isContainer(current.getBlock()) && (wanted.isAir() || current.getBlock() != wanted.getBlock())) {
					BlockCats.clearInventoryAt(world, pos);
				}
				if (wanted.isOf(Blocks.NETHER_PORTAL)) {
					stampNetherPortal(world, pos, current);
				} else {
					int flags = Block.NOTIFY_LISTENERS | Block.SKIP_DROPS;
					if (BlockCats.isFluid(wanted)
						|| (WceConfig.COPY_REDSTONE && BlockCats.isRedstoneSignal(wanted.getBlock()))) {
						flags |= Block.NOTIFY_NEIGHBORS;
					}
					world.setBlockState(pos, wanted, flags);
					if (record.blockEntityNbt != null) {
						PatternStore.applyBlockEntity(world, pos, record.blockEntityNbt);
					}
					if (BlockCats.isFluid(wanted)) {
						world.scheduleFluidTick(pos, wanted.getFluidState().getFluid(), wanted.getFluidState().getFluid().getTickRate(world));
					}
				}
				MirrorDrops.noteWrite(world, pos);
			}
		} catch (Exception ignored) {
		} finally {
			Wce.popApplying();
		}
	}

	private static void stampNetherPortal(ServerWorld world, BlockPos pos, BlockState current) {
		if (current.isOf(Blocks.NETHER_PORTAL)) {
			return;
		}
		if (current.isAir()) {
			BlockState below = world.getBlockState(pos.down());
			if (below.isOf(Blocks.OBSIDIAN)) {
				world.setBlockState(pos, Blocks.FIRE.getDefaultState(), Block.NOTIFY_ALL);
			}
		}
	}

	public static int reconcileBox(ServerWorld world, BlockPos from, BlockPos to) {
		List<BlockPos> cells = new ArrayList<>();
		for (int y = from.getY(); y <= to.getY(); y++) {
			for (int x = from.getX(); x <= to.getX(); x++) {
				for (int z = from.getZ(); z <= to.getZ(); z++) {
					cells.add(new BlockPos(x, y, z));
				}
			}
		}
		return reconcileCells(world, cells);
	}

	public static int reconcileAround(ServerWorld world, BlockPos origin) {
		List<BlockPos> cells = new ArrayList<>();
		for (int dy = 1; dy <= WceConfig.COLLAPSE_SCAN_HEIGHT; dy++) {
			cells.add(origin.up(dy));
		}
		cells.add(origin.north());
		cells.add(origin.south());
		cells.add(origin.east());
		cells.add(origin.west());
		cells.add(origin.down());
		return reconcileCells(world, cells);
	}

	public static int reconcileCells(ServerWorld world, List<BlockPos> cells) {
		int corrected = 0;
		PatternStore store = Wce.store();
		for (BlockPos cell : cells) {
			EditRecord record = store.getLiveAt(world, cell);
			if (record == null || record.isAir()) {
				continue;
			}
			if (!BlockCats.chunkLoaded(world, cell)) {
				continue;
			}
			BlockState actual = world.getBlockState(cell);
			if (record.isEntity()) {
				continue;
			}
			if (actual.getBlock() == record.state.getBlock()) {
				continue;
			}
			store.recordEdit(world, cell, actual, PatternStore.captureBlockEntity(world, cell), null);
			corrected++;
		}
		return corrected;
	}

}
