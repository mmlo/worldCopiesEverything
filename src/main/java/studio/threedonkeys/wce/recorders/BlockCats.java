package studio.threedonkeys.wce.recorders;

import net.minecraft.block.AbstractBannerBlock;
import net.minecraft.block.AbstractChestBlock;
import net.minecraft.block.AbstractPressurePlateBlock;
import net.minecraft.block.AbstractRedstoneGateBlock;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.AbstractSkullBlock;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.CocoaBlock;
import net.minecraft.block.ConcretePowderBlock;
import net.minecraft.block.CropBlock;
import net.minecraft.block.DaylightDetectorBlock;
import net.minecraft.block.DecoratedPotBlock;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.FungusBlock;
import net.minecraft.block.JukeboxBlock;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.NetherWartBlock;
import net.minecraft.block.ObserverBlock;
import net.minecraft.block.PitcherCropBlock;
import net.minecraft.block.PropaguleBlock;
import net.minecraft.block.RedstoneBlock;
import net.minecraft.block.RedstoneLampBlock;
import net.minecraft.block.RedstoneOreBlock;
import net.minecraft.block.RedstoneTorchBlock;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.SaplingBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.SpawnerBlock;
import net.minecraft.block.StemBlock;
import net.minecraft.block.SweetBerryBushBlock;
import net.minecraft.block.TripwireHookBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class BlockCats {
	private BlockCats() {}

	public static boolean isContainer(Block block) {
		return block instanceof AbstractChestBlock
			|| block instanceof BarrelBlock
			|| block instanceof ShulkerBoxBlock;
	}

	public static boolean isInventoryHolder(BlockState state) {
		return isContainer(state.getBlock()) || state.hasBlockEntity();
	}

	public static boolean needsBlockEntityCopy(BlockState state) {
		Block block = state.getBlock();
		return block instanceof AbstractBannerBlock
			|| block instanceof AbstractSkullBlock
			|| block instanceof DecoratedPotBlock
			|| block instanceof LecternBlock
			|| block instanceof JukeboxBlock
			|| block instanceof SpawnerBlock
			|| block instanceof AbstractSignBlock;
	}

	public static boolean isFluid(BlockState state) {
		return state.isOf(Blocks.WATER) || state.isOf(Blocks.LAVA);
	}

	public static boolean isRedstoneSignal(Block block) {
		return block instanceof LeverBlock
			|| block instanceof ButtonBlock
			|| block instanceof AbstractPressurePlateBlock
			|| block instanceof RedstoneTorchBlock
			|| block instanceof RedstoneWireBlock
			|| block instanceof AbstractRedstoneGateBlock
			|| block instanceof DaylightDetectorBlock
			|| block instanceof TripwireHookBlock
			|| block instanceof ObserverBlock
			|| block instanceof RedstoneBlock
			|| block instanceof RedstoneLampBlock
			|| block instanceof RedstoneOreBlock
			|| block == Blocks.REDSTONE_WALL_TORCH;
	}

	public static boolean isFalling(Block block) {
		return block instanceof FallingBlock
			|| block instanceof AnvilBlock
			|| block instanceof ConcretePowderBlock
			|| block == Blocks.SCAFFOLDING;
	}

	public static boolean isCrop(Block block) {
		return block instanceof CropBlock
			|| block instanceof NetherWartBlock
			|| block instanceof SweetBerryBushBlock
			|| block instanceof CocoaBlock
			|| block instanceof PitcherCropBlock
			|| block instanceof StemBlock;
	}

	public static boolean isGrowableStructure(Block block) {
		return block instanceof SaplingBlock
			|| block instanceof FungusBlock
			|| block instanceof PropaguleBlock
			|| block == Blocks.BROWN_MUSHROOM
			|| block == Blocks.RED_MUSHROOM
			|| block == Blocks.AZALEA
			|| block == Blocks.FLOWERING_AZALEA
			|| block == Blocks.BAMBOO
			|| block == Blocks.BAMBOO_SAPLING;
	}

	public static boolean isColumnDependent(Block block) {
		return block == Blocks.SCAFFOLDING
			|| block == Blocks.CACTUS
			|| block == Blocks.SUGAR_CANE
			|| block == Blocks.BAMBOO
			|| block == Blocks.KELP
			|| block == Blocks.KELP_PLANT;
	}

	public static boolean isColumnGrower(Block block) {
		return block == Blocks.CACTUS
			|| block == Blocks.SUGAR_CANE
			|| block == Blocks.KELP
			|| block == Blocks.KELP_PLANT;
	}

	public static boolean isPortalFrame(Block block) {
		return block == Blocks.OBSIDIAN || block == Blocks.END_PORTAL_FRAME;
	}

	public static boolean isNetherPortal(BlockState state) {
		return state.isOf(Blocks.NETHER_PORTAL);
	}

	public static boolean isEndPortal(BlockState state) {
		return state.isOf(Blocks.END_PORTAL);
	}

	public static boolean isFire(BlockState state) {
		return state.isOf(Blocks.FIRE) || state.isOf(Blocks.SOUL_FIRE);
	}

	public static boolean isBoneMeal(ItemStack stack) {
		return stack.isOf(Items.BONE_MEAL);
	}

	public static boolean isPortalIgniter(ItemStack stack) {
		return stack.isOf(Items.FLINT_AND_STEEL) || stack.isOf(Items.FIRE_CHARGE);
	}

	public static boolean isEnderEye(ItemStack stack) {
		return stack.isOf(Items.ENDER_EYE);
	}

	public static boolean isMusicDisc(ItemStack stack) {
		return stack.getItem() instanceof net.minecraft.item.MusicDiscItem;
	}

	public static void clearInventoryAt(World world, BlockPos pos) {
		BlockEntity be = world.getBlockEntity(pos);
		if (be instanceof LootableContainerBlockEntity loot) {
			loot.setLootTable(null, 0L);
		}
		if (be instanceof Inventory inventory) {
			inventory.clear();
			be.markDirty();
		}
	}

	public static String id(Block block) {
		return Registries.BLOCK.getId(block).toString();
	}

	public static boolean isSame(BlockState a, BlockState b) {
		return a == b || a.equals(b);
	}

	public static boolean chunkLoaded(ServerWorld world, BlockPos pos) {
		return world.isChunkLoaded(pos) && world.isInBuildLimit(pos);
	}

	public static boolean chunkLoaded(ServerWorld world, int chunkX, int chunkZ) {
		return world.getChunkManager().isChunkLoaded(chunkX, chunkZ);
	}
}
