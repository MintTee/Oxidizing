package oxidizing;

//? if fabric
import net.fabricmc.api.ModInitializer;
//? if neoforge
//import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oxidizing.config.ConfigIO;

//? if fabric {
public class Main implements ModInitializer {
//?} else {
/*@Mod(Main.MOD_ID)
public class Main {
*///?}

    public static final String MOD_ID = "oxidizing";
    public static final Logger LOGGER = LoggerFactory.getLogger("Oxidizing");
    public static final String VERSION = /*$ mod_version*/ "1.0.0";
    public static final String MINECRAFT = /*$ minecraft*/ "1.21.11";

    //? if fabric {
    @Override
    public void onInitialize() {
    //?} else
    //public Main() {
        LOGGER.info("Oxidizing {} for MC {} initializing", VERSION, MINECRAFT);
        ConfigIO.load();
    }
}
