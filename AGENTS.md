<project>
  <name>World Copies Everything</name>
  <environment>Minecraft Fabric Mod (Java)</environment>
  <description>
    A Java port of a Bedrock script. Any block a player places, breaks, or interacts with is recorded and stamped infinitely across the world, repeating every 16 blocks (X and Z axes).
  </description>
</project>

<architecture>
  <core_loop>
    1. Player interacts/places/breaks a block.
    2. Recorders (in `recorders/` package) capture the change, resolving groups (like 2-block beds) and NBT.
    3. The change is saved to the `PatternStore` (canonical 16x16 grid).
    4. `ChunkStamper` patrols around loaded chunks near the player and stamps the updated pattern.
    5. The pattern is persisted to the world save via `WcePersistentState`.
  </core_loop>
  
  <technical_details>
    <detail>Item Frames are treated as Entities in Java. We capture their interactions (Use/Attack) to clone them.</detail>
    <detail>Jukeboxes wait 1 tick for the BlockEntity to update before echoing sounds to clones.</detail>
    <detail>Inventories (Chests, Shulkers) deliberately copy as empty blocks to avoid infinite item dupes.</detail>
    <detail>Paintings are excluded completely to prevent entity-lag crashes.</detail>
    <detail>Mod uses Fabric API events, custom Mixins, and Minecraft server ticking.</detail>
  </technical_details>
</architecture>

<code_structure>
  <entry_points>
    <file path="src/main/java/studio/threedonkeys/wce/Wce.java" role="Main class, tick loop, global state" />
    <file path="src/main/java/studio/threedonkeys/wce/WorldCopiesEverything.java" role="Fabric Mod Initializer" />
  </entry_points>
  
  <engines>
    <file path="src/main/java/studio/threedonkeys/wce/pattern/PatternStore.java" role="Stores the canonical repeating edits" />
    <file path="src/main/java/studio/threedonkeys/wce/stamp/ChunkStamper.java" role="Enforces the pattern onto chunks live" />
  </engines>
  
  <mixins_dir path="src/main/java/studio/threedonkeys/wce/mixin/" role="Vanilla code hooks (Explosions, BlockItem placements, Mobs)" />
</code_structure>

<commands>
  <build>./gradlew build</build>
  <run_client>./gradlew runClient</run_client>
  <generate_sources>./gradlew genSources</generate_sources>
</commands>

<guidelines>
  <rule>Do not use Bukkit/Spigot APIs. This is a Fabric mod using yarn mappings.</rule>
  <rule>When modifying NBT or BlockStates, remember we target Minecraft 1.20.1/1.21 logic (check versions in gradle).</rule>
  <rule>Ensure all tick-delayed tasks use `Wce.scheduler().runLater()` rather than `Thread.sleep`.</rule>
</guidelines>
