# deneme anticheat

Server-side anticheat for Fabric. Movement and knockback are verified against a full
re-implementation of the vanilla client tick, combat is verified against the entity
positions the client was actually looking at, and rotations are analysed statistically
instead of against fixed thresholds.

- **Platform:** Fabric, Minecraft 1.21.11 (loader 0.16+)
- **Runtime:** Java 21
- **Scope:** 167 registered checks across packet, movement, combat and velocity analysis
- **Everything is active by default.** There is no experimental gate, no per-check toggle
  and no detection that fires on a single packet.

## Build

Fabric Loom 1.19 needs a Java 25 runtime for the build itself; the compiled classes target
Java 17 and the toolchain is pinned to Java 21.

```bash
JAVA_HOME=/path/to/jdk-25 ./gradlew :fabric:build
```

Artifact: `fabric/build/libs/deneme-anticheat-fabric-<version>.jar`

PacketEvents is nested inside the jar (see `libs/maven`), so the server needs nothing else
installed. Only the jar goes into `mods/`.

## How it works

```
packet in -> transaction compensated state -> prediction tick -> checks -> evidence -> punishment group
```

1. **Compensation.** Incoming movement is replayed against the world state the client
   actually had when it moved, using transaction ordering, so a lagging client is not
   punished for being laggy.
2. **Prediction.** The vanilla tick is re-implemented (`predictionengine/`) and the
   resulting offset is compared to what the client reported.
3. **Checks.** Each check sees only the listeners it needs: `PacketReceiveListener`,
   `PacketSendListener`, `RotationListener`, `PositionListener`, `PostPredictionListener`,
   `VehicleListener`.
4. **Evidence.** Weak independent signals are pooled per player instead of each one being
   forced to a punishing threshold (see `CombatEvidence`).
5. **Punishment.** Groups in `punishments.yml` decide which commands run at which violation
   level.

Violations are severity based: a check accumulates, decays over time (`decay` in
`@CheckData`) and has to cross a repeat count inside a window before it flags.

## Detection coverage

| Area | Checks | Examples |
| --- | --- | --- |
| Packet and protocol | 57 | `BadPackets*`, `PacketOrder*`, `Crash*`, `Exploit*` |
| Movement | 33 | `OffsetHandler`, `Phase`, `GroundSpoof`, `NoFall`, `Elytra*`, `Vehicle*`, `Timer*`, `Sprint*` |
| Combat and killaura | 21 | `AimTargetAngle`, `AimSwitchTime`, `SwingRatio`, `MultiInteract*` |
| Aim analysis | 9 | `AimSensitivity`, `AimSnap`, `AimModulo360`, `AimThroughWall` |
| Knockback and velocity | 5 | `VelocityVector`, `ExplosionRatio`, `SprintTamper` |
| Scaffolding | 9 | `MultiPlace`, `DuplicateRotPlace` |
| Breaking | 10 | `NoSwingBreak`, `InvalidBreak` |
| Chat, bot and misc | 3 | `ChatA-D`, `Baritone`, `Post` |

The remaining registered entries are prediction handlers, analysis processors and debug
tools (`PredictionRunner`, `OffsetHandler`, `SuperDebug`, ...) that share the same registry.

### Aim and killaura

| Check | What it measures |
| --- | --- |
| `AimTargetAngle` | Attack angle versus the angular radius the hitbox allows, `atan(r / d)`. Hits outside it with a margin are collected over a window. |
| `AimPostAttackRotation` | The rotation is returned to its pre-hit value right after the attack with matching magnitude (aura flick). |
| `AimSwitchTime` | Target switches faster than a real mouse can move, in degrees per millisecond, derived from DPI and sensitivity limits. |
| `AimAttackAngleEntropy` | Shannon entropy of the attack-time yaw delta. Machine rotations form a narrow, low-entropy distribution. |
| `AimThroughWall` | Attacks where the eye-to-hitbox ray crosses a blocking block, i.e. the target could not have been visible. |
| `AimSensitivity` | Rotations no mouse sensitivity step can produce. |
| `AimSnap` | Large rotation immediately before an attack with a calm baseline before it. |
| `AimModulo360`, `AimDuplicateLook` | Impossible angle jumps and duplicated look packets. |

### Autoclicker and interaction

| Check | What it measures |
| --- | --- |
| `ClickBurst` | Six consecutive attack intervals with a spread of one millisecond or less. |
| `ClickIntervalEntropy` | Entropy of the click interval histogram over 30+ clicks; humans are far less regular. |
| `ClickTiming` | Machine-precise intervals. |
| `SwingRatio` | More attacks than arm swings over a 120 attack window. |
| `ClickWhileAimStill` | A moving target is hit while the view has not moved for several rotations. |
| `MultiInteractA/B`, `SelfInteract`, `InvalidInteractCursor` | Multiple targets, shifted hit positions, self hits, out-of-hitbox interaction vectors. |

### Reach

| Check | What it measures |
| --- | --- |
| `Reach` | Distance to the compensated hitbox. |
| `ReachRotated` | The look ray never crossed the hitbox that was hit, within normal reach. |
| `ReachTimestamp` | The hit lands on a position the target had already left, then the target jumps towards it (backtracking). |
| `Hitboxes` | Hits outside the valid hitbox. |

### Knockback and velocity

| Check | What it measures |
| --- | --- |
| `AntiKB` (`Knockback`) | Received knockback versus the movement that follows it, on the prediction engine. |
| `AntiExplosion` (`Explosion`) | Same for explosion motion. |
| `VelocityVector` | Ratio between the knockback vector sent and the horizontal movement observed over the next ticks. |
| `ExplosionRatio` | The same ratio for explosion `playerMotion`. |
| `SprintTamper`, `KeepSprint` | Sprint state rewritten inside the knockback tick, or kept after a hit without a toggle. |

### Hybrid

| Check | What it measures |
| --- | --- |
| `RotationMovementMismatch` | A 25°+ yaw change where the movement direction does not follow, i.e. rotation spoofing combined with velocity. |

## Evidence pooling

`CombatEvidence` is a shared pool instead of a check with its own threshold. Every combat
check reports its signal there before it reaches its own flag threshold (weights are per
signal, the window is 10 seconds). Two independent signals agreeing on the same player are
worth more than either one alone, which catches the "each check alone stays under the
threshold" client.

```java
CombatEvidence.report(player, "aim_angle", 1.0);
```

## False positive policy

Every combat check skips the same conditions before it evaluates anything:

- teleport packets (which also cover respawn) and vehicle switches
- players riding or being ridden (`inVehicle()`, `target.riding != null`)
- Bedrock/Geyser players (`GeyserUtil`)
- `CREATIVE` and `SPECTATOR`
- dead entities, unloaded chunks
- large target position jumps, where the compensated position is not trustworthy
- `disableDeneme` players and any player with a transaction ping above the configured bound

Thresholds are deliberately multi-hit. `KnockbackHandler` keeps its original, long verified
thresholds; the new velocity checks run alongside it rather than replacing it.

## Commands

Both `/denemeac` and `/dac` work.

| Command | Permission | Description |
| --- | --- | --- |
| `/denemeac alerts` | `deneme.alerts` | Toggle alert subscription |
| `/denemeacalerton` / `/denemeacalertoff` | `deneme.alerts` | Subscribe / unsubscribe from alerts |
| `/denemeac verbose` | `deneme.verbose` | Toggle verbose output for a player |
| `/denemeac brands` | `deneme.brand` | Toggle client brand notifications |
| `/denemeac profile <player>` | `deneme.profile` | Inspect a player's current state |
| `/denemeac history <player>` | `deneme.history` | Query stored violations (`copy`, `migrate`, `repair` subcommands) |
| `/denemeac spectate <player>` | `deneme.spectate` | Spectate a suspect, `/denemeac stopspectating` to return |
| `/denemeac list` | `deneme.list` | List tracked players |
| `/denemeac perf` | `deneme.performance` | Print prediction timings |
| `/denemeac dump` | `deneme.dump` | Generate a debug dump |
| `/denemeac debug` / `consoledebug` | `deneme.debug` / `deneme.consoledebug` | Per-player debug output |
| `/denemeac sendalert`, `testwebhook` | `deneme.sendalert`, `deneme.testwebhook` | Test alert delivery |
| `/denemeac reload` | `deneme.reload` | Reload configuration |
| `/denemeac version` | `deneme.version` | Version and environment info |

## Alerts

```
denemeac | [Steve] killaura detected (AimTargetAngle)
denemeac | [Steve] velocity detected (VelocityVector)
```

Prefix, then the player in brackets, then the category derived from the check's stable key
(`deneme.combat.*` and `deneme.aim.*` map to `killaura`), then the check name. Formatting
lives in `messages/<lang>.yml` under `alerts-format`; the prefix is `prefix` in the same file.

## Configuration

Files are written to the mod's data directory (`config/denemeanticheat` on Fabric):

| File | Contents |
| --- | --- |
| `config.yml` | Alerts, console and proxy forwarding, client brand filters, spectator handling, update checks |
| `punishments.yml` | Punishment groups, violation thresholds, commands per step |
| `messages/<lang>.yml` | Every user facing string, 11 languages |
| `database.yml` | Routes violation, session, identity, setting and blob data to a backend |
| `databases/<id>.yml` | Per-backend settings (`sqlite`, `mysql`, ...) |
| `discord.yml` | Webhook URL, embed title and colour, timestamp and field selection |

Bundled defaults live under `common/src/main/resources/{config,database,databases,discord,messages,punishments}`
and are versioned: `config.yml` carries a `config-flavor` marker and a `config-version`, so an
upgraded jar migrates an existing file instead of flat-merging into a changed key set.

### Punishment groups

A check only alerts if some group in `punishments.yml` lists it, and the match is a case
insensitive substring of the check name or its alternative name. A check that is not listed
in a group runs silently.

```yaml
Combat:
  remove-violations-after: 300
  checks:
    - "Aim"
    - "SwingRatio"
    - "ClickWhileAimStill"
    - "CombatEvidence"
  commands:
    - "20:40 [alert]"
    - "1:1 [log]"
```

`threshold:interval command` runs the command every `interval` violations after `threshold`.

## Adding a check

1. Extend `Check` in `checks/impl/<area>/` and implement the listener interfaces it needs.
2. Annotate it with `@CheckData`, `stableKey` is mandatory and must be unique
   (`deneme.<area>.<name>`); the first two segments decide the alert category.
3. Register it in `manager/CheckManager.java`.
4. Add the name to the relevant group in every `punishments/<lang>.yml`, otherwise the
   check never alerts.

## Project layout

```
api/            public API surface
api-internal/   internal API
common/         checks, prediction engine, compensation, commands, config
fabric/         platform bindings, one module per supported Minecraft version
libs/           nested PacketEvents artifact
```

## License

GPL-3.0. See `LICENSE`.
