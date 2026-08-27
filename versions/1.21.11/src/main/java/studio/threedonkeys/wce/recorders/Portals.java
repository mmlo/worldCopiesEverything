package studio.threedonkeys.wce.recorders;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import studio.threedonkeys.wce.Wce;
import studio.threedonkeys.wce.WceConfig;
import studio.threedonkeys.wce.pattern.EditRecord;
import studio.threedonkeys.wce.pattern.PatternStore;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public final class Portals {
	private Portals() {}

	public static void recordNetherPortal(ServerWorld world, BlockPos origin) {
		if (!WceConfig.COPY_PORTALS) {
			return;
		}
		try {
			List<BlockPos> starts = List.of(
				origin,
				origin.down(),
				origin.up(),
				origin.east(),
				origin.west(),
				origin.south(),
				origin.north()
			);
			Queue<BlockPos> queue = new ArrayDeque<>();
			Set<BlockPos> seen = new HashSet<>();
			for (BlockPos start : starts) {
				if (BlockCats.chunkLoaded(world, start) && world.getBlockState(start).isOf(Blocks.NETHER_PORTAL) && seen.add(start.toImmutable())) {
					queue.add(start.toImmutable());
				}
			}
			List<Found> found = new ArrayList<>();
			while (!queue.isEmpty() && found.size() < WceConfig.MAX_PORTAL_BLOCKS) {
				BlockPos loc = queue.poll();
				if (!BlockCats.chunkLoaded(world, loc)) {
					continue;
				}
				BlockState state = world.getBlockState(loc);
				if (!state.isOf(Blocks.NETHER_PORTAL)) {
					continue;
				}
				found.add(new Found(loc, state));
				for (Direction dir : Direction.values()) {
					BlockPos next = loc.offset(dir);
					if (seen.add(next.toImmutable())) {
						queue.add(next.toImmutable());
					}
				}
			}
			if (!found.isEmpty()) {
				recordPortalBlocks(world, found);
			}
		} catch (Exception ignored) {
		}
	}

	public static void recordEndPortal(ServerWorld world, BlockPos center) {
		if (!WceConfig.COPY_PORTALS) {
			return;
		}
		List<Found> found = new ArrayList<>();
		for (int y = center.getY() - 1; y <= center.getY() + 1 && found.size() < WceConfig.MAX_PORTAL_BLOCKS; y++) {
			for (int x = center.getX() - 4; x <= center.getX() + 4; x++) {
				for (int z = center.getZ() - 4; z <= center.getZ() + 4; z++) {
					BlockPos pos = new BlockPos(x, y, z);
					if (!BlockCats.chunkLoaded(world, pos)) {
						continue;
					}
					BlockState state = world.getBlockState(pos);
					if (state.isOf(Blocks.END_PORTAL)) {
						found.add(new Found(pos, state));
					}
				}
			}
		}
		if (!found.isEmpty()) {
			recordPortalBlocks(world, found);
		}
	}

	public static void clearVanishedPortals(ServerWorld world, BlockPos center) {
		if (!WceConfig.COPY_PORTALS) {
			return;
		}
		for (int y = center.getY() - 4; y <= center.getY() + 24; y++) {
			for (int x = center.getX() - 5; x <= center.getX() + 5; x++) {
				for (int z = center.getZ() - 5; z <= center.getZ() + 5; z++) {
					BlockPos pos = new BlockPos(x, y, z);
					EditRecord record = Wce.store().getLiveAt(world, pos);
					if (record == null) {
						continue;
					}
					if (!record.state.isOf(Blocks.NETHER_PORTAL) && !record.state.isOf(Blocks.END_PORTAL)) {
						continue;
					}
					if (!BlockCats.chunkLoaded(world, pos)) {
						continue;
					}
					BlockState actual = world.getBlockState(pos);
					if (actual.getBlock() == record.state.getBlock()) {
						continue;
					}
					Wce.store().recordEdit(world, pos, actual, PatternStore.captureBlockEntity(world, pos), null);
				}
			}
		}
	}

	public static void scheduleNetherScans(ServerWorld world, BlockPos ignite) {
		BlockPos frozen = ignite.toImmutable();
		for (int delay : new int[] {2, 10, 40}) {
			Wce.scheduler().runLater(world.getServer(), delay, () -> recordNetherPortal(world, frozen));
		}
	}

	public static void scheduleEndScans(ServerWorld world, BlockPos center) {
		BlockPos frozen = center.toImmutable();
		for (int delay : new int[] {2, 10, 40}) {
			Wce.scheduler().runLater(world.getServer(), delay, () -> recordEndPortal(world, frozen));
		}
	}

	private static void recordPortalBlocks(ServerWorld world, List<Found> blocks) {
		boolean complete = true;
		for (Found entry : blocks) {
			EditRecord existing = Wce.store().getLiveAt(world, entry.pos);
			if (existing == null || existing.state.getBlock() != entry.state.getBlock()) {
				complete = false;
				break;
			}
		}
		if (complete) {
			return;
		}
		for (Found entry : blocks) {
			Wce.store().killAt(world, entry.pos);
		}
		Long group = null;
		for (Found entry : blocks) {
			EditRecord record = Wce.store().recordEdit(world, entry.pos, entry.state, null, group);
			if (record != null && group == null) {
				group = record.group;
			}
		}
	}

	private record Found(BlockPos pos, BlockState state) {}
}
