package oxidizing.test;

//? if neoforge
//import net.neoforged.fml.common.Mod;
//? if neoforge && >=1.21.5 {
/*import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
*///?}

// The NeoForge test mod. Before 1.21.5 NeoForge finds OxidizingGameTest through its @GameTestHolder;
// from 1.21.5 tests are registry entries: a test function plus a test instance placing the empty structure.
//? if neoforge && >=1.21.5 {
/*@Mod(TestMod.MOD_ID)
public class TestMod {
    static final String MOD_ID = "oxidizing_test";

    public TestMod(IEventBus modBus) {
        OxidizingGameTest tests = new OxidizingGameTest();
        Map<String, Consumer<GameTestHelper>> functions = new LinkedHashMap<>();
        functions.put("dry_sides_count_zero", tests::drySidesCountZero);
        functions.put("each_water_side_counts", tests::eachWaterSideCounts);
        functions.put("flowing_water_counts_lava_does_not", tests::flowingWaterCountsLavaDoesNot);
        functions.put("waterlogged_neighbour_counts", tests::waterloggedNeighbourCounts);
        functions.put("waterlogged_counts_as_all_sides", tests::waterloggedCountsAsAllSides);
        functions.put("wet_copper_advances_one_stage", tests::wetCopperAdvancesOneStage);
        functions.put("failed_roll_leaves_copper_but_skips_vanilla", tests::failedRollLeavesCopperButSkipsVanilla);
        functions.put("dry_copper_left_to_vanilla", tests::dryCopperLeftToVanilla);
        functions.put("fully_oxidized_unchanged", tests::fullyOxidizedUnchanged);
        functions.put("waxed_copper_unchanged", tests::waxedCopperUnchanged);
        functions.put("stairs_keep_their_shape", tests::stairsKeepTheirShape);
        functions.put("random_tick_oxidizes_waterlogged_copper", tests::randomTickOxidizesWaterloggedCopper);

        modBus.addListener(RegisterEvent.class, event -> event.register(Registries.TEST_FUNCTION, helper ->
                functions.forEach((name, function) -> helper.register(id(name), function))));
        modBus.addListener(RegisterGameTestsEvent.class, event -> {
            var environment = event.registerEnvironment(id("default"));
            for (String name : functions.keySet()) {
                ResourceKey<Consumer<GameTestHelper>> function = ResourceKey.create(Registries.TEST_FUNCTION, id(name));
                event.registerTest(id(name), new FunctionGameTestInstance(function, new TestData<>(environment, id("empty"), 20, 0, true)));
            }
        });
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
*///?} else if neoforge {
/*@Mod("oxidizing_test")
public class TestMod {
}
*///?}
