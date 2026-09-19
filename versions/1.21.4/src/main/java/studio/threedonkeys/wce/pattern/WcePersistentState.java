package studio.threedonkeys.wce.pattern;

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

	private static final PersistentState.Type<WcePersistentState> TYPE = new PersistentState.Type<>(
		WcePersistentState::new,
		WcePersistentState::fromNbt,
		null
	);

	public static WcePersistentState get(MinecraftServer server) {
		ServerWorld overworld = server.getWorld(World.OVERWORLD);
		if (overworld == null) {
			return null;
		}
		return overworld.getPersistentStateManager().getOrCreate(TYPE, ID);
	}

	public static WcePersistentState fromNbt(NbtCompound nbt, net.minecraft.registry.RegistryWrapper.WrapperLookup registryLookup) {
		WcePersistentState state = new WcePersistentState();
		if (!nbt.contains("edits", 9)) {
			return state;
		}
		NbtList list = nbt.getList("edits", NbtElement.COMPOUND_TYPE);
		EditRecord previous = null;
		state.store.setLastSeq(-1);
		for (int i = 0; i < list.size(); i++) {
			NbtCompound entry = list.getCompound(i);
			String dimId = entry.getString("d");
			int cx = entry.getInt("x");
			int y = entry.getInt("y");
			int cz = entry.getInt("z");
			String key = dimId + "|" + cx + "," + y + "," + cz;
			long seq = state.store.lastSeq() + 1;
			state.store.setLastSeq(seq);
			boolean grouped = entry.getBoolean("q") && previous != null;
			EditRecord record = new EditRecord(
				key,
				seq,
				grouped ? previous.group : seq,
				dimId,
				cx,
				y,
				cz,
				PatternStore.nbtToState(entry.getCompound("s"))
			);
			if (entry.contains("n", NbtElement.COMPOUND_TYPE)) {
				record.blockEntityNbt = entry.getCompound("n");
			}
			if (entry.contains("e", NbtElement.STRING_TYPE)) {
				record.entityType = Identifier.tryParse(entry.getString("e"));
			}
			if (entry.contains("en", NbtElement.COMPOUND_TYPE)) {
				record.entityNbt = entry.getCompound("en");
			}
			if (entry.contains("f", NbtElement.STRING_TYPE)) {
				try {
					record.entityFacing = Direction.byName(entry.getString("f"));
				} catch (Exception ignored) {
				}
			}
			if (entry.contains("u")) {
				record.sx = Math.max(1, entry.getInt("u"));
			}
			if (entry.contains("v")) {
				record.sz = Math.max(1, entry.getInt("v"));
			}
			EditRecord old = state.store.pattern().put(key, record);
			if (old != null) {
				state.store.kill(old);
			}
			state.store.editLog().add(record);
			previous = record;
		}
		long savedSeq = nbt.getLong("lastSeq");
		if (savedSeq > state.store.lastSeq()) {
			state.store.setLastSeq(savedSeq);
		}
		Wce.LOGGER.info("[WCE] Restored {} pattern edits from the world save.", state.store.size());
		return state;
	}

	@Override
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
