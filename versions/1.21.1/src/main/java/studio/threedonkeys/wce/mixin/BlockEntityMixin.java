package studio.threedonkeys.wce.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import studio.threedonkeys.wce.Wce;
import studio.threedonkeys.wce.WceConfig;
import studio.threedonkeys.wce.recorders.BlockCats;
import studio.threedonkeys.wce.recorders.Containers;

@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin {
	@Inject(method = "markDirty", at = @At("RETURN"))
	private void wce$onMarkDirty(CallbackInfo ci) {
		if (!WceConfig.COPY_CONTAINER_INVENTORIES || Wce.paused() || !Wce.ready() || Wce.isApplying()) {
			return;
		}
		BlockEntity be = (BlockEntity)(Object)this;
		World world = be.getWorld();
		if (world instanceof ServerWorld serverWorld) {
			BlockState state = be.getCachedState();
			if (state != null && BlockCats.isContainer(state.getBlock())) {
				Containers.onContainerChanged(serverWorld, be.getPos());
			}
		}
	}
}
