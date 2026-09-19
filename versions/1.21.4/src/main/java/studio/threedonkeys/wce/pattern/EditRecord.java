package studio.threedonkeys.wce.pattern;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

public final class EditRecord {
	public final String key;
	public final long seq;
	public final long group;
	public final String dimId;
	public final int cx;
	public final int y;
	public final int cz;
	public BlockState state;
	public NbtCompound blockEntityNbt;
	public Identifier entityType;
	public NbtCompound entityNbt;
	public Direction entityFacing;
	public int sx = 1;
	public int sz = 1;
	public boolean dead;

	public EditRecord(String key, long seq, long group, String dimId, int cx, int y, int cz, BlockState state) {
		this.key = key;
		this.seq = seq;
		this.group = group;
		this.dimId = dimId;
		this.cx = cx;
		this.y = y;
		this.cz = cz;
		this.state = state == null ? Blocks.AIR.getDefaultState() : state;
	}

	public boolean isAir() {
		return state.isAir() && entityType == null;
	}

	public boolean isEntity() {
		return entityType != null;
	}

	public boolean needsExactData() {
		return blockEntityNbt != null || entityType != null;
	}
}
