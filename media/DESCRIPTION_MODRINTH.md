# World Copies Everything

**[GitHub Repository & Source Code](https://github.com/mmlo/worldCopiesEverything)** | **[Issue Tracker](https://github.com/mmlo/worldCopiesEverything/issues)**


A Fabric port of the Bedrock pack **World Copies Everything**: every block you place, break, or change becomes part of an infinite 16×16 repeating pattern. Walk thousands of blocks away — as new chunks load, the pattern is stamped into the terrain.

Port Fabric do pack Bedrock **World Copies Everything**: cada bloco que você coloca, quebra ou altera entra num padrão infinito que se repete a cada 16×16. Ande milhares de blocos — ao carregar chunks novos, o padrão é aplicado no terreno.

---

## English

### How it works

There is no finite grid of copies. Each edit is stored once at a canonical cell `(x % 16, y, z % 16)`. A patrol then stamps that pattern into loaded chunks around every player. Chunks far away are not skipped forever — they receive the pattern the moment you travel there.

- Repeat interval: **16 blocks** (one chunk)
- Live stamp radius: **24 chunks** around each player (engine limit: loaded chunks only)
- Pattern cap: **12,000** cells (oldest edits are forgotten first)
- Saved with the world (every ~10 seconds)

**Chests copy as empty blocks** — inventory stays local so copies never duplicate items.  
**Paintings are not copied.**  
**Mobs** are mirrored only into the 8 neighbouring cells, last 60 seconds, cap at **64** live clones, and are never written to the world save.

### Features

- Place, break, and interact (doors, levers, stripping logs, tilling, …)
- Doors, beds, and tall plants as atomic pairs
- Flowing water/lava, waterlogging
- Redstone copies the **switch**; each copy's pistons fire on their own
- Banners, heads, decorated pots, lecterns, jukeboxes, spawners, signs
- Item frames (place, item, rotation, break)
- Nether and End portals
- Bonemeal trees, fire you light, crop/sapling growth
- Falling blocks only if they stay standing
- Scaffolding / cane / cactus columns without item rain
- Explosions and primed TNT
- Mirror-drop suppression
- Jukebox echo on nearby copies
- Mob clones (natural / breeding / spawner / spawn egg)

### Commands (cheats / OP)

| Command | Effect |
|---|---|
| `/wce help` | Command list |
| `/wce pause` | Stop recording and stamping |
| `/wce resume` | Start copying again |
| `/wce reset` | Forget the pattern (already-copied blocks stay) |
| `/wce status` | State, edits, portals, chests, clones |
| `/wce verify` | Fix stale records in a 16-block radius |

Language: **English** by default. If the game language is **Português (Brasil)**, chat uses Brazilian Portuguese.

### Install

Pick the jar that matches your Minecraft version:

| Minecraft | Channel |
|---|---|
| **1.20.1** | Release |
| **1.21.1** | Release |
| **26.2** | Release |
| **26.3** (snapshot / alpha.9) | Alpha |

Then:

1. [Fabric Loader](https://fabricmc.net/use/installer/)
2. [Fabric API](https://modrinth.com/mod/fabric-api) for that same Minecraft version
3. Drop this jar into `mods/`

Works in singleplayer and on dedicated servers (server-side). Clients do not need the mod to join a server.

Copying is throttled (200 block writes every 4 ticks in a 24-chunk radius) so the integrated server keeps a playable tick rate.

### Credits

Original Bedrock pack by **TheLake** — ThreeDonkeys studio.  
Fabric port published by **mml**.  
License: **MIT**

---

## Português (Brasil)

### Como funciona

Não existe uma grelha finita de cópias. Cada alteração é gravada uma vez na célula canónica `(x % 16, y, z % 16)`. Uma patrulha aplica esse padrão nos chunks carregados à volta de cada jogador. Chunks longe não ficam de fora para sempre — recebem o padrão quando você chega lá.

- Intervalo: **16 blocos** (um chunk)
- Raio ao vivo: **24 chunks** (só chunks carregados)
- Limite do padrão: **12.000** células (as mais antigas são esquecidas primeiro)
- Persistente no save do mundo (~a cada 10 s)

**Baús copiam vazios** — o inventário fica só no original, para não duplicar itens.  
**Pinturas não são copiadas.**  
**Mobs** só nas 8 células vizinhas, duram 60 s, no máximo **64** clones vivos, e não entram no save.

### Recursos

- Colocar, quebrar e interagir (portas, alavancas, strip, enxada, …)
- Portas, camas e plantas altas em pares atómicos
- Água/lava a fluir e waterlogging
- Redstone copia o **interruptor**; cada cópia dispara o próprio pistão
- Banners, heads, vasos, lectern, jukebox, spawner, placas
- Item frames
- Portais do Nether e do End
- Árvores com bonemeal, fogo, crescimento de plantações
- Areia/gravel só se ficarem de pé
- Colunas de scaffolding/cana/cacto sem chuva de itens
- Explosões e TNT primado
- Eco de disco no jukebox
- Clones de mobs

### Comandos (cheats / OP)

| Comando | Efeito |
|---|---|
| `/wce help` | Lista de comandos |
| `/wce pause` | Pausa gravação e cópias |
| `/wce resume` | Volta a copiar |
| `/wce reset` | Esquece o padrão (blocos já copiados ficam) |
| `/wce status` | Estado, edições, portais, baús, clones |
| `/wce verify` | Corrige registros num raio de 16 |

Idioma: **inglês** por padrão. Se o jogo estiver em **Português (Brasil)**, o chat usa pt-BR.

### Instalação

Escolha o jar da sua versão do Minecraft:

| Minecraft | Canal |
|---|---|
| **1.20.1** | Release |
| **1.21.1** | Release |
| **26.2** | Release |
| **26.3** (snapshot / alpha.9) | Alpha |

Depois:

1. [Fabric Loader](https://fabricmc.net/use/installer/)
2. [Fabric API](https://modrinth.com/mod/fabric-api) da mesma versão
3. Coloque o jar em `mods/`

Funciona em singleplayer e servidor dedicado. O cliente não precisa do mod para entrar num servidor.

As cópias são limitadas (200 writes a cada 4 ticks, raio de 24 chunks) para o servidor integrado não cair o tick.

### Créditos

Pack Bedrock original de **TheLake** — ThreeDonkeys studio.  
Port Fabric publicado por **mml**.  
Licença: **MIT**
