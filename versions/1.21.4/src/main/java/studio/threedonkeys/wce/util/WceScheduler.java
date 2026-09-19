package studio.threedonkeys.wce.util;

import net.minecraft.server.MinecraftServer;
import studio.threedonkeys.wce.Wce;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class WceScheduler {
	private final List<Task> tasks = new ArrayList<>();
	private final Queue<Task> incoming = new ConcurrentLinkedQueue<>();

	public void runLater(MinecraftServer server, int ticks, Runnable runnable) {
		if (server == null || runnable == null) {
			return;
		}
		if (ticks <= 0) {
			runnable.run();
			return;
		}
		incoming.add(new Task(server.getTicks() + ticks, runnable));
	}

	public void tick(MinecraftServer server) {
		Task pending;
		while ((pending = incoming.poll()) != null) {
			tasks.add(pending);
		}
		if (tasks.isEmpty()) {
			return;
		}
		int now = server.getTicks();
		List<Task> toRun = new ArrayList<>();
		Iterator<Task> iterator = tasks.iterator();
		while (iterator.hasNext()) {
			Task task = iterator.next();
			if (now >= task.runAt) {
				iterator.remove();
				toRun.add(task);
			}
		}
		for (Task task : toRun) {
			try {
				task.runnable.run();
			} catch (Throwable t) {
				Wce.LOGGER.error("[WCE] Error executing scheduled task", t);
			}
		}
	}

	public void clear() {
		incoming.clear();
		tasks.clear();
	}

	private record Task(int runAt, Runnable runnable) {}
}
