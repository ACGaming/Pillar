/**
 * This class was created by <Vazkii>. It's distributed as
 * part of the Pillar Mod. Get the Source Code in github:
 * https://github.com/Vazkii/Pillar
 * 
 * Pillar is Open Source and distributed under the
 * [ADD-LICENSE-HERE]
 * 
 * File Created @ [25/06/2016, 18:42:33 (GMT)]
 */
package vazkii.pillar;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockStoneBrick;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandResultStats.Type;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.structure.template.TemplateManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import vazkii.pillar.schema.FillingType;
import vazkii.pillar.schema.StructureSchema;

public final class StructureGenerator {
	
	private static final HashMap<String, DataHandler> dataHandlers = new HashMap();
	private static int iteration;
	
	static {
		dataHandlers.put("run", StructureGenerator::commandRun);
		dataHandlers.put("chest", StructureGenerator::commandChest);
		dataHandlers.put("spawner", StructureGenerator::commandSpawner);
		dataHandlers.put("struct", StructureGenerator::commandStruct);
	}
	
	public static boolean placeStructureAtPosition(Random rand, StructureSchema schema, Rotation baseRotation, WorldServer world, BlockPos pos) {
		return placeStructureAtPosition(rand, schema, baseRotation, world, pos, 0);
	}
	
	public static boolean placeStructureAtPosition(Random rand, StructureSchema schema, Rotation baseRotation, WorldServer world, BlockPos pos, int iteration) {
		if(pos == null)
			return false;

		if(iteration > Pillar.maximumGenerationIterations)
			return false;
		
		MinecraftServer minecraftserver = world.getMinecraftServer();
		TemplateManager templatemanager = Pillar.templateManager;
		Template template = templatemanager.func_189942_b(minecraftserver, new ResourceLocation(schema.structureName));

		if(template == null)
			return false;
		
		BlockPos size = template.getSize();
		int top = pos.getY() + size.getY(); 
		if(top >= 256) {
			int shift = top - 256;
			pos.add(0, -shift, 0);
		}
		
		if(Pillar.devMode && iteration == 0)
			Pillar.log("Generating Structure " +  schema.structureName + " at " + pos);

		PlacementSettings settings = new PlacementSettings();
		settings.setMirror(schema.mirrorType);

		Rotation rot = schema.rotation;
		if(schema.rotation == null)
			rot = Rotation.values()[rand.nextInt(Rotation.values().length)];
		rot = rot.add(baseRotation);
		
		settings.setRotation(rot);
		settings.setIgnoreEntities(schema.ignoreEntities);
		settings.setChunk((ChunkPos) null);
		settings.setReplacedBlock((Block) null);
		settings.setIgnoreStructureBlock(false);

		settings.func_189946_a(MathHelper.clamp_float(schema.integrity, 0.0F, 1.0F));

		BlockPos finalPos = pos.add(schema.offsetX, schema.offsetY, schema.offsetZ);
		template.addBlocksToWorldChunk(world, finalPos, settings);

		if(schema.decay > 0) {
			for(int i = 0; i < size.getX(); i++)
				for(int j = 0; j < size.getY(); j++)
					for(int k = 0; k < size.getZ(); k++) {
						BlockPos currPos = finalPos.add(template.transformedBlockPos(settings, new BlockPos(i, j, k)));
						IBlockState state = world.getBlockState(currPos);
						if(state.getBlock() == Blocks.STONEBRICK && state.getValue(BlockStoneBrick.VARIANT) == BlockStoneBrick.EnumType.DEFAULT && rand.nextFloat() < schema.decay)
							world.setBlockState(currPos, state.withProperty(BlockStoneBrick.VARIANT, rand.nextBoolean() ? BlockStoneBrick.EnumType.MOSSY : BlockStoneBrick.EnumType.CRACKED));
					}
		}

		if(schema.filling != null && !schema.filling.isEmpty()) {
			Block block = Block.getBlockFromName(schema.filling);
			if(block != null)
				for(int i = 0; i < size.getX(); i++)
					for(int j = 0; j < size.getZ(); j++) {
						BlockPos currPos = finalPos.add(template.transformedBlockPos(settings, new BlockPos(i, 0, j)));
						IBlockState currState = world.getBlockState(currPos);
						if(currState.getBlock().isAir(currState, world, currPos) || currState.getBlock() == Blocks.STRUCTURE_BLOCK)
							continue;
						
						FillingType type = schema.fillingType;
						if(type == null)
							type = FillingType.AIR;
						
						int k = -1;
						while(true) {
							BlockPos checkPos = currPos.add(0, k, 0);
							IBlockState state = world.getBlockState(checkPos);
							if(type.canFill(world, state, checkPos)) {
								IBlockState newState = block.getStateFromMeta(schema.fillingMetadata);
								
								if(schema.decay > 0 && newState.getBlock() == Blocks.STONEBRICK && newState.getValue(BlockStoneBrick.VARIANT) == BlockStoneBrick.EnumType.DEFAULT && rand.nextFloat() < schema.decay)
									newState = newState.withProperty(BlockStoneBrick.VARIANT, rand.nextBoolean() ? BlockStoneBrick.EnumType.MOSSY : BlockStoneBrick.EnumType.CRACKED);
								
								world.setBlockState(checkPos, newState);
							} else break;

							if(checkPos.getY() == 0)
								break;
							
							k--;
						}
					}
		}
		
        Map<BlockPos, String> dataBlocks = template.getDataBlocks(finalPos, settings);

        for(Entry<BlockPos, String> entry : dataBlocks.entrySet()) {
        	BlockPos entryPos = entry.getKey();
        	String data = entry.getValue();
        	world.setBlockState(entryPos, Blocks.AIR.getDefaultState());
        	handleData(rand, schema, settings, entryPos, data, world, iteration);
        }

		return true;
	}
	
	private static void handleData(Random rand, StructureSchema schema, PlacementSettings settings, BlockPos pos, String data, WorldServer world, int iteration) {
		if(data == null || data.isEmpty())
			return;
		
		// TODO Function handling
		
		data = data.replaceAll("\\/\\*\\*.*", "").trim();
		String command = data.replaceAll("\\s.*", "").toLowerCase();
		
		if(dataHandlers.containsKey(command)) {
			data = data.replaceAll("^.*?\\s", "");
			dataHandlers.get(command).handleData(rand, schema, settings, pos, data, world, iteration);
		}
	}
	
	private static void commandRun(Random rand, StructureSchema schema, PlacementSettings settings, BlockPos pos, String data, WorldServer world, int iteration) {
		StructureCommandSender.world = world;
		StructureCommandSender.position = pos;
		
		if(data.startsWith("/"))
			data = data.substring(1);
		
		MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
		server.getCommandManager().executeCommand(StructureCommandSender.INSTANCE, data);
	}
	
	private static void commandChest(Random rand, StructureSchema schema, PlacementSettings settings, BlockPos pos, String data, WorldServer world, int iteration) {
		String[] tokens = data.split("\\s");
		
		if(tokens.length == 0)
			return;
		
		String orientation = tokens.length == 1 ? "" : tokens[0];
		String lootTable = tokens.length == 1 ? tokens[0] : tokens[1];
		
		EnumFacing facing = EnumFacing.byName(orientation);
		if(facing == null)
			facing = EnumFacing.NORTH;
		
		facing = settings.getRotation().rotate(facing);
		
		world.setBlockState(pos, Blocks.CHEST.getDefaultState().withProperty(BlockChest.FACING, facing));
		
		TileEntityChest chest = (TileEntityChest) world.getTileEntity(pos);
		chest.setLootTable(new ResourceLocation(lootTable), rand.nextLong());
	}
	
	private static void commandSpawner(Random rand, StructureSchema schema, PlacementSettings settings, BlockPos pos, String data, WorldServer world, int iteration) {
		String[] tokens = data.split("\\s");
		
		if(tokens.length == 0)
			return;
		
		world.setBlockState(pos, Blocks.MOB_SPAWNER.getDefaultState());
		
		TileEntityMobSpawner spawner = (TileEntityMobSpawner) world.getTileEntity(pos);
		spawner.getSpawnerBaseLogic().setEntityName(tokens[0]);
	}
	
	private static void commandStruct(Random rand, StructureSchema schema, PlacementSettings settings, BlockPos pos, String data, WorldServer world, int iteration) {
		String[] tokens = data.split("\\s");
		
		if(tokens.length == 0)
			return;
		
		String structureName = tokens[0];

		StructureSchema newSchema = StructureLoader.loadedSchemas.get(structureName);
		if(newSchema == null || newSchema == schema)
			return;
		
		int offX = 0, offY = 0, offZ = 0;
		
		if(tokens.length >= 4) {
			offX = toInt(tokens[1], 0);
			offY = toInt(tokens[2], 0);
			offZ = toInt(tokens[3], 0);
		}
		
		Rotation rotation = Rotation.NONE;
		
		if(tokens.length >= 5) {
			String s = tokens[4];
			System.out.println("rotation is " + s);
			switch(s) {
			case "90": 
			case "-270":
				rotation = Rotation.CLOCKWISE_90;
				break;
			case "180":
			case "-180":
				rotation = Rotation.CLOCKWISE_180;
				break;
			case "270":
			case "-90":
				rotation = Rotation.COUNTERCLOCKWISE_90;
				break;
			}
		}
		rotation = rotation.add(settings.getRotation());
	
		BlockPos finalPos = pos.add(offX, offY, offZ);
		placeStructureAtPosition(rand, newSchema, rotation, world, finalPos, iteration + 1);
	}
	
	private static String[] tokenize(String data) {
		return data.split("\\s*(?<!\\);\\s*");
	}

	private static int toInt(String s, int def) {
		try {
			int i = Integer.parseInt(s);
			return i;
		} catch(NumberFormatException e) {
			return def;
		}
		
	}
	
	private static interface DataHandler {
		public void handleData(Random rand, StructureSchema schema, PlacementSettings settings, BlockPos pos, String data, WorldServer world, int iteration);
	}
	
	public static class StructureCommandSender implements ICommandSender {
		
		public static final StructureCommandSender INSTANCE = new StructureCommandSender();
		
		public static World world;
		public static BlockPos position;

		@Override
		public void addChatMessage(ITextComponent p_145747_1_) {
			// NO-OP
		}

		@Override
		public boolean canCommandSenderUseCommand(int p_70003_1_, String p_70003_2_) {
			return p_70003_1_ <= 2;
		}

		@Override
		public World getEntityWorld() {
			return world;
		}

		@Override
		public String getName() {
			return "Pillar-executor";
		}

		@Override
		public ITextComponent getDisplayName() {
			return null;
		}

		@Override
		public BlockPos getPosition() {
			return position;
		}

		@Override
		public Entity getCommandSenderEntity() {
			return null;
		}

		@Override
		public boolean sendCommandFeedback() {
			return false;
		}

		@Override
		public void setCommandStat(Type type, int amount) {
			// NO-OP
		}

		@Override
		public Vec3d getPositionVector() {
			return new Vec3d(position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5);
		}

		@Override
		public MinecraftServer getServer() {
			return world.getMinecraftServer();
		}

	}
	
}

