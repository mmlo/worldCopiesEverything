package studio.threedonkeys.wce.recorders;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.GlowItemFrameEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import studio.threedonkeys.wce.Wce;
import studio.threedonkeys.wce.pattern.EditRecord;

import java.util.List;

public final class ItemFrames {
	private ItemFrames() {}

	public static void reset() {
	}

	public static void register() {
		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (world.isClient || Wce.paused() || !Wce.ready() || Wce.isApplying()) {
				return ActionResult.PASS;
			}
			if (!(world instanceof ServerWorld serverWorld) || !(entity instanceof ItemFrameEntity frame)) {
				return ActionResult.PASS;
			}
			Wce.scheduler().runLater(serverWorld.getServer(), 1, () -> {
				if (!frame.isRemoved()) {
					capture(serverWorld, frame);
				}
			});
			return ActionResult.PASS;
		});
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (world.isClient || Wce.paused() || !Wce.ready() || Wce.isApplying()) {
				return ActionResult.PASS;
			}
			if (!(world instanceof ServerWorld serverWorld) || !(entity instanceof ItemFrameEntity frame)) {
				return ActionResult.PASS;
			}
			BlockPos pos = frame.getAttachedBlockPos().toImmutable();
			Wce.scheduler().runLater(serverWorld.getServer(), 1, () -> {
				if (!frame.isRemoved() && frame.isAlive()) {
					capture(serverWorld, frame);
				} else {
					recordRemoval(serverWorld, pos);
				}
			});
			return ActionResult.PASS;
		});
	}

	public static void onPlaced(ServerWorld world, BlockPos around) {
		if (Wce.paused() || !Wce.ready() || Wce.isApplying()) {
			return;
		}
		Wce.scheduler().runLater(world.getServer(), 1, () -> {
			Box box = new Box(around).expand(1.0);
			for (ItemFrameEntity frame : world.getNonSpectatingEntities(ItemFrameEntity.class, box)) {
				capture(world, frame);
			}
		});
	}

	public static void capture(ServerWorld world, ItemFrameEntity frame) {
		BlockPos pos = frame.getAttachedBlockPos();
		NbtCompound nbt = new NbtCompound();
		ItemStack stack = frame.getHeldItemStack();
		if (!stack.isEmpty()) {
			nbt.put("Item", stack.encodeAllowEmpty(world.getRegistryManager()));
		}
		nbt.putByte("ItemRotation", (byte) frame.getRotation());
		nbt.putByte("Facing", (byte) frame.getHorizontalFacing().getId());
		Identifier type = Registries.ENTITY_TYPE.getId(frame.getType());
		Wce.store().recordEntity(world, pos, type, nbt, frame.getHorizontalFacing());
	}

	public static void recordRemoval(ServerWorld world, BlockPos pos) {
		Wce.store().recordAir(world, pos, null);
	}

	public static void stamp(ServerWorld world, BlockPos pos, EditRecord record) {
		if (record.entityType == null) {
			return;
		}
		EntityType<?> type = Registries.ENTITY_TYPE.get(record.entityType);
		if (type != EntityType.ITEM_FRAME && type != EntityType.GLOW_ITEM_FRAME) {
			return;
		}
		Direction facing = record.entityFacing == null ? Direction.NORTH : record.entityFacing;
		ItemFrameEntity existing = find(world, pos, facing, type);
		ItemStack item = ItemStack.EMPTY;
		int rotation = 0;
		if (record.entityNbt != null && record.entityNbt.contains("Item")) {
			item = ItemStack.fromNbtOrEmpty(world.getRegistryManager(), record.entityNbt.getCompound("Item"));
		}
		if (record.entityNbt != null) {
			rotation = record.entityNbt.getByte("ItemRotation");
		}
		if (existing != null) {
			existing.setHeldItemStack(item, false);
			existing.setRotation(rotation);
			return;
		}
		ItemFrameEntity frame = type == EntityType.GLOW_ITEM_FRAME
			? new GlowItemFrameEntity(world, pos, facing)
			: new ItemFrameEntity(world, pos, facing);
		frame.setHeldItemStack(item, false);
		frame.setRotation(rotation);
		world.spawnEntity(frame);
	}

	public static void removeAt(ServerWorld world, BlockPos pos) {
		Box box = new Box(pos);
		List<ItemFrameEntity> frames = world.getNonSpectatingEntities(ItemFrameEntity.class, box.expand(0.1));
		for (ItemFrameEntity frame : frames) {
			if (frame.getAttachedBlockPos().equals(pos)) {
				frame.discard();
			}
		}
	}

	private static ItemFrameEntity find(ServerWorld world, BlockPos pos, Direction facing, EntityType<?> type) {
		Box box = new Box(pos).expand(0.1);
		for (ItemFrameEntity frame : world.getNonSpectatingEntities(ItemFrameEntity.class, box)) {
			if (frame.getAttachedBlockPos().equals(pos)
				&& frame.getHorizontalFacing() == facing
				&& frame.getType() == type) {
				return frame;
			}
		}
		return null;
	}

}
