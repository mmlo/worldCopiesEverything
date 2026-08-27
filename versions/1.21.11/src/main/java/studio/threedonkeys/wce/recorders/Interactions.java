package studio.threedonkeys.wce.recorders;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.JukeboxBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import studio.threedonkeys.wce.Wce;
import studio.threedonkeys.wce.WceConfig;
import studio.threedonkeys.wce.pattern.EditRecord;
import studio.threedonkeys.wce.pattern.PatternStore;

public final class Interactions {
	private Interactions() {}

	public static void register() {
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (!WceConfig.COPY_INTERACTIONS || world.isClient() || Wce.paused() || !Wce.ready() || Wce.isApplying()) {
				return ActionResult.PASS;
			}
			if (!(world instanceof ServerWorld serverWorld)) {
				return ActionResult.PASS;
			}
			BlockHitResult hit = hitResult;
			BlockPos pos = hit.getBlockPos().toImmutable();
			BlockState before = serverWorld.getBlockState(pos);
			ItemStack stack = player.getStackInHand(hand);

			if (WceConfig.COPY_GROWN_STRUCTURES && BlockCats.isBoneMeal(stack) && BlockCats.isGrowableStructure(before.getBlock())) {
				NaturalChanges.armGrowthScan(serverWorld, pos);
				return ActionResult.PASS;
			}

			if (before.getBlock() instanceof AbstractSignBlock) {
				Signs.watch(serverWorld, pos, Signs.snapshot(serverWorld, pos));
			}

			if (WceConfig.COPY_PORTALS && BlockCats.isPortalIgniter(stack)) {
				Portals.scheduleNetherScans(serverWorld, pos.offset(hit.getSide()));
			}
			if (WceConfig.COPY_PORTALS && BlockCats.isEnderEye(stack)) {
				Portals.scheduleEndScans(serverWorld, pos);
			}

			boolean usedItem = !stack.isEmpty();
			BlockPos pairPos = MultiBlocks.companionPos(pos, before);
			BlockState pairBefore = null;
			if (pairPos != null && BlockCats.chunkLoaded(serverWorld, pairPos)) {
				BlockState pair = serverWorld.getBlockState(pairPos);
				if (MultiBlocks.isCompanion(before, pair)) {
					pairBefore = pair;
				}
			}
			BlockPos facePos = pos.offset(hit.getSide());
			BlockState faceBefore = BlockCats.chunkLoaded(serverWorld, facePos) ? serverWorld.getBlockState(facePos) : null;
			BlockState pairBeforeFrozen = pairBefore;
			BlockPos pairPosFrozen = pairPos;
			BlockPos facePosFrozen = facePos;
			BlockState faceBeforeFrozen = faceBefore;
			BlockState beforeFrozen = before;

			Wce.scheduler().runLater(serverWorld.getServer(), 1, () -> {
				if (!BlockCats.chunkLoaded(serverWorld, pos)) {
					return;
				}
				BlockState after = serverWorld.getBlockState(pos);
				BlockState pairAfter = null;
				if (pairBeforeFrozen != null && pairPosFrozen != null && BlockCats.chunkLoaded(serverWorld, pairPosFrozen)) {
					pairAfter = serverWorld.getBlockState(pairPosFrozen);
				}
				boolean nbtMayHaveChanged = BlockCats.needsBlockEntityCopy(after) && usedItem;
				boolean mainChanged = !BlockCats.isSame(beforeFrozen, after) || nbtMayHaveChanged;
				boolean pairChanged = pairBeforeFrozen != null && pairAfter != null && !BlockCats.isSame(pairBeforeFrozen, pairAfter);
				if (mainChanged || pairChanged) {
					EditRecord main = Wce.store().recordEdit(
						serverWorld,
						pos,
						after,
						BlockCats.needsBlockEntityCopy(after) ? null : PatternStore.captureBlockEntity(serverWorld, pos),
						null
					);
					if (main != null && pairAfter != null) {
						Wce.store().recordEdit(serverWorld, pairPosFrozen, pairAfter, PatternStore.captureBlockEntity(serverWorld, pairPosFrozen), main.group);
					}
				}
				if (after.getBlock() instanceof JukeboxBlock) {
					Jukeboxes.onJukeboxChanged(serverWorld, pos);
				}
				if (faceBeforeFrozen != null && BlockCats.chunkLoaded(serverWorld, facePosFrozen)) {
					BlockState faceAfter = serverWorld.getBlockState(facePosFrozen);
					if (!BlockCats.isSame(faceBeforeFrozen, faceAfter)
						&& !BlockCats.needsBlockEntityCopy(faceAfter)
						&& !BlockCats.isFalling(faceAfter.getBlock())) {
						Wce.store().recordEdit(serverWorld, facePosFrozen, faceAfter, PatternStore.captureBlockEntity(serverWorld, facePosFrozen), null);
						if (BlockCats.isFire(faceAfter)) {
							NaturalChanges.watchFire(serverWorld, facePosFrozen);
						}
						if (faceAfter.isOf(net.minecraft.block.Blocks.NETHER_PORTAL) || faceBeforeFrozen.isOf(net.minecraft.block.Blocks.OBSIDIAN)) {
							Portals.scheduleNetherScans(serverWorld, facePosFrozen);
						}
					}
				}
			});
			return ActionResult.PASS;
		});
	}
}
