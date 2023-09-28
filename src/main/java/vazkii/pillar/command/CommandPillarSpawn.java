/**
 * This class was created by <Vazkii>. It's distributed as
 * part of the Pillar Mod. Get the Source Code in github:
 * https://github.com/Vazkii/Pillar
 * <p>
 * Pillar is Open Source and distributed under the
 * CC-BY-NC-SA 3.0 License: https://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB
 * <p>
 * File Created @ [25/06/2016, 21:09:39 (GMT)]
 */
package vazkii.pillar.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import vazkii.pillar.StructureGenerator;
import vazkii.pillar.StructureLoader;
import vazkii.pillar.schema.StructureSchema;

import java.util.ArrayList;
import java.util.List;

public class CommandPillarSpawn extends CommandBase {

    @Override
    public String getName() {
        return "pillar-spawn";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "pillar-spawn <structure name> <x> <y> <z> [<rotation>]";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 5 && args.length != 4 && args.length != 1)
            throw new CommandException("Wrong argument length.");

        String name = args[0];
        BlockPos pos = args.length == 1 ? sender.getPosition() : parseBlockPos(sender, args, 1, false);

        StructureSchema schema = StructureLoader.loadedSchemas.get(name);
        if (schema == null) throw new CommandException("There's no structure with that name.");

        Rotation rot = Rotation.NONE;

        if (args.length > 4) {
            switch (args[4]) {
                case "90":
                case "-270":
                    rot = Rotation.CLOCKWISE_90;
                    break;
                case "180":
                case "-180":
                    rot = Rotation.CLOCKWISE_180;
                    break;
                case "270":
                case "-90":
                    rot = Rotation.COUNTERCLOCKWISE_90;
                    break;
            }
        }

        World world = sender.getEntityWorld();
        if (world instanceof WorldServer)
            StructureGenerator.placeStructureAtPosition(world.rand, schema, rot, (WorldServer) world, pos, true);

        sender.sendMessage(new TextComponentString("Placed down structure '" + name + "'").setStyle(new Style().setColor(TextFormatting.GREEN)));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            List<String> list = new ArrayList(StructureLoader.loadedSchemas.keySet());
            return getListOfStringsMatchingLastWord(args, list);
        }

        return super.getTabCompletions(server, sender, args, pos);
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }
}
