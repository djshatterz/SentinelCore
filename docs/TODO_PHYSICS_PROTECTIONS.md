# TODO: Complete Physics Protections for Spawn Zone

## Status
The following physics protections need proper implementation and testing. Initial mixin scaffolding exists but requires correct method/field mappings for Minecraft 1.21.10.

## Pending Features

### 1. Explosion Protection
**Goal**: Prevent explosions from destroying blocks inside the spawn protection zone.

**Current State**: 
- Mixin exists at `src/main/java/org/github/shatterz/sentinelcore/mixin/ExplosionMixin.java`
- Disabled in `sentinelcore.mixins.json` due to mapping issues
- Config flag ready: `spawnProtection.physics.blockExplosions`

**Issues**:
- Yarn mappings for `Explosion` class fields may not match
- Method `affectWorld` descriptor not found in current mappings

**Next Steps**:
- Verify correct Yarn-mapped field names for `world` and `affectedBlocks` in Explosion class
- Confirm method name for explosion detonation (may be `affectWorld`, `explode`, or similar)
- Alternative: Use Fabric API's explosion events if/when available for 1.21.10

### 2. Piston Movement Blocking
**Goal**: Prevent pistons from pushing/pulling blocks into or out of the spawn protection zone.

**Current State**:
- Mixin exists at `src/main/java/org/github/shatterz/sentinelcore/mixin/PistonHandlerMixin.java`
- Disabled in `sentinelcore.mixins.json` due to runtime issues
- Config flag ready: `spawnProtection.physics.blockPistons`

**Issues**:
- Runtime behavior not working as expected
- May need to target different method or use different injection point

**Next Steps**:
- Test with different injection points (`tryMove` at various locations)
- Consider targeting `calculatePush` or piston block methods directly
- Verify `PistonHandler` constructor fields are correctly shadowed
- Alternative: Use Fabric API's piston events if/when available for 1.21.10

## Working Features (Already Implemented)

✅ Block placement/breaking prevention (except whitelisted shulker boxes)  
✅ Whitelisted block interactions (crafting tables, chests, etc.)  
✅ Fluid placement/pickup blocking (buckets)  
✅ Mob spawn suppression (via `ServerEntityEvents.ENTITY_LOAD`)  
✅ Enderman grief prevention (drops carried blocks when entering zone)  
✅ Creative mode bypass  
✅ Permission-based bypass (`sentinelcore.spawnprot.bypass`)  

## Implementation Priority

1. **High Priority**: Explosion protection (most impactful for spawn integrity)
2. **Medium Priority**: Piston blocking (less common but important for grief prevention)

## Testing Checklist (When Implemented)

- [ ] TNT detonation at zone boundary - blocks inside should remain
- [ ] Creeper explosion near zone - no block damage inside
- [ ] Piston push from outside into zone - should fail
- [ ] Piston push from inside zone - should fail  
- [ ] Piston pull into zone - should fail
- [ ] Config flags properly gate each feature
- [ ] No console errors or warnings
- [ ] Performance impact acceptable (test with multiple explosions/pistons)

## References

- Mixin files: `src/main/java/org/github/shatterz/sentinelcore/mixin/`
- Helper methods: `SpawnProtectionManager.shouldProtectExplosion()`, `shouldProtectPiston()`
- Config schema: `CoreConfig.SpawnProtection.Physics`
