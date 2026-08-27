package studio.threedonkeys.wce;

/**
 * Config — everything you'd want to tweak for a video, matching the Bedrock pack.
 */
public final class WceConfig {
	private WceConfig() {}

	/**
	 * Distance in blocks between pattern repeats on the X and Z axes.
	 * 16 → repeats every chunk (the classic look).
	 */
	public static final int COPY_INTERVAL = 16;

	/**
	 * How far around each player the patrol reaches, in chunks.
	 * Java can write into any loaded chunk; 24 saturates typical simulation distance.
	 */
	public static final int PATCH_CHUNK_RADIUS = 24;
	public static final int PATCH_INTERVAL = 4;
	public static final int PATCH_WRITES_PER_SWEEP = 200;
	public static final int PROBE_BACKOFF_TICKS = 20;

	public static final int PATTERN_MAX_EDITS = 12000;
	public static final int SAVE_INTERVAL = 200;

	public static final boolean COPY_INTERACTIONS = true;

	public static final boolean COPY_GROWN_STRUCTURES = true;
	public static final int TREE_SCAN_RADIUS = 6;
	public static final int TREE_SCAN_UP = 30;
	public static final int TREE_SCAN_DOWN = 2;

	public static final boolean MIRROR_MOBS = true;
	public static final int MOB_COPY_RADIUS = 1;
	public static final int MOB_CLONE_SECONDS = 60;
	public static final int MAX_MOB_CLONES = 64;
	public static final int MOB_TRIGGER_RANGE = 80;
	public static final int MOB_LOCAL_RANGE = 64;

	public static final int SIGN_WATCH_SECONDS = 60;
	public static final int SIGN_CHECK_INTERVAL = 5;

	public static final boolean COPY_NATURAL_CHANGES = true;
	public static final int WATCH_POLL_INTERVAL = 20;
	public static final int WATCH_CELLS_PER_POLL = 1500;
	public static final int MAX_WATCH_REGIONS = 24;
	public static final int FIRE_WATCH_RADIUS = 4;
	public static final int FIRE_WATCH_SECONDS = 120;
	public static final int CROP_WATCH_SECONDS = 600;
	public static final int SAPLING_WATCH_SECONDS = 300;

	public static final int JUKEBOX_ECHO_CHUNKS = 4;

	public static final boolean COPY_PORTALS = true;
	public static final int MAX_PORTAL_BLOCKS = 800;

	public static final boolean COPY_EXPLOSIONS = true;
	public static final int EXPLOSION_BLOCKS_PER_TICK = 512;

	/**
	 * Copy the SWITCH (lever/button/wire) so each copy's own pistons fire.
	 * Never copy piston-moved blocks — the two sides would fight.
	 */
	public static final boolean COPY_REDSTONE = true;

	public static final int MIRROR_DROP_WINDOW = 20;
	public static final int MIRROR_DROP_PLAYER_RANGE = 8;
	public static final int COLLAPSE_SCAN_HEIGHT = 64;

	public static final String CLONE_TAG = "wce_clone";
	public static final int PATCHED_LRU_MAX = 30000;
}
