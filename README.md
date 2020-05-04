# Structure Helpers
[![](https://jitpack.io/v/Draylar/structure-helpers.svg)](https://jitpack.io/#Draylar/structure-helpers)

### Overview
*Structure Helpers* is a library which assists mods in creating structures. 

Features:
 - child junctions, which allows for jigsaw elements that don't check collision
 - updated /locate command which automatically populates with all registered structures
 - better data/loot handling for `StructureBlockInfo` blocks in structures
 - new Loot Data block which provides an easier route of placing loot in structures
 - utility processors & pool elements
 

##### RandomChanceProcessor

The mod adds one structure processor, `robosky.structurehelpers.RandomChanceProcessor`, 
which allows blocks of the modder's choice to be replaced with a random selection from a pool with predefined weights.

Examples where this would be useful include vanilla strongholds, which use a similar random table to pick between
stone brick variants.

### Downloading
You can pull structure-helpers into your project with JitPack. First, add jitpack to your `build.gradle` repositories block:
```groovy
repositories {	
    maven { url 'https://jitpack.io' }
}
```
Then pull the latest 1.15.2 branch version in your dependencies block:
```groovy
dependencies {
    modImplementation 'com.github.Draylar:structure-helpers:1.15.2-SNAPSHOT'
}
```