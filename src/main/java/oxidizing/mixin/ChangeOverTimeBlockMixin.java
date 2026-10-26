package oxidizing.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.ChangeOverTimeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import oxidizing.WetOxidation;

// The default method holding vanilla's oxidation roll, called by every copper block's randomTick after its own
// guards (door lower half, unopened chest...). Named onRandomTick up to 1.20.2, changeOverTime from 1.20.3: only one
// exists per version. The arguments come through @Local because the Random parameter's type changed in 1.19, and an
// @Inject handler can't leave out only some of the target's arguments
@Mixin(ChangeOverTimeBlock.class)
public interface ChangeOverTimeBlockMixin {

    @Inject(method = {"onRandomTick", "changeOverTime"}, at = @At("HEAD"), cancellable = true)
    private void oxidizing$wetCopper(CallbackInfo ci, @Local(argsOnly = true) BlockState state,
                                     @Local(argsOnly = true) ServerLevel level, @Local(argsOnly = true) BlockPos pos) {
        if (WetOxidation.tryOxidize(state, level, pos, level.getRandom().nextFloat())) ci.cancel();
    }
}
