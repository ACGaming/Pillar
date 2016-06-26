/**
 * This class was created by <Vazkii>. It's distributed as
 * part of the Pillar Mod. Get the Source Code in github:
 * https://github.com/Vazkii/Pillar
 * 
 * Pillar is Open Source and distributed under the
 * [ADD-LICENSE-HERE]
 * 
 * File Created @ [25/06/2016, 18:22:31 (GMT)]
 */
package vazkii.pillar;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.world.storage.loot.LootTableManager;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import vazkii.pillar.schema.FillingType;
import vazkii.pillar.schema.GeneratorType;
import vazkii.pillar.schema.StructureSchema;

public final class StructureLoader {

	public static final Map<String, StructureSchema> loadedSchemas = new HashMap();

	private static Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private static StructureSchema defaultSchema;

	public static void loadStructures(World world) {
		Pillar.log("Loading structures...");
		
		Pillar.log("Resetting old managers");
		Pillar.resetManagers();
		
		File[] files = Pillar.pillarDir.listFiles((File f) -> {
			if(!f.getName().endsWith(".json") || f.getName().equals(Pillar.TEMPLATE_FILE))
				return false;

			File f1 = new File(Pillar.structureDir, getStructureNBTLocation(f.getName()));
			return f1.exists();
		});
		
		loadedSchemas.clear();
		for(File f : files) {
			try {
				Pillar.log("Loading " + f.getName());
				StructureSchema schema = gson.<StructureSchema>fromJson(new FileReader(f), new TypeToken<StructureSchema>(){}.getType());
				schema.structureName = getStructureNBTLocation(f.getName()).replaceAll("\\.nbt$", "");
				if(schema != null && schema.generatorType != null) {
					Pillar.log("Loaded schema " + schema.structureName);
					loadedSchemas.put(schema.structureName, schema);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		if(world != null)
			copyAllLootTables(world);
		
		Pillar.log("Finished structure loading. " + loadedSchemas.size() + " Structures loaded.");
	}
	
	public static void copyAllLootTables(World world) {
		File worldDir = getPillarLootTableDir(world);
		
		List<String> refreshes = new ArrayList();
		
		File[] files = Pillar.lootTablesDir.listFiles((File f) -> f.getName().endsWith(".json"));
		for(File file : files)
			try {
				Files.copy(file.toPath(), new File(worldDir, file.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
				refreshes.add(file.getName().replaceAll("\\.json$", ""));
			} catch (IOException e) {
				e.printStackTrace();
			}
		
		if(!refreshes.isEmpty()) {
			LoadingCache<ResourceLocation, LootTable> cache = ReflectionHelper.getPrivateValue(LootTableManager.class, world.getLootTableManager(), Pillar.OBF_REGISTERED_LOOT_TABES);
			for(String s : refreshes)
				cache.refresh(new ResourceLocation("pillar", s));
		}
	}
	
	public static void copyNeededLootTable(World world, String lootTable) {
		File worldDir = getPillarLootTableDir(world);
		File expectedFile = new File(worldDir, lootTable + ".json");
		if(expectedFile.exists())
			return;
		
		File targetFile = new File(Pillar.lootTablesDir, lootTable + ".json");
		if(targetFile.exists())
			try {
				Files.copy(targetFile.toPath(), expectedFile.toPath(), StandardCopyOption.ATOMIC_MOVE);
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
	
	public static File getPillarLootTableDir(World world) {
        File dir = new File(world.getSaveHandler().getWorldDirectory(), "data/loot_tables/pillar");
        if(!dir.exists())
        	dir.mkdirs();
        
        return dir;
	}
	
	public static String getStructureNBTLocation(String jsonFileName) {
		String name = jsonFileName.replaceAll("\\.json$", ".nbt");
		name = name.replaceAll("\\.(?!nbt)", "/");
		return name;
	}
	
	public static String jsonifySchema(StructureSchema schema) {
		return gson.toJson(schema);
	}

	public static StructureSchema getDefaultSchema() {
		if (defaultSchema != null)
			return defaultSchema;

		defaultSchema = new StructureSchema();
		defaultSchema.generatorType = GeneratorType.NONE;
		defaultSchema.minY = -1;
		defaultSchema.maxY = -1;
		defaultSchema.offsetX = 0;
		defaultSchema.offsetY = 0;
		defaultSchema.offsetZ = 0;

		defaultSchema.mirrorType = "NONE";
		defaultSchema.rotation = null;
		defaultSchema.ignoreEntities = false;

		defaultSchema.dimensionSpawns = Collections.emptyList();
		defaultSchema.biomeNameSpawns = Collections.emptyList();
		defaultSchema.biomeTagSpawns = Collections.emptyList();
		defaultSchema.isDimensionSpawnsBlacklist = false;
		defaultSchema.isBiomeNameSpawnsBlacklist = false;
		defaultSchema.isBiomeTagSpawnsBlacklist = false;
		defaultSchema.generateEverywhere = false;

		defaultSchema.integrity = 1;
		defaultSchema.decay = 0;

		defaultSchema.filling = "";
		defaultSchema.fillingMetadata = 0;
		defaultSchema.fillingType = FillingType.AIR;

		defaultSchema.rarity = 100;

		return defaultSchema;
	}

}
