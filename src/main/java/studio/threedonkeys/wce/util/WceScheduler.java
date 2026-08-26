package studio.threedonkeys.wce.util;

import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class WceScheduler {
	private final List<Task> tasks = new ArrayList<>();

	public void runLater(MinecraftServer server, int ticks, Runnable runnable) {
		if (server == null || runnable == null) {
			return;
		}
		if (ticks <= 0) {
			runnable.run();
			return;
		}
		tasks.add(new Task(server.getTicks() + ticks, runnable));
	}

	public void tick(MinecraftServer server) {
		if (tasks.isEmpty()) {
			return;
		}
		int now = server.getTicks();
		Iterator<Task> iterator = tasks.iterator();
		while (iterator.hasNext()) {
			Task task = iterator.next();
			if (now >= task.runAt) {
				iterator.remove();
				try {
					task.runnable.run();
				} catch (Exception ignored) {
				}
			}
		}
	}

	public void clear() {
		tasks.clear();
	}

	private record Task(int runAt, Runnable runnable) {}
}
