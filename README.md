[deneme anticheat source]

Server side anticheat for Fabric. Movement and knockback are verified against a full
re-implementation of the vanilla client tick, combat is verified against the entity
positions the client was actually looking at, and rotations are checked statistically
instead of against fixed thresholds.

Target: Minecraft 1.21.11, Fabric loader 0.16+, Java 21.

## Build

```
./gradlew :fabric:build
```

Artifact: `fabric/build/libs/deneme-anticheat-fabric-<version>.jar`

PacketEvents is bundled inside the jar and relocated, so the server needs no extra
plugin, mod or dependency. Only the jar goes into `mods/`.

Building with `-PshadePE=false` produces a `lite` build that expects PacketEvents to be
provided by the platform instead.

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
