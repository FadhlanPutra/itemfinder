# Changelog

<!-- FORMAT

Replace {} With your value
Reference: https://keepachangelog.com/en/1.1.0/

## [{VERSION}] - {YYYY-MM-DD} (Format ISO 8601)

### {FIXED/ADDED/CHANGED/REMOVED/DEPRECATED/SECURITY/etc}

 -->


## [1.0.1] - 2026-10-13

## FIXED
- Container names in search results now appear as translated names
  ("Chest", "Ender Chest", "Chest Boat") instead of internal class names
  (`class_2595`, "Chestblockentity").
- The HUD, GUI, and chat now use the same container names.
- The cache now stores block/entity IDs, ensuring names remain correct after a version change.
- Singular/plural corrections ("1 container", "1 item").
- Replacing hardcoded Indonesian text.
- New translation key
- Cleaning unused code


## [1.0.0] - 2026-05-04

### ADDED
- Search by keyword or exact item
- Highlighted containers in the world
- Particle trail guides you to results
- HUD shows container names and distance
- Works in multiplayer (with cache fallback)
