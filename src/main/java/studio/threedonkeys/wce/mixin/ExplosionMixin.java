package studio.threedonkeys.wce.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import studio.threedonkeys.wce.recorders.Explosions;

import java.util.ArrayList;
import java.util.List;

@Mixin(Explosion.class)
public abstract class ExplosionMixin {
	@Shadow
	@Final
	private World world;

	@Unique
	private List<BlockState> wce$before;

	@Inject(method = "affectWorld", at = @At("HEAD"))
	private void wce$before(boolean particles, CallbackInfo ci) {
		if (this.world.isClient) {
			return;
		}
		List<BlockPos> affected = ((Explosion) (Object) this).getAffectedBlocks();
		this.wce$before = new ArrayList<>(affected.size());
		for (BlockPos pos : affected) {
			this.wce$before.add(this.world.getBlockState(pos));
		}
	}

	@Inject(method = "affectWorld", at = @At("RETURN"))
	private void wce$after(boolean particles, CallbackInfo ci) {
		if (this.world.isClient || !(this.world instanceof ServerWorld serverWorld)) {
			return;
		}
		Explosions.onExplosion(serverWorld, ((Explosion) (Object) this).getAffectedBlocks(), this.wce$before == null ? List.of() : this.wce$before);
		this.wce$before = null;
	}
}
