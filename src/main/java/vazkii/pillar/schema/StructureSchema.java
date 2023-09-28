/**
 * This class was created by <Vazkii>. It's distributed as
 * part of the Pillar Mod. Get the Source Code in github:
 * https://github.com/Vazkii/Pillar
 * <p>
 * Pillar is Open Source and distributed under the
 * CC-BY-NC-SA 3.0 License: https://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB
 * <p>
 * File Created @ [25/06/2016, 18:08:37 (GMT)]
 */
package vazkii.pillar.schema;

import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import vazkii.pillar.StructureLoader;

import java.util.List;

public final class StructureSchema {

    public String structureName;
    public GeneratorType generatorType;
    public int maxY, minY;
    public int offsetX, offsetY, offsetZ;
    public String mirrorType;
    public String rotation;
    public boolean ignoreEntities;
    public List<Integer> dimensionSpawns;
    public List<String> biomeNameSpawns;
    public List<String> biomeTagSpawns;
    public boolean isDimensionSpawnsBlacklist;
    public boolean isBiomeNameSpawnsBlacklist;
    public boolean isBiomeTagSpawnsBlacklist;
    public boolean generateEverywhere;
    public float integrity, decay;
    public int rarity;
    public String filling;
    public int fillingMetadata;
    public FillingType fillingType;

    @Override
    public String toString() {
        return StructureLoader.jsonifySchema(this);
    }

    public Mirror getMirrorType() {
        if (rotation == null) return Mirror.NONE;

        return switch (mirrorType) {
            case "mirror_left_right", "LEFT_RIGHT" -> Mirror.LEFT_RIGHT;
            case "mirror_front_back", "FRONT_BACK" -> Mirror.FRONT_BACK;
            default -> Mirror.NONE;
        };
    }

    public Rotation getRotation() {
        if (rotation == null) return null;

        return switch (rotation) {
            case "90", "-270" -> Rotation.CLOCKWISE_90;
            case "180", "-180" -> Rotation.CLOCKWISE_180;
            case "270", "-90" -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }
}
