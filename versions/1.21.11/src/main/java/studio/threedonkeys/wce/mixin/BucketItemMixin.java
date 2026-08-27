package studio.threedonkeys.wce.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BucketItem;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import studio.threedonkeys.wce.Wce;
import studio.threedonkeys.wce.recorders.PlaceBreak;

@Mixin(BucketItem.class)
public abstract class BucketItemMixin {
	@Inject(method = "placeFluid", at = @At("RETURN"))
	private void wce$placeFluid(@Nullable PlayerEntity player, World world, BlockPos pos, @Nullable BlockHitResult hitResult, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValue() || player == null || world.isClient() || Wce.isApplying()) {
			return;
		}
		if (world instanceof ServerWorld serverWorld) {
			PlaceBreak.onFluidPlaced(serverWorld, pos);
		}
	}
}
