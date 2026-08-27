package studio.threedonkeys.wce.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import studio.threedonkeys.wce.Wce;
import studio.threedonkeys.wce.recorders.PlaceBreak;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
	@Inject(method = "place", at = @At("RETURN"))
	private void wce$onPlace(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir) {
		if (!cir.getReturnValue().isAccepted()) {
			return;
		}
		World world = context.getWorld();
		if (world.isClient() || !(world instanceof ServerWorld serverWorld) || Wce.isApplying()) {
			return;
		}
		PlayerEntity player = context.getPlayer();
		if (player == null) {
			return;
		}
		PlaceBreak.onPlayerPlace(serverWorld, context.getBlockPos());
	}
}
