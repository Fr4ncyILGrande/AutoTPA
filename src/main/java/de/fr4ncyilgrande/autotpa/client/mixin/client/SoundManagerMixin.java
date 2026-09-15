package de.fr4ncyilgrande.autotpa.mixin.client;

import de.fr4ncyilgrande.autotpa.client.tpa.SoundSuppressionState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(SoundManager.class)
public abstract class SoundManagerMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("AutoTPA/Sound");

    @Inject(
            method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sound/SoundManager$Sound;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void autotpa$onPlay(SoundInstance sound,
                               CallbackInfoReturnable<SoundManager.Sound> cir) {
        if (sound == null) return;

        ResourceLocation id = sound.getLocation();
        if (id == null) return;

        String soundId = id.toString();

        if (SoundSuppressionState.isDebugMode()
                && SoundSuppressionState.isWithinCorrelationWindow()) {
            LOGGER.info("[AutoTPA][sound] Played '{}' shortly after a blocked TPA request "
                            + "was hidden. Add it to suppressedSoundIds in config/autotpa.json "
                            + "if it belongs to that request.",
                    soundId);
        }

        if (SoundSuppressionState.shouldSuppress(soundId)) {
            cir.setReturnValue(null);
        }
    }
}