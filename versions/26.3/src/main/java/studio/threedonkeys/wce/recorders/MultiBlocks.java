package studio.threedonkeys.wce.recorders;

import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import studio.threedonkeys.wce.Wce;
import studio.threedonkeys.wce.pattern.EditRecord;
import studio.threedonkeys.wce.pattern.PatternStore;

public final class MultiBlocks {
	private MultiBlocks() {}

	public static boolean isMulti(BlockState state) {
		return state.contains(Properties.DOUBLE_BLOCK_HALF) || state.contains(BedBlock.PART);
	}

	public static BlockPos companionPos(BlockPos pos, BlockState state) {
		if (state.contains(Properties.DOUBLE_BLOCK_HALF)) {
			DoubleBlockHalf half = state.get(Properties.DOUBLE_BLOCK_HALF);
			return half == DoubleBlockHalf.UPPER ? pos.down() : pos.up();
		}
		if (state.contains(BedBlock.PART) && state.contains(BedBlock.FACING)) {
			BedPart part = state.get(BedBlock.PART);
			Direction facing = state.get(BedBlock.FACING);
			return part == BedPart.HEAD ? pos.offset(facing.getOpposite()) : pos.offset(facing);
		}
		return null;
	}

	public static boolean isCompanion(BlockState origin, BlockState candidate) {
		if (candidate.getBlock() != origin.getBlock()) {
			return false;
		}
		if (origin.contains(Properties.DOUBLE_BLOCK_HALF) && candidate.contains(Properties.DOUBLE_BLOCK_HALF)) {
			return origin.get(Properties.DOUBLE_BLOCK_HALF) != candidate.get(Properties.DOUBLE_BLOCK_HALF);
		}
		if (origin.contains(BedBlock.PART) && candidate.contains(BedBlock.PART)
			&& origin.contains(BedBlock.FACING) && candidate.contains(BedBlock.FACING)) {
			return origin.get(BedBlock.PART) != candidate.get(BedBlock.PART)
				&& origin.get(BedBlock.FACING) == candidate.get(BedBlock.FACING);
		}
		return false;
	}

	public static void recordCompanionOfPlacement(ServerWorld world, BlockPos pos, BlockState state, long group, int retries) {
		if (!isMulti(state)) {
			return;
		}
		BlockPos pairPos = companionPos(pos, state);
		BlockState pairState = null;
		if (pairPos != null && BlockCats.chunkLoaded(world, pairPos)) {
			BlockState at = world.getBlockState(pairPos);
			if (isCompanion(state, at)) {
				pairState = at;
			}
		}
		if (pairState == null && state.contains(BedBlock.PART)) {
			for (Direction dir : new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
				BlockPos candidatePos = pos.offset(dir);
				if (!BlockCats.chunkLoaded(world, candidatePos)) {
					continue;
				}
				BlockState candidate = world.getBlockState(candidatePos);
				if (isCompanion(state, candidate)) {
					pairPos = candidatePos;
					pairState = candidate;
					break;
				}
			}
		}
		if (pairState == null) {
			if (retries > 0) {
				BlockPos origin = pos.toImmutable();
				BlockState snapshot = state;
				Wce.scheduler().runLater(world.getServer(), 1,
					() -> recordCompanionOfPlacement(world, origin, snapshot, group, retries - 1));
			}
			return;
		}
		captureHalf(world, pairPos, pairState, group);
	}

	private static void captureHalf(ServerWorld world, BlockPos pos, BlockState state, long group) {
		if (BlockCats.needsBlockEntityCopy(state)) {
			Wce.store().recordEdit(world, pos, state, null, group);
		} else {
			Wce.store().recordEdit(world, pos, state, PatternStore.captureBlockEntity(world, pos), group);
		}
	}

	public static void recordBreak(ServerWorld world, BlockPos pos, BlockState broken) {
		PatternStore store = Wce.store();
		BlockPos pairPos = companionPos(pos, broken);
		if (pairPos == null) {
			store.recordAir(world, pos, null);
			return;
		}
		boolean pairConfirmed = broken.contains(Properties.DOUBLE_BLOCK_HALF);
		if (!pairConfirmed && BlockCats.chunkLoaded(world, pairPos)) {
			BlockState pair = world.getBlockState(pairPos);
			pairConfirmed = pair.isAir() || isCompanion(broken, pair);
		}
		if (!pairConfirmed) {
			store.recordAir(world, pos, null);
			return;
		}
		boolean upperIsPair = broken.contains(Properties.DOUBLE_BLOCK_HALF)
			&& broken.get(Properties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;
		BlockPos first = upperIsPair ? pairPos : pos;
		BlockPos second = upperIsPair ? pos : pairPos;
		EditRecord firstRecord = store.recordEdit(world, first, Blocks.AIR.getDefaultState(), null, null);
		long group = firstRecord == null ? store.lastSeq() : firstRecord.group;
		store.recordAir(world, second, group);
	}

	public static void recordPair(ServerWorld world, BlockPos pos, BlockState state, Long existingGroup) {
		EditRecord main = Wce.store().recordEdit(world, pos, state, PatternStore.captureBlockEntity(world, pos), existingGroup);
		if (main != null) {
			recordCompanionOfPlacement(world, pos, state, main.group, 1);
		} else if (isMulti(state)) {
			Wce.scheduler().runLater(world.getServer(), 2, () -> {
				EditRecord later = Wce.store().getLiveAt(world, pos);
				if (later != null) {
					recordCompanionOfPlacement(world, pos, world.getBlockState(pos), later.group, 1);
				}
			});
		}
	}
}
