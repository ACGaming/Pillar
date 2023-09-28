/**
 * This class was created by <Vazkii>. It's distributed as
 * part of the Pillar Mod. Get the Source Code in github:
 * https://github.com/Vazkii/Pillar
 * <p>
 * Pillar is Open Source and distributed under the
 * CC-BY-NC-SA 3.0 License: https://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB
 * <p>
 * File Created @ [25/06/2016, 18:03:20 (GMT)]
 */
package vazkii.pillar;

import net.minecraft.util.datafix.DataFixer;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.template.TemplateManager;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;
import vazkii.pillar.command.CommandPillarCopy;
import vazkii.pillar.command.CommandPillarReload;
import vazkii.pillar.command.CommandPillarSpawn;

import java.io.*;

@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION)
public class Pillar {

    public static final String TEMPLATE_FILE = "_template.json";
    // Obfuscation stuff
    public static final String[] OBF_REGISTERED_LOOT_TABLES = {"c", "field_186527_c", "registeredLootTables"};
    public static File pillarDir;
    public static File structureDir;
    public static File lootTablesDir;
    public static TemplateManager templateManager;
    public static boolean devMode;
    public static float rarityMultiplier;
    public static int maxStructuresInOneChunk;
    public static int generatorWeight;
    public static int maximumGenerationIterations;

    public static void resetManagers(World world) {
        templateManager = new TemplateManager(structureDir.getAbsolutePath(), new DataFixer(0));
    }

    public static void log(String m) {
        FMLLog.log(Level.INFO, "[Pillar] %s", m);
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        Configuration config = new Configuration(event.getSuggestedConfigurationFile());

        config.load();
        devMode = config.getBoolean("Dev Mode", Configuration.CATEGORY_GENERAL, false, "");
        rarityMultiplier = config.getFloat("Rarity Multiplier", Configuration.CATEGORY_GENERAL, 1F, 0F, Float.MAX_VALUE, "");
        maxStructuresInOneChunk = config.getInt("Max Structures In One Chunk", Configuration.CATEGORY_GENERAL, 1, 1, Integer.MAX_VALUE, "");
        generatorWeight = config.getInt("Generator Weight", Configuration.CATEGORY_GENERAL, 0, Integer.MIN_VALUE, Integer.MAX_VALUE, "The weight to apply to Pillar's generator. Higher weight generators will spawn their structures before other mods'");
        maximumGenerationIterations = config.getInt("Maximum Generation Iterations", Configuration.CATEGORY_GENERAL, 50, 0, Integer.MAX_VALUE, "In a chain of structures spawned by 'struct' data blocks in other structures, how many can be spawned before the chain is put to a halt.");

        if (config.hasChanged()) config.save();

        pillarDir = new File(event.getModConfigurationDirectory().getParentFile(), "pillar");
        if (!pillarDir.exists()) pillarDir.mkdir();

        structureDir = new File(pillarDir, "structures");
        if (!structureDir.exists()) structureDir.mkdir();

        lootTablesDir = new File(pillarDir, "loot_tables");
        if (!lootTablesDir.exists()) lootTablesDir.mkdir();

        File template = new File(pillarDir, TEMPLATE_FILE);
        if (!template.exists()) {
            try {
                template.createNewFile();
                InputStream inStream = Pillar.class.getResourceAsStream("/assets/pillar/" + TEMPLATE_FILE);
                System.out.println(inStream);
                OutputStream outStream = new FileOutputStream(template);
                IOUtils.copy(inStream, outStream);
                inStream.close();
                outStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        StructureLoader.loadStructures(null);
        GameRegistry.registerWorldGenerator(new WorldGenerator(), generatorWeight);
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandPillarReload());
        event.registerServerCommand(new CommandPillarSpawn());
        event.registerServerCommand(new CommandPillarCopy());
    }
}
