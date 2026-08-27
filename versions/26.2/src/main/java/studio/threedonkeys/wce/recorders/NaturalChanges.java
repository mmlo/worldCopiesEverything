package studio.threedonkeys.wce.recorders;

import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import studio.threedonkeys.wce.Wce;
import studio.threedonkeys.wce.WceConfig;
import studio.threedonkeys.wce.pattern.PatternStore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class NaturalChanges {
	private static final Set<String> growthScansActive = new HashSet<>();
	private static final List<Region> regionWatches = new ArrayList<>();

	private NaturalChanges() {}

	public static void reset() {
		growthScansActive.clear();
		regionWatches.clear();
	}

	public static void armGrowthScan(ServerWorld world, BlockPos origin) {
		if (!WceConfig.COPY_GROWN_STRUCTURES) {
			return;
		}
		String key = Wce.cellKey(Wce.dimId(world), origin.getX(), origin.getY(), origin.getZ());
		if (!growthScansActive.add(key)) {
			return;
		}
		int minX = origin.getX() - WceConfig.TREE_SCAN_RADIUS;
		int minZ = origin.getZ() - WceConfig.TREE_SCAN_RADIUS;
		int minY = origin.getY() - WceConfig.TREE_SCAN_DOWN;
		int maxX = origin.getX() + WceConfig.TREE_SCAN_RADIUS;
		int maxZ = origin.getZ() + WceConfig.TREE_SCAN_RADIUS;
		int maxY = origin.getY() + WceConfig.TREE_SCAN_UP;
		List<BlockState> before = new ArrayList<>();
		List<BlockPos> positions = new ArrayList<>();
		for (int y = minY; y <= maxY; y++) {
			for (int x = minX; x <= maxX; x++) {
				for (int z = minZ; z <= maxZ; z++) {
					BlockPos pos = new BlockPos(x, y, z);
					positions.add(pos);
					if (BlockCats.chunkLoaded(world, pos)) {
						before.add(world.getBlockState(pos));
					} else {
						before.add(null);
					}
				}
			}
		}
		Wce.scheduler().runLater(world.getServer(), 2, () -> {
			growthScansActive.remove(key);
			for (int i = 0; i < positions.size(); i++) {
				BlockState prev = before.get(i);
				if (prev == null) {
					continue;
				}
				BlockPos pos = positions.get(i);
				if (!BlockCats.chunkLoaded(world, pos)) {
					continue;
				}
				BlockState current = world.getBlockState(pos);
				if (!BlockCats.isSame(prev, current)) {
					Wce.store().recordEdit(world, pos, current, PatternStore.captureBlockEntity(world, pos), null);
				}
			}
		});
	}

	public static void watchRegion(ServerWorld world, BlockPos from, BlockPos to, int seconds) {
		if (!WceConfig.COPY_NATURAL_CHANGES) {
			return;
		}
		List<Cell> cells = new ArrayList<>();
		for (int y = from.getY(); y <= to.getY(); y++) {
			for (int x = from.getX(); x <= to.getX(); x++) {
				for (int z = from.getZ(); z <= to.getZ(); z++) {
					BlockPos pos = new BlockPos(x, y, z);
					if (BlockCats.chunkLoaded(world, pos)) {
						cells.add(new Cell(pos, world.getBlockState(pos)));
					}
				}
			}
		}
		if (cells.isEmpty()) {
			return;
		}
		regionWatches.add(new Region(world, cells, Wce.currentTick() + seconds * 20));
		if (regionWatches.size() > WceConfig.MAX_WATCH_REGIONS) {
			regionWatches.remove(0);
		}
	}

	public static void watchFire(ServerWorld world, BlockPos origin) {
		int r = WceConfig.FIRE_WATCH_RADIUS;
		watchRegion(world, origin.add(-r, -r, -r), origin.add(r, r, r), WceConfig.FIRE_WATCH_SECONDS);
	}

	public static void watchCrop(ServerWorld world, BlockPos pos) {
		watchRegion(world, pos, pos, WceConfig.CROP_WATCH_SECONDS);
	}

	public static void watchColumnGrower(ServerWorld world, BlockPos pos) {
		watchRegion(world, pos, pos.up(3), WceConfig.CROP_WATCH_SECONDS);
	}

	public static void watchSapling(ServerWorld world, BlockPos pos) {
		watchRegion(world, pos.add(-5, -1, -5), pos.add(5, 25, 5), WceConfig.SAPLING_WATCH_SECONDS);
	}

	public static void tick(MinecraftServer server) {
		if (Wce.paused() || regionWatches.isEmpty()) {
			return;
		}
		int now = server.getTicks();
		int budget = WceConfig.WATCH_CELLS_PER_POLL;
		for (int r = regionWatches.size() - 1; r >= 0; r--) {
			Region region = regionWatches.get(r);
			if (now > region.expireTick) {
				regionWatches.remove(r);
				continue;
			}
			if (budget <= 0) {
				break;
			}
			List<Cell> cells = region.cells;
			int checked = 0;
			while (checked < cells.size() && budget > 0) {
				Cell cell = cells.get(region.cursor % cells.size());
				region.cursor++;
				checked++;
				budget--;
				if (!BlockCats.chunkLoaded(region.world, cell.pos)) {
					continue;
				}
				BlockState current = region.world.getBlockState(cell.pos);
				if (!BlockCats.isSame(cell.perm, current)) {
					cell.perm = current;
					region.expireTick = now + 60 * 20;
					Wce.store().recordEdit(region.world, cell.pos, current, PatternStore.captureBlockEntity(region.world, cell.pos), null);
				}
			}
		}
	}

	private static final class Region {
		final ServerWorld world;
		final List<Cell> cells;
		int expireTick;
		int cursor;

		Region(ServerWorld world, List<Cell> cells, int expireTick) {
			this.world = world;
			this.cells = cells;
			this.expireTick = expireTick;
		}
	}

	private static final class Cell {
		final BlockPos pos;
		BlockState perm;

		Cell(BlockPos pos, BlockState perm) {
			this.pos = pos;
			this.perm = perm;
		}
	}
}
