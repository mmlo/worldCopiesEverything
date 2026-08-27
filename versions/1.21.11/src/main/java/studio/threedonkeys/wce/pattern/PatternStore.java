package studio.threedonkeys.wce.pattern;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import studio.threedonkeys.wce.Wce;
import studio.threedonkeys.wce.WceConfig;
import studio.threedonkeys.wce.recorders.BlockCats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PatternStore {
	public static final PatternStore EMPTY = new PatternStore();

	private final Map<String, EditRecord> pattern = new HashMap<>();
	private final List<EditRecord> editLog = new ArrayList<>();
	private long lastSeq = -1;
	private int deadCount;
	private int dropScanIndex;
	private boolean dirty;
	private Runnable dirtyCallback = () -> {};

	public void setDirtyCallback(Runnable callback) {
		this.dirtyCallback = callback == null ? () -> {} : callback;
	}

	public Map<String, EditRecord> pattern() {
		return pattern;
	}

	public List<EditRecord> editLog() {
		return editLog;
	}

	public long lastSeq() {
		return lastSeq;
	}

	public void setLastSeq(long seq) {
		this.lastSeq = seq;
	}

	public int size() {
		return pattern.size();
	}

	public boolean isEmpty() {
		return pattern.isEmpty();
	}

	public void markDirty() {
		dirty = true;
		dirtyCallback.run();
	}

	public void flushDirty() {
		if (!dirty) {
			return;
		}
		dirty = false;
		dirtyCallback.run();
	}

	public EditRecord getLive(String key) {
		EditRecord record = pattern.get(key);
		return record != null && !record.dead ? record : null;
	}

	public EditRecord getLiveAt(ServerWorld world, BlockPos pos) {
		return getLive(Wce.patternKey(Wce.dimId(world), pos.getX(), pos.getY(), pos.getZ()));
	}

	public void killAt(ServerWorld world, BlockPos pos) {
		String key = Wce.patternKey(Wce.dimId(world), pos.getX(), pos.getY(), pos.getZ());
		EditRecord record = pattern.get(key);
		if (record != null) {
			kill(record);
			pattern.remove(key);
		}
	}

	public void kill(EditRecord record) {
		if (record.dead) {
			return;
		}
		record.dead = true;
		deadCount++;
	}

	public int reset() {
		int forgotten = pattern.size();
		for (EditRecord record : editLog) {
			if (!record.dead) {
				kill(record);
			}
		}
		pattern.clear();
		editLog.clear();
		deadCount = 0;
		dropScanIndex = 0;
		markDirty();
		dirty = false;
		return forgotten;
	}

	/**
	 * Store one block change into the infinite pattern. Pass {@code group} to bind
	 * this edit to another record atomically (doors, beds, portals, columns).
	 */
	public EditRecord recordEdit(ServerWorld world, BlockPos pos, BlockState state, NbtCompound blockEntityNbt, Long group) {
		if (world == null || pos == null) {
			return null;
		}
		if (state == null) {
			state = Blocks.AIR.getDefaultState();
		}
		boolean needsBe = BlockCats.needsBlockEntityCopy(state);
		if (needsBe && blockEntityNbt == null) {
			scheduleNbtCapture(world, pos.toImmutable(), state, group);
			return null;
		}
		String dimId = Wce.dimId(world);
		int cx = Wce.floorMod(pos.getX(), WceConfig.COPY_INTERVAL);
		int cz = Wce.floorMod(pos.getZ(), WceConfig.COPY_INTERVAL);
		String key = Wce.patternKey(dimId, pos.getX(), pos.getY(), pos.getZ());
		EditRecord old = pattern.get(key);
		if (old != null) {
			if (!old.dead && !needsBe && old.entityType == null
				&& BlockCats.isSame(old.state, state)
				&& nbtEqual(old.blockEntityNbt, blockEntityNbt)) {
				return old;
			}
			kill(old);
		}
		long seq = ++lastSeq;
		EditRecord record = new EditRecord(key, seq, group != null ? group : seq, dimId, cx, pos.getY(), cz, state);
		record.blockEntityNbt = blockEntityNbt == null ? null : blockEntityNbt.copy();
		pattern.put(key, record);
		editLog.add(record);
		markDirty();
		if (pattern.size() > WceConfig.PATTERN_MAX_EDITS) {
			dropOldest();
		}
		compactIfNeeded();
		return record;
	}

	public EditRecord recordEntity(ServerWorld world, BlockPos pos, net.minecraft.util.Identifier entityType,
		NbtCompound entityNbt, net.minecraft.util.math.Direction facing) {
		if (world == null || pos == null || entityType == null) {
			return null;
		}
		String dimId = Wce.dimId(world);
		int cx = Wce.floorMod(pos.getX(), WceConfig.COPY_INTERVAL);
		int cz = Wce.floorMod(pos.getZ(), WceConfig.COPY_INTERVAL);
		String key = Wce.patternKey(dimId, pos.getX(), pos.getY(), pos.getZ());
		EditRecord old = pattern.get(key);
		if (old != null) {
			kill(old);
		}
		long seq = ++lastSeq;
		EditRecord record = new EditRecord(key, seq, seq, dimId, cx, pos.getY(), cz, Blocks.AIR.getDefaultState());
		record.entityType = entityType;
		record.entityNbt = entityNbt == null ? null : entityNbt.copy();
		record.entityFacing = facing;
		pattern.put(key, record);
		editLog.add(record);
		markDirty();
		if (pattern.size() > WceConfig.PATTERN_MAX_EDITS) {
			dropOldest();
		}
		compactIfNeeded();
		return record;
	}

	public void recordAir(ServerWorld world, BlockPos pos, Long group) {
		recordEdit(world, pos, Blocks.AIR.getDefaultState(), null, group);
	}

	public int lowerBound(long targetSeq) {
		int lo = 0;
		int hi = editLog.size();
		while (lo < hi) {
			int mid = (lo + hi) >>> 1;
			if (editLog.get(mid).seq < targetSeq) {
				lo = mid + 1;
			} else {
				hi = mid;
			}
		}
		return lo;
	}

	private void scheduleNbtCapture(ServerWorld world, BlockPos pos, BlockState expected, Long group) {
		Wce.scheduler().runLater(world.getServer(), 1, () -> {
			if (!BlockCats.chunkLoaded(world, pos)) {
				return;
			}
			BlockState now = world.getBlockState(pos);
			if (now.getBlock() != expected.getBlock()) {
				return;
			}
			NbtCompound nbt = captureBlockEntity(world, pos);
			recordEdit(world, pos, now, nbt, group);
		});
	}

	public static NbtCompound captureBlockEntity(ServerWorld world, BlockPos pos) {
		BlockEntity be = world.getBlockEntity(pos);
		if (be == null) {
			return null;
		}
		if (BlockCats.isContainer(world.getBlockState(pos).getBlock())) {
			return null;
		}
		NbtCompound nbt = be.createNbt(world.getRegistryManager());
		nbt.remove("x");
		nbt.remove("y");
		nbt.remove("z");
		return nbt;
	}

	public static void applyBlockEntity(ServerWorld world, BlockPos pos, NbtCompound nbt) {
		if (nbt == null) {
			return;
		}
		BlockEntity be = world.getBlockEntity(pos);
		if (be == null) {
			return;
		}
		NbtCompound copy = nbt.copy();
		copy.putInt("x", pos.getX());
		copy.putInt("y", pos.getY());
		copy.putInt("z", pos.getZ());
		BlockEntity newBe = BlockEntity.createFromNbt(pos, world.getBlockState(pos), copy, world.getRegistryManager());
		if (newBe != null) {
			world.removeBlockEntity(pos);
			world.addBlockEntity(newBe);
			be = newBe;
		}
		be.markDirty();
		world.updateListeners(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
		var packet = be.toUpdatePacket();
		if (packet != null) {
			world.getChunkManager().markForUpdate(pos);
		}
	}

	private void dropOldest() {
		while (dropScanIndex < editLog.size()) {
			EditRecord record = editLog.get(dropScanIndex++);
			if (!record.dead) {
				kill(record);
				pattern.remove(record.key);
				return;
			}
		}
	}

	private void compactIfNeeded() {
		if (deadCount < 1000 || deadCount * 2 < editLog.size()) {
			return;
		}
		int write = 0;
		for (int read = 0; read < editLog.size(); read++) {
			if (!editLog.get(read).dead) {
				editLog.set(write++, editLog.get(read));
			}
		}
		if (write < editLog.size()) {
			editLog.subList(write, editLog.size()).clear();
		}
		deadCount = 0;
		dropScanIndex = 0;
	}

	private static boolean nbtEqual(NbtCompound a, NbtCompound b) {
		if (a == b) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		return a.equals(b);
	}

	public static NbtCompound stateToNbt(BlockState state) {
		return NbtHelper.fromBlockState(state);
	}

	public static BlockState nbtToState(NbtCompound nbt) {
		if (nbt == null) {
			return Blocks.AIR.getDefaultState();
		}
		return NbtHelper.toBlockState(Registries.BLOCK, nbt);
	}
}
