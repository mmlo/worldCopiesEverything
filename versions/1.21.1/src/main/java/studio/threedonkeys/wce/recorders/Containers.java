package studio.threedonkeys.wce.recorders;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import studio.threedonkeys.wce.Wce;
import studio.threedonkeys.wce.WceConfig;
import studio.threedonkeys.wce.pattern.PatternStore;

import java.util.HashSet;
import java.util.Set;

public final class Containers {
	private static final Set<String> pending = new HashSet<>();

	private Containers() {}

	public static void reset() {
		pending.clear();
	}

	public static void onContainerChanged(ServerWorld world, BlockPos pos) {
		if (!WceConfig.COPY_CONTAINER_INVENTORIES || Wce.paused() || !Wce.ready() || Wce.isApplying()) {
			return;
		}
		String cellKey = Wce.cellKey(Wce.dimId(world), pos.getX(), pos.getY(), pos.getZ());
		if (!pending.add(cellKey)) {
			return;
		}
		BlockPos frozenPos = pos.toImmutable();
		Wce.scheduler().runLater(world.getServer(), 2, () -> {
			pending.remove(cellKey);
			if (!BlockCats.chunkLoaded(world, frozenPos)) {
				return;
			}
			BlockState current = world.getBlockState(frozenPos);
			if (!BlockCats.isContainer(current.getBlock())) {
				return;
			}
			NbtCompound nbt = PatternStore.captureBlockEntity(world, frozenPos);
			Wce.store().recordEdit(world, frozenPos, current, nbt, null);
		});
	}
}
