package oxidizing.test;

//? if <1.20 {
/*import java.util.UUID;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
*///?}
//? if fabric && >=1.21.5 {
import net.fabricmc.fabric.api.gametest.v1.GameTest;
//?} else if fabric {
/*import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
*///?} else if <1.21.5 {
/*import net.minecraft.gametest.framework.GameTest;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}
//? if <1.21.5
//import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
//? if >=1.21.5 <1.21.11
//import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChangeOverTimeBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import oxidizing.WetOxidation;

// Before 1.21.5, Fabric's GameTest API needs FabricGameTest (for EMPTY_STRUCTURE, see the `gametest` swap)
// and NeoForge finds the tests through @GameTestHolder. From 1.21.5 TestMod registers them on NeoForge
//? if fabric && >=1.21.5 {
public class OxidizingGameTest {
//?} else if fabric {
/*public class OxidizingGameTest implements FabricGameTest {
*///?} else if <1.21.5 {
/*@GameTestHolder("oxidizing_test")
@PrefixGameTestTemplate(false)
public class OxidizingGameTest {
*///?} else
/*public class OxidizingGameTest {*/

    // From 26.2 each copper family is a WeatheringCopperCollection instead of one field per stage
    //? if >=26.2 {
    /*private static final Block COPPER_BLOCK = Blocks.COPPER_BLOCK.weathering().unaffected();
    private static final Block EXPOSED_COPPER = Blocks.COPPER_BLOCK.weathering().exposed();
    private static final Block OXIDIZED_COPPER = Blocks.COPPER_BLOCK.weathering().oxidized();
    private static final Block WAXED_COPPER_BLOCK = Blocks.COPPER_BLOCK.waxed().unaffected();
    private static final Block CUT_COPPER_SLAB = Blocks.CUT_COPPER_SLAB.weathering().unaffected();
    private static final Block EXPOSED_CUT_COPPER_SLAB = Blocks.CUT_COPPER_SLAB.weathering().exposed();
    private static final Block CUT_COPPER_STAIRS = Blocks.CUT_COPPER_STAIRS.weathering().unaffected();
    private static final Block EXPOSED_CUT_COPPER_STAIRS = Blocks.CUT_COPPER_STAIRS.weathering().exposed();
    *///?} else {
    private static final Block COPPER_BLOCK = Blocks.COPPER_BLOCK;
    private static final Block EXPOSED_COPPER = Blocks.EXPOSED_COPPER;
    private static final Block OXIDIZED_COPPER = Blocks.OXIDIZED_COPPER;
    private static final Block WAXED_COPPER_BLOCK = Blocks.WAXED_COPPER_BLOCK;
    private static final Block CUT_COPPER_SLAB = Blocks.CUT_COPPER_SLAB;
    private static final Block EXPOSED_CUT_COPPER_SLAB = Blocks.EXPOSED_CUT_COPPER_SLAB;
    private static final Block CUT_COPPER_STAIRS = Blocks.CUT_COPPER_STAIRS;
    private static final Block EXPOSED_CUT_COPPER_STAIRS = Blocks.EXPOSED_CUT_COPPER_STAIRS;
    //?}

    // Relative position of the copper under test; its 6 neighbours are set explicitly by place()
    private static final BlockPos COPPER = new BlockPos(1, 1, 1);
    private static final float ALWAYS = 0f;
    private static final float NEVER_FOR_ONE_SIDE = 0.99f;

    /*$ gametest*/ @GameTest
    public void drySidesCountZero(GameTestHelper helper) {
        place(helper, COPPER_BLOCK.defaultBlockState(), 0);
        check(helper, wetSides(helper) == 0, "dry copper should have no wet side");
        helper.succeed();
    }

    /*$ gametest*/ @GameTest
    public void eachWaterSideCounts(GameTestHelper helper) {
        for (int sides = 1; sides <= 6; sides++) {
            place(helper, COPPER_BLOCK.defaultBlockState(), sides);
            check(helper, wetSides(helper) == sides, "expected " + sides + " wet sides, got " + wetSides(helper));
        }
        helper.succeed();
    }

    /*$ gametest*/ @GameTest
    public void flowingWaterCountsLavaDoesNot(GameTestHelper helper) {
        place(helper, COPPER_BLOCK.defaultBlockState(), 0);
        helper.setBlock(COPPER.east(), Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 3));
        helper.setBlock(COPPER.west(), Blocks.LAVA.defaultBlockState());
        check(helper, wetSides(helper) == 1, "flowing water should count and lava shouldn't, got " + wetSides(helper));
        helper.succeed();
    }

    /*$ gametest*/ @GameTest
    public void waterloggedNeighbourCounts(GameTestHelper helper) {
        place(helper, COPPER_BLOCK.defaultBlockState(), 0);
        helper.setBlock(COPPER.north(), CUT_COPPER_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true));
        check(helper, wetSides(helper) == 1, "a waterlogged neighbour should count as one wet side");
        helper.succeed();
    }

    /*$ gametest*/ @GameTest
    public void waterloggedCountsAsAllSides(GameTestHelper helper) {
        place(helper, CUT_COPPER_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true), 0);
        check(helper, wetSides(helper) == 6, "waterlogged copper should count as 6 wet sides");
        helper.succeed();
    }

    /*$ gametest*/ @GameTest
    public void wetCopperAdvancesOneStage(GameTestHelper helper) {
        place(helper, COPPER_BLOCK.defaultBlockState(), 1);
        check(helper, tryOxidize(helper, ALWAYS), "wet copper should be handled by the mod");
        check(helper, blockAt(helper, COPPER).is(EXPOSED_COPPER), "copper should become exposed copper");
        check(helper, blockAt(helper, COPPER.below()).is(Blocks.WATER), "the water should be untouched");
        check(helper, blockAt(helper, COPPER.above()).isAir(), "the other neighbours should be untouched");
        helper.succeed();
    }

    /*$ gametest*/ @GameTest
    public void failedRollLeavesCopperButSkipsVanilla(GameTestHelper helper) {
        place(helper, COPPER_BLOCK.defaultBlockState(), 1);
        check(helper, tryOxidize(helper, NEVER_FOR_ONE_SIDE), "wet copper should be handled even when the roll fails");
        check(helper, blockAt(helper, COPPER).is(COPPER_BLOCK), "copper should not change on a failed roll");
        helper.succeed();
    }

    /*$ gametest*/ @GameTest
    public void dryCopperLeftToVanilla(GameTestHelper helper) {
        place(helper, COPPER_BLOCK.defaultBlockState(), 0);
        check(helper, !tryOxidize(helper, ALWAYS), "dry copper should be left to vanilla");
        check(helper, blockAt(helper, COPPER).is(COPPER_BLOCK), "dry copper should not change");
        helper.succeed();
    }

    /*$ gametest*/ @GameTest
    public void fullyOxidizedUnchanged(GameTestHelper helper) {
        place(helper, OXIDIZED_COPPER.defaultBlockState(), 6);
        tryOxidize(helper, ALWAYS);
        check(helper, blockAt(helper, COPPER).is(OXIDIZED_COPPER), "oxidized copper has no next stage");
        helper.succeed();
    }

    /*$ gametest*/ @GameTest
    public void waxedCopperUnchanged(GameTestHelper helper) {
        place(helper, WAXED_COPPER_BLOCK.defaultBlockState(), 6);
        check(helper, blockAt(helper, COPPER).is(WAXED_COPPER_BLOCK), "waxed copper should be placed");
        // Waxed copper isn't a ChangeOverTimeBlock, so vanilla never reaches the mixin for it
        check(helper, !(blockAt(helper, COPPER).getBlock() instanceof ChangeOverTimeBlock),
                "waxed copper should not oxidize");
        helper.succeed();
    }

    /*$ gametest*/ @GameTest
    public void stairsKeepTheirShape(GameTestHelper helper) {
        BlockState stairs = CUT_COPPER_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, Direction.EAST).setValue(StairBlock.HALF, Half.TOP);
        place(helper, stairs, 2);
        tryOxidize(helper, ALWAYS);
        BlockState result = blockAt(helper, COPPER);
        check(helper, result.is(EXPOSED_CUT_COPPER_STAIRS), "stairs should become exposed cut copper stairs");
        check(helper, result.getValue(StairBlock.FACING) == Direction.EAST && result.getValue(StairBlock.HALF) == Half.TOP,
                "stairs should keep their facing and half");
        helper.succeed();
    }

    // Goes through vanilla's randomTick, so it covers the mixin: with 6 wet sides the chance is 1, while vanilla
    // alone would only oxidize on about 1 random tick in 18
    /*$ gametest*/ @GameTest
    public void randomTickOxidizesWaterloggedCopper(GameTestHelper helper) {
        place(helper, CUT_COPPER_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true), 0);
        blockAt(helper, COPPER).randomTick(helper.getLevel(), helper.absolutePos(COPPER), helper.getLevel().getRandom());
        BlockState result = blockAt(helper, COPPER);
        check(helper, result.is(EXPOSED_CUT_COPPER_SLAB), "one random tick should oxidize waterlogged copper");
        check(helper, result.getValue(BlockStateProperties.WATERLOGGED), "the slab should stay waterlogged");
        helper.succeed();
    }

    // Puts the copper at COPPER and water on its first `waterSides` faces (Direction order), air on the others
    private static void place(GameTestHelper helper, BlockState copper, int waterSides) {
        helper.setBlock(COPPER, copper);
        Direction[] directions = Direction.values();
        for (int i = 0; i < directions.length; i++) {
            helper.setBlock(COPPER.relative(directions[i]), i < waterSides ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState());
        }
    }

    private static int wetSides(GameTestHelper helper) {
        return WetOxidation.wetSides(blockAt(helper, COPPER), helper.getLevel(), helper.absolutePos(COPPER));
    }

    private static boolean tryOxidize(GameTestHelper helper, float roll) {
        return WetOxidation.tryOxidize(blockAt(helper, COPPER), helper.getLevel(), helper.absolutePos(COPPER), roll);
    }

    private static BlockState blockAt(GameTestHelper helper, BlockPos relative) {
        return helper.getLevel().getBlockState(helper.absolutePos(relative));
    }

    // assertTrue takes a String from 1.21.11, a Component from 1.21.5, and doesn't exist before 1.19.3
    private static void check(GameTestHelper helper, boolean condition, String message) {
        //? if >=1.21.11 {
        helper.assertTrue(condition, message);
        //?} else if >=1.21.5 {
        /*helper.assertTrue(condition, Component.literal(message));
        *///?} else
        /*if (!condition) throw new GameTestAssertException(message);*/
    }

    // GameTestHelper only makes mock server players from 1.20. Before, build one the same way: a server player
    // with an unconnected Connection (packets are queued, never sent). ProfilePublicKey existed in 1.19 to 1.19.2
    private static ServerPlayer newPlayer(GameTestHelper helper) {
        //? if >=1.20 {
        return helper.makeMockServerPlayerInLevel();
        //?} else {
        /*MinecraftServer server = helper.getLevel().getServer();
        GameProfile profile = new GameProfile(UUID.randomUUID(), "oxidizing_test");
        //? if >=1.19 <1.19.3 {
        ServerPlayer player = new ServerPlayer(server, helper.getLevel(), profile, null);
        //?} else
        //ServerPlayer player = new ServerPlayer(server, helper.getLevel(), profile);
        new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), player);
        return player;
        *///?}
    }
}
