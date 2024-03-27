package net.misigno.createdpcompat;

import com.simibubi.create.Create;

import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.foundation.utility.BlockFace;
import com.simibubi.create.foundation.utility.Pair;
import io.github.fabricators_of_create.porting_lib.util.EnvExecutor;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.loader.api.FabricLoader;
import net.kyrptonaught.customportalapi.CustomPortalApiRegistry;
import net.kyrptonaught.customportalapi.CustomPortalBlock;
import net.kyrptonaught.customportalapi.CustomPortalsMod;
import net.kyrptonaught.customportalapi.util.PortalLink;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

import static com.simibubi.create.content.trains.track.AllPortalTracks.registerIntegration;
import static net.kyrptonaught.customportalapi.util.CustomTeleporter.customTPTarget;
import static net.kyrptonaught.customportalapi.util.CustomTeleporter.wrapRegistryKey;

public class CreateDPCompat implements ModInitializer {
	public static final String ID = "createdpcompat";
	public static final String NAME = "Create compat for datapack portals";
	public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

	@Override
	public void onInitialize() {
		LOGGER.info("Create addon mod [{}] is loading alongside Create [{}]!", NAME, Create.VERSION);
		LOGGER.info(EnvExecutor.unsafeRunForDist(
				() -> () -> "{} is accessing Porting Lib from the client!",
				() -> () -> "{} is accessing Porting Lib from the server!"
		), NAME);
		if(FabricLoader.getInstance().isModLoaded("customportalapi"))
			registerIntegration(new ResourceLocation("customportalapi","customportalblock"),CreateDPCompat::datapackportals);
		if(FabricLoader.getInstance().isModLoaded("betterend"))
			registerIntegration(new ResourceLocation("betterend","end_portal_block"),CreateDPCompat::betterendportals);
	}

	private static Pair<ServerLevel, BlockFace> betterendportals(Pair<ServerLevel, BlockFace> inbound) {

		ServerLevel otherLevel = inbound.getFirst().getServer().getLevel(inbound.getFirst().dimension() == Level.END ? Level.OVERWORLD : Level.END);
		SuperGlueEntity probe = new SuperGlueEntity(inbound.getFirst(), new AABB(inbound.getSecond().getConnectedPos()));
		probe.setYRot(inbound.getSecond().getFace()
				.toYRot());
		probe.setPortalEntrancePos();
		PortalInfo portalInfo = getPortalInfo(otherLevel,probe,inbound);
		if(portalInfo ==null)
			return null;
		BlockState otherPortalState = otherLevel.getBlockState(BlockPos.containing(portalInfo.pos));
		if (otherPortalState.getBlock() != inbound.getFirst().getBlockState(inbound.getSecond().getConnectedPos()).getBlock())
			return null;

		Direction targetDirection = inbound.getSecond().getFace();
		if (targetDirection.getAxis() == otherPortalState.getValue(BlockStateProperties.HORIZONTAL_AXIS))
			targetDirection = targetDirection.getClockWise();
		BlockPos otherPos = BlockPos.containing(portalInfo.pos).relative(targetDirection.getOpposite());
		return Pair.of(otherLevel,new BlockFace(otherPos,targetDirection));
	}

	private static PortalInfo getPortalInfo(ServerLevel otherLevel, SuperGlueEntity probe, Pair<ServerLevel, BlockFace> inbound) {
		try{
			Object clazz = Class.forName("org.betterx.betterend.portal.TravelerState")
					.getDeclaredConstructor(Entity.class)
					.newInstance(probe);
			Method method = clazz.getClass().getDeclaredMethod("handleInsidePortal", BlockPos.class);
			method.invoke(clazz,inbound.getSecond().getConnectedPos());
			method = clazz.getClass().getDeclaredMethod("findDimensionEntryPoint", ServerLevel.class);
			method.setAccessible(true);
			return (PortalInfo) method.invoke(clazz,otherLevel);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private static Pair<ServerLevel, BlockFace> datapackportals(Pair<ServerLevel, BlockFace> inbound) {
		BlockPos portalPos = inbound.getSecond().getConnectedPos();
		/*try{
			Object clazz = Class.forName("net.kyrptonaught.customportalapi.CustomPortalBlock")
					.getDeclaredConstructor(BlockBehaviour.Properties.class)
					.newInstance(FabricBlockSettings.copyOf(Blocks.NETHER_PORTAL));

		}catch (Exception e) {
			e.printStackTrace();
		}*/
		CustomPortalBlock portalBlock = (CustomPortalBlock) (inbound.getFirst().getBlockState(portalPos).getBlock());
		Block frameBlock = portalBlock.getPortalBase(inbound.getFirst(), portalPos);
		PortalLink link =  CustomPortalApiRegistry.getPortalLinkFromBase(frameBlock);
		return datapackPortalProvider(inbound,wrapRegistryKey(link.dimID),wrapRegistryKey(link.returnDimID),frameBlock,link);
	}

	private static Pair<ServerLevel, BlockFace> datapackPortalProvider(Pair<ServerLevel, BlockFace> inbound, ResourceKey<Level> firstDimension, ResourceKey<Level> secondDimension, Block portalBlock, PortalLink link) {
		ServerLevel otherLevel = inbound.getFirst().getServer().getLevel(inbound.getFirst().dimension() == secondDimension ? firstDimension : secondDimension);
		SuperGlueEntity probe = new SuperGlueEntity(inbound.getFirst(), new AABB(inbound.getSecond().getConnectedPos()));
		probe.setYRot(inbound.getSecond().getFace()
				.toYRot());
		probe.setPortalEntrancePos();
		PortalInfo portalInfo = customTPTarget(otherLevel,probe,inbound.getSecond().getConnectedPos(),portalBlock, link.getFrameTester());

		BlockState otherPortalState = otherLevel.getBlockState(BlockPos.containing(portalInfo.pos));
		if (otherPortalState.getBlock() != inbound.getFirst().getBlockState(inbound.getSecond().getConnectedPos()).getBlock())
			return null;

		Direction targetDirection = inbound.getSecond().getFace();
		if (targetDirection.getAxis() == otherPortalState.getValue(BlockStateProperties.AXIS))
			targetDirection = targetDirection.getClockWise();
		BlockPos otherPos = BlockPos.containing(portalInfo.pos).relative(targetDirection);
		return Pair.of(otherLevel,new BlockFace(otherPos,targetDirection.getOpposite()));
	}

}

