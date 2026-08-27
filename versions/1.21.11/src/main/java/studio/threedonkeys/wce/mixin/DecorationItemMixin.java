package studio.threedonkeys.wce.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DecorationItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import studio.threedonkeys.wce.Wce;
import studio.threedonkeys.wce.recorders.ItemFrames;

@Mixin(DecorationItem.class)
public abstract class DecorationItemMixin {
	@Inject(method = "useOnBlock", at = @At("RETURN"))
	private void wce$onUse(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
		if (!cir.getReturnValue().isAccepted()) {
			return;
		}
		World world = context.getWorld();
		PlayerEntity player = context.getPlayer();
		ItemStack stack = context.getStack();
		if (player == null || world.isClient || Wce.isApplying()) {
			return;
		}
		if (!stack.isOf(Items.ITEM_FRAME) && !stack.isOf(Items.GLOW_ITEM_FRAME)) {
			return;
		}
		if (world instanceof ServerWorld serverWorld) {
			BlockPos placed = context.getBlockPos().offset(context.getSide());
			ItemFrames.onPlaced(serverWorld, placed);
		}
	}
}
