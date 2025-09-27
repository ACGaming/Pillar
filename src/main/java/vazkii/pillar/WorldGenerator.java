/**
 * This class was created by <Vazkii>. It's distributed as
 * part of the Pillar Mod. Get the Source Code in github:
 * https://github.com/Vazkii/Pillar
 * <p>
 * Pillar is Open Source and distributed under the
 * CC-BY-NC-SA 3.0 License: https://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB
 * <p>
 * File Created @ [25/06/2016, 19:13:22 (GMT)]
 */
package vazkii.pillar;

import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.IWorldGenerator;
import vazkii.pillar.schema.GeneratorType;
import vazkii.pillar.schema.StructureSchema;

import java.util.*;
import java.util.stream.Collectors;

public class WorldGenerator implements IWorldGenerator {

    private final Map<BlockPos, String> generatedStructurePositions = new HashMap<>();

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
        if (!(world instanceof WorldServer)) return;

        int structuresGenerated = 0;
        List<StructureSchema> schemaList = new ArrayList<>(StructureLoader.loadedSchemas.values());
        Collections.shuffle(schemaList, random);
        for (StructureSchema schema : schemaList) {
            EnumActionResult res = generateStructure(schema, random, world, chunkX, chunkZ);
            if (res == EnumActionResult.PASS) continue;

            if (res == EnumActionResult.SUCCESS) structuresGenerated++;

            if (structuresGenerated >= Pillar.maxStructuresInOneChunk) break;
        }
    }

    public EnumActionResult generateStructure(StructureSchema schema, Random random, World world, int chunkX, int chunkZ) {
        if (schema.generatorType == GeneratorType.NONE) return EnumActionResult.PASS;

        int rarity = (int) (schema.rarity * Pillar.rarityMultiplier);
        if (rarity > 0 && random.nextInt(rarity) == 0) {
            int x = chunkX * 16 + random.nextInt(16);
            int z = chunkZ * 16 + random.nextInt(16);
            BlockPos xzPos = new BlockPos(x, 0, z);
            BlockPos pos = schema.generatorType.getGenerationPosition(schema, random, world, xzPos);

            if (pos != null) {
                IBlockState state = world.getBlockState(pos);

                if (schema.generatorType.shouldFindLowestBlock())
                    while (state.getBlock().isReplaceable(world, pos) && !(state.getBlock() instanceof BlockLiquid)) {
                        if (pos.getY() <= 0) return EnumActionResult.FAIL;

                        pos = pos.down();
                        state = world.getBlockState(pos);
                    }

                if (canSpawnInPosition(schema, world, pos)) {
                    // Prevent race-condition between multiple chunk generators by calling this before the expensive place call
                    generatedStructurePositions.put(pos, schema.structureName);
                    boolean generated = StructureGenerator.placeStructureAtPosition(random, schema, Rotation.NONE, (WorldServer) world, pos, true);
                    if (!generated) {
                        // Remove the structure from memory if the generation failed
                        generatedStructurePositions.remove(pos);
                        return EnumActionResult.FAIL;
                    }
                    return EnumActionResult.SUCCESS;
                }
            }

            return EnumActionResult.PASS;
        }

        return EnumActionResult.FAIL;
    }

    public boolean canSpawnInPosition(StructureSchema schema, World world, BlockPos pos) {
        if (schema.generateEverywhere) return true;

        HashSet<BlockPos> structureKnownPositions = generatedStructurePositions.entrySet().stream()
                .filter(entry -> schema.structureName.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(HashSet::new));

        // Check the minimum distance between structures
        for (BlockPos generatedPos : structureKnownPositions) {
            if (pos.distanceSq(generatedPos) <= schema.minDistanceToSameTypeStructures * schema.minDistanceToSameTypeStructures) {
                return false;
            }
        }

        if (!schema.dimensionSpawns.isEmpty()) {
            int dim = world.provider.getDimension();
            if (schema.isDimensionSpawnsBlacklist && schema.dimensionSpawns.contains(dim)) return false;

            if (!schema.isDimensionSpawnsBlacklist && !schema.dimensionSpawns.contains(dim)) return false;
        }

        Biome biome = world.getBiome(pos);
        String name = biome.getRegistryName().toString();

        if (schema.isBiomeNameSpawnsBlacklist && !schema.biomeNameSpawns.contains(name)) return true;
        if (schema.biomeNameSpawns.contains(name)) return !schema.isBiomeNameSpawnsBlacklist;

        try {
            Set<BiomeDictionary.Type> types = BiomeDictionary.getTypes(biome);
            if (schema.isBiomeTagSpawnsBlacklist) {
                for (BiomeDictionary.Type type : types) {
                    if (schema.biomeTagSpawns.contains(type.getName())) return false;
                }
                return true;
            } else {
                for (BiomeDictionary.Type type : types) {
                    if (schema.biomeTagSpawns.contains(type.getName())) return true;
                }
            }
        } catch (NullPointerException e) {
            // In case a biome isn't properly registered
        }

        return false;
    }
}
