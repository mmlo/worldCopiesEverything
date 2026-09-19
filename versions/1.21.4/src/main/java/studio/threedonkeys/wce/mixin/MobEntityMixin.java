package studio.threedonkeys.wce.mixin;

import net.minecraft.entity.EntityData;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import studio.threedonkeys.wce.recorders.Mobs;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin {
	@Inject(method = "initialize", at = @At("RETURN"))
	private void wce$initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, CallbackInfoReturnable<EntityData> cir) {
		if (!(world instanceof ServerWorld serverWorld)) {
			return;
		}
		Mobs.onMobInitialize((MobEntity) (Object) this, serverWorld, spawnReason);
	}
}
