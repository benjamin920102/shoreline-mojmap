package net.shoreline.client.mixin.option;

import com.mojang.serialization.Codec;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import net.shoreline.client.impl.event.option.PerspectiveUpdateEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

@Mixin(Options.class)
public class MixinGameOptions
{

    @Mutable
    @Shadow
    @Final
    private OptionInstance<Integer> fov;

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;load()V", shift = At.Shift.BEFORE))
    private void hookInit(Minecraft client, File optionsFile, CallbackInfo ci)
    {
        fov = new OptionInstance<>("options.fov", OptionInstance.noTooltip(), (optionText, value) -> switch (value)
        {
            case 70 -> Options.genericValueLabel(optionText, Component.translatable("options.fov.min"));
            case 110 -> Options.genericValueLabel(optionText, Component.translatable("options.fov.max"));
            default -> Options.genericValueLabel(optionText, value);
        }, new OptionInstance.IntRange(30, 180), Codec.DOUBLE.xmap(value -> (int) (value * 40.0 + 70.0), value -> ((double) value.intValue() - 70.0) / 40.0), 70, value -> Minecraft.getInstance().levelRenderer.needsUpdate());
    }

    @Inject(method = "setCameraType", at = @At(value = "HEAD"))
    private void hookSetPerspective(CameraType perspective, CallbackInfo ci)
    {
        PerspectiveUpdateEvent perspectiveEvent = new PerspectiveUpdateEvent(perspective);
        EventBus.INSTANCE.dispatch(perspectiveEvent);
    }
}
