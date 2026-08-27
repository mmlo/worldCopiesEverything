package studio.threedonkeys.wce.recorders;

import net.minecraft.block.BlockState;
import net.minecraft.entity.TntEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import studio.threedonkeys.wce.Wce;
import studio.threedonkeys.wce.WceConfig;
import studio.threedonkeys.wce.pattern.PatternStore;
import studio.threedonkeys.wce.stamp.ChunkStamper;

import java.util.List;

public final class Explosions {
	private static int explosionTick = -1;
	private static int explosionCount;
	private static Bounds explosionBounds;

	private Explosions() {}

	public static void onExplosion(ServerWorld world, List<BlockPos> affected, List<BlockState> before) {
		if (!WceConfig.COPY_EXPLOSIONS || Wce.paused() || !Wce.ready() || Wce.isApplying()) {
			return;
		}
		int tick = Wce.currentTick();
		if (tick != explosionTick) {
			explosionTick = tick;
			explosionCount = 0;
		}
		for (int i = 0; i < affected.size(); i++) {
			if (++explosionCount > WceConfig.EXPLOSION_BLOCKS_PER_TICK) {
				break;
			}
			BlockPos pos = affected.get(i).toImmutable();
			if (!BlockCats.chunkLoaded(world, pos)) {
				continue;
			}
			BlockState after = world.getBlockState(pos);
			Wce.store().recordEdit(world, pos, after, PatternStore.captureBlockEntity(world, pos), null);
			noteExplodedCell(world, pos);
			if (WceConfig.COPY_PORTALS && i < before.size() && BlockCats.isPortalFrame(before.get(i).getBlock())) {
				Wce.scheduler().runLater(world.getServer(), 2, () -> Portals.clearVanishedPortals(world, pos));
			}
		}
	}

	public static void onTntSpawn(ServerWorld world, TntEntity tnt) {
		if (Wce.paused() || !Wce.ready() || Wce.isApplying()) {
			return;
		}
		BlockPos cell = tnt.getBlockPos().toImmutable();
		Wce.scheduler().runLater(world.getServer(), 1, () -> {
			if (!BlockCats.chunkLoaded(world, cell)) {
				return;
			}
			BlockState state = world.getBlockState(cell);
			if (state.isOf(net.minecraft.block.Blocks.TNT)) {
				return;
			}
			Wce.store().recordEdit(world, cell, state, PatternStore.captureBlockEntity(world, cell), null);
		});
	}

	private static void noteExplodedCell(ServerWorld world, BlockPos location) {
		if (explosionBounds == null || explosionBounds.world != world) {
			explosionBounds = new Bounds(world, location.toImmutable(), location.toImmutable());
			Wce.scheduler().runLater(world.getServer(), 5, () -> {
				Bounds bounds = explosionBounds;
				explosionBounds = null;
				if (bounds == null) {
					return;
				}
				ChunkStamper.reconcileBox(
					bounds.world,
					bounds.from.add(-1, -1, -1),
					bounds.to.add(1, 2, 1)
				);
			});
			return;
		}
		explosionBounds.include(location);
	}

	private static final class Bounds {
		final ServerWorld world;
		BlockPos from;
		BlockPos to;

		Bounds(ServerWorld world, BlockPos from, BlockPos to) {
			this.world = world;
			this.from = from;
			this.to = to;
		}

		void include(BlockPos location) {
			from = new BlockPos(
				Math.min(from.getX(), location.getX()),
				Math.min(from.getY(), location.getY()),
				Math.min(from.getZ(), location.getZ())
			);
			to = new BlockPos(
				Math.max(to.getX(), location.getX()),
				Math.max(to.getY(), location.getY()),
				Math.max(to.getZ(), location.getZ())
			);
		}
	}
}
