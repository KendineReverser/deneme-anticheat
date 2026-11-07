[deneme anticheat source]

Server side anticheat for Fabric. Movement and knockback are verified against a full
re-implementation of the vanilla client tick, combat is verified against the entity
positions the client was actually looking at, and rotations are checked statistically
instead of against fixed thresholds.

Target: Minecraft 1.21.11, Fabric loader 0.16+, Java 21 runtime.

## Build

Fabric Loom 1.19 requires a Java 25 runtime for the build itself, the compiled classes
target Java 17 and the toolchain is pinned to Java 21.

```
JAVA_HOME=/path/to/jdk-25 ./gradlew :fabric:build
```

Artifact: `fabric/build/libs/deneme-anticheat-fabric-<version>.jar`

PacketEvents is nested inside the jar (see `libs/maven`), so the server needs nothing
else installed. Only the jar goes into `mods/`.

## Detections

Movement and knockback come from the simulation engine. On top of it this build adds
statistical combat analysis, all of it severity based rather than single-event based:

- `AimSensitivity` - every rotation is checked against the mouse sensitivity step the
  client claims to use. Rotations that no sensitivity can produce are flagged.
- `AimSnap` - a large rotation immediately followed by an attack, with a calm baseline
  before it, repeated over several hits.
- `ClickTiming` - attack intervals with machine level variance.
- `SprintTamper` - sprint state rewritten inside the knockback tick, the classic
  knockback reduction trick.

The last three ship experimental and require `experimental-checks` to be enabled in the
config. Nothing in this build punishes on a single event.

## Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/denemeac alerts` | `deneme.alerts` | Toggle alert subscription for yourself |
| `/denemeacalerton` | `deneme.alerts` | Subscribe to alerts |
| `/denemeacalertoff` | `deneme.alerts` | Unsubscribe from alerts |
| `/denemeac verbose` | `deneme.verbose` | Toggle verbose output |
| `/denemeac brands` | `deneme.brands` | Toggle client brand notifications |
| `/denemeac profile <player>` | `deneme.profile` | Inspect a player's current state |
| `/denemeac history <player>` | `deneme.history` | Query stored violations |
| `/denemeac spectate <player>` | `deneme.spectate` | Spectate a suspect |
| `/denemeac list` | `deneme.list` | List tracked players |
| `/denemeac perf` | `deneme.perf` | Print check timings |
| `/denemeac dump` | `deneme.dump` | Generate a debug dump |
| `/denemeac reload` | `deneme.reload` | Reload configuration |

## Alerts

```
denemeac | [Steve] killaura detected (AimModulo360)
denemeac | [Steve] velocity detected (KnockbackHandler)
```

The first word is the prefix, the bracket holds the player, then the category derived
from the check group, then the check name. Formatting lives in `messages/<lang>.yml`
under `alerts-format`; the prefix is `prefix` in the same file.

## Configuration

The mod writes its files to its data directory (`config/denemeanticheat` on Fabric):

- `config.yml` - thresholds, exemptions, alert and datastore settings
- `punishments.yml` - thresholds per check and the commands run at each step
- `messages/<lang>.yml` - every user facing string, including the alert format
- `history.yml` - violation storage backend

## License

GPL-3.0. See `LICENSE` and `NOTICE`.
