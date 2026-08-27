package studio.threedonkeys.wce;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import studio.threedonkeys.wce.command.WceCommands;
import studio.threedonkeys.wce.recorders.Interactions;
import studio.threedonkeys.wce.recorders.ItemFrames;
import studio.threedonkeys.wce.recorders.Mobs;
import studio.threedonkeys.wce.recorders.PlaceBreak;
import studio.threedonkeys.wce.stamp.MirrorDrops;

public final class WorldCopiesEverything implements ModInitializer {
	@Override
	public void onInitialize() {
		PlaceBreak.register();
		Interactions.register();
		ItemFrames.register();
		MirrorDrops.register();
		Mobs.register();
		WceCommands.register();

		ServerLifecycleEvents.SERVER_STARTED.register(Wce::onServerStarted);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> Wce.onServerStopped());
		ServerTickEvents.END_SERVER_TICK.register(Wce::tick);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> WceCommands.sendWelcome(handler.player));

		Wce.LOGGER.info("[WCE] World Copies Everything ready.");
	}
}
