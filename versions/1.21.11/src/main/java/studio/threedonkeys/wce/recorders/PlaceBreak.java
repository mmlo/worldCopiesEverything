package studio.threedonkeys.wce.recorders;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.JukeboxBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import studio.threedonkeys.wce.Wce;
import studio.threedonkeys.wce.WceConfig;
import studio.threedonkeys.wce.pattern.EditRecord;
import studio.threedonkeys.wce.pattern.PatternStore;
import studio.threedonkeys.wce.stamp.ChunkStamper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PlaceBreak {
	private static final Map<String, List<BlockPos>> pendingCollapse = new HashMap<>();

	private PlaceBreak() {}

	public static void register() {
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
			if (Wce.paused() || world.isClient() || !(world instanceof ServerWorld serverWorld)) {
				return true;
			}
			if (!BlockCats.isColumnDependent(state.getBlock())) {
				return true;
			}
			List<BlockPos> column = new ArrayList<>();
			for (int dy = 1; dy <= WceConfig.COLLAPSE_SCAN_HEIGHT; dy++) {
				BlockPos cell = pos.up(dy);
				if (!BlockCats.chunkLoaded(serverWorld, cell)) {
					break;
				}
				if (serverWorld.getBlockState(cell).getBlock() != state.getBlock()) {
					break;
				}
				column.add(cell.toImmutable());
			}
			if (!column.isEmpty()) {
				if (pendingCollapse.size() > 64) {
					pendingCollapse.clear();
				}
				pendingCollapse.put(Wce.cellKey(Wce.dimId(serverWorld), pos.getX(), pos.getY(), pos.getZ()), column);
			}
			return true;
		});

		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			if (Wce.paused() || world.isClient() || !(world instanceof ServerWorld serverWorld) || !Wce.ready()) {
				return;
			}
			onPlayerBreak(serverWorld, pos.toImmutable(), state);
		});
	}

	public static void onPlayerPlace(ServerWorld world, BlockPos pos) {
		if (Wce.paused() || !Wce.ready() || Wce.isApplying()) {
			return;
		}
		BlockPos frozen = pos.toImmutable();
		BlockState placed = world.getBlockState(frozen);
		if (placed.isAir()) {
			return;
		}
		if (BlockCats.isFalling(placed.getBlock())) {
			Wce.scheduler().runLater(world.getServer(), 2, () -> {
				if (!BlockCats.chunkLoaded(world, frozen)) {
					return;
				}
				BlockState still = world.getBlockState(frozen);
				if (still.getBlock() != placed.getBlock()) {
					return;
				}
				Wce.store().recordEdit(world, frozen, still, PatternStore.captureBlockEntity(world, frozen), null);
			});
			return;
		}
		boolean sign = placed.getBlock() instanceof AbstractSignBlock;
		EditRecord recorded = Wce.store().recordEdit(
			world,
			frozen,
			placed,
			sign || BlockCats.needsBlockEntityCopy(placed) ? null : PatternStore.captureBlockEntity(world, frozen),
			null
		);
		if (sign) {
			Signs.watch(world, frozen, Signs.snapshot(world, frozen));
		}
		if (recorded != null) {
			MultiBlocks.recordCompanionOfPlacement(world, frozen, placed, recorded.group, 1);
		} else if (MultiBlocks.isMulti(placed)) {
			Wce.scheduler().runLater(world.getServer(), 2, () -> {
				EditRecord later = Wce.store().getLiveAt(world, frozen);
				if (later != null) {
					MultiBlocks.recordCompanionOfPlacement(world, frozen, world.getBlockState(frozen), later.group, 1);
				}
			});
		}
		if (BlockCats.isCrop(placed.getBlock())) {
			NaturalChanges.watchCrop(world, frozen);
		} else if (BlockCats.isColumnGrower(placed.getBlock())) {
			NaturalChanges.watchColumnGrower(world, frozen);
		} else if (BlockCats.isGrowableStructure(placed.getBlock())) {
			NaturalChanges.watchSapling(world, frozen);
		}
	}

	public static void onPlayerBreak(ServerWorld world, BlockPos pos, BlockState broken) {
		if (broken.getBlock() instanceof JukeboxBlock) {
			Jukeboxes.stopEcho(world, pos);
		}
		String collapseKey = Wce.cellKey(Wce.dimId(world), pos.getX(), pos.getY(), pos.getZ());
		List<BlockPos> column = pendingCollapse.remove(collapseKey);
		if (column != null) {
			recordCollapsedColumn(world, pos, column);
		} else {
			MultiBlocks.recordBreak(world, pos, broken);
		}
		Wce.scheduler().runLater(world.getServer(), 3, () -> ChunkStamper.reconcileAround(world, pos));
		if (WceConfig.COPY_PORTALS && BlockCats.isPortalFrame(broken.getBlock())) {
			Wce.scheduler().runLater(world.getServer(), 2, () -> Portals.clearVanishedPortals(world, pos));
		}
	}

	private static void recordCollapsedColumn(ServerWorld world, BlockPos broken, List<BlockPos> column) {
		Long group = null;
		for (int i = column.size() - 1; i >= 0; i--) {
			EditRecord record = Wce.store().recordEdit(world, column.get(i), Blocks.AIR.getDefaultState(), null, group);
			if (record != null && group == null) {
				group = record.group;
			}
		}
		Wce.store().recordAir(world, broken, group);
	}

	public static void onFluidPlaced(ServerWorld world, BlockPos pos) {
		if (Wce.paused() || !Wce.ready() || Wce.isApplying()) {
			return;
		}
		BlockPos frozen = pos.toImmutable();
		Wce.scheduler().runLater(world.getServer(), 1, () -> {
			if (!BlockCats.chunkLoaded(world, frozen)) {
				return;
			}
			BlockState state = world.getBlockState(frozen);
			if (state.isAir()) {
				return;
			}
			Wce.store().recordEdit(world, frozen, state, null, null);
			if (BlockCats.isFire(state)) {
				NaturalChanges.watchFire(world, frozen);
			}
		});
	}
}
