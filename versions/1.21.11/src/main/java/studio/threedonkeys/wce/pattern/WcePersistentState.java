package studio.threedonkeys.wce.pattern;
import net.minecraft.world.PersistentStateType;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import studio.threedonkeys.wce.Wce;

public final class WcePersistentState extends PersistentState {
	public static final String ID = "world_copies_everything";

	public final PatternStore store = new PatternStore();

	public WcePersistentState() {
		store.setDirtyCallback(this::markDirty);
	}

	private static final PersistentStateType<WcePersistentState> TYPE = new PersistentStateType<>(
		ID,
		WcePersistentState::new,
		net.minecraft.nbt.NbtCompound.CODEC.xmap(nbt -> WcePersistentState.fromNbt(nbt, null), state -> state.writeNbt(new NbtCompound(), null)),
		net.minecraft.datafixer.DataFixTypes.LEVEL
	);

	public static WcePersistentState get(MinecraftServer server) {
		ServerWorld overworld = server.getWorld(World.OVERWORLD);
		if (overworld == null) {
			return null;
		}
		return overworld.getPersistentStateManager().getOrCreate(TYPE);
	}

	public static WcePersistentState fromNbt(NbtCompound nbt, net.minecraft.registry.RegistryWrapper.WrapperLookup registryLookup) {
		WcePersistentState state = new WcePersistentState();
		if (!nbt.contains("edits")) {
			return state;
		}
		NbtList list = nbt.getListOrEmpty("edits");
		EditRecord previous = null;
		state.store.setLastSeq(-1);
		for (int i = 0; i < list.size(); i++) {
			NbtCompound entry = list.getCompoundOrEmpty(i);
			String dimId = entry.getString("d", "");
			int cx = entry.getInt("x", 0);
			int y = entry.getInt("y", 0);
			int cz = entry.getInt("z", 0);
			String key = dimId + "|" + cx + "," + y + "," + cz;
			long seq = state.store.lastSeq() + 1;
			state.store.setLastSeq(seq);
			boolean grouped = entry.getBoolean("q", false) && previous != null;
			EditRecord record = new EditRecord(
				key,
				seq,
				grouped ? previous.group : seq,
				dimId,
				cx,
				y,
				cz,
				PatternStore.nbtToState(entry.getCompound("s").orElse(null))
			);
			if (entry.contains("n")) {
				record.blockEntityNbt = entry.getCompound("n").orElse(null);
			}
			if (entry.contains("e")) {
				record.entityType = Identifier.tryParse(entry.getString("e", ""));
			}
			if (entry.contains("en")) {
				record.entityNbt = entry.getCompound("en").orElse(null);
			}
			if (entry.contains("f")) {
				try {
					record.entityFacing = Direction.byId(entry.getString("f", "north"));
				} catch (Exception ignored) {
				}
			}
			if (entry.contains("u")) {
				record.sx = Math.max(1, entry.getInt("u", 1));
			}
			if (entry.contains("v")) {
				record.sz = Math.max(1, entry.getInt("v", 1));
			}
			EditRecord old = state.store.pattern().put(key, record);
			if (old != null) {
				state.store.kill(old);
			}
			state.store.editLog().add(record);
			previous = record;
		}
		long savedSeq = nbt.getLong("lastSeq", 0L);
		if (savedSeq > state.store.lastSeq()) {
			state.store.setLastSeq(savedSeq);
		}
		Wce.LOGGER.info("[WCE] Restored {} pattern edits from the world save.", state.store.size());
		return state;
	}

	
	public NbtCompound writeNbt(NbtCompound nbt, net.minecraft.registry.RegistryWrapper.WrapperLookup registryLookup) {
		nbt.putLong("lastSeq", store.lastSeq());
		NbtList list = new NbtList();
		EditRecord previousLive = null;
		for (EditRecord record : store.editLog()) {
			if (record.dead) {
				continue;
			}
			NbtCompound entry = new NbtCompound();
			entry.putString("d", record.dimId);
			entry.putInt("x", record.cx);
			entry.putInt("y", record.y);
			entry.putInt("z", record.cz);
			entry.put("s", PatternStore.stateToNbt(record.state));
			if (record.blockEntityNbt != null) {
				entry.put("n", record.blockEntityNbt.copy());
			}
			if (record.entityType != null) {
				entry.putString("e", record.entityType.toString());
			}
			if (record.entityNbt != null) {
				entry.put("en", record.entityNbt.copy());
			}
			if (record.entityFacing != null) {
				entry.putString("f", record.entityFacing.asString());
			}
			if (previousLive != null && previousLive.group == record.group) {
				entry.putBoolean("q", true);
			}
			if (record.sx > 1) {
				entry.putInt("u", record.sx);
			}
			if (record.sz > 1) {
				entry.putInt("v", record.sz);
			}
			list.add(entry);
			previousLive = record;
		}
		nbt.put("edits", list);
		return nbt;
	}
}
