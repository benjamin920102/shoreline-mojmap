package net.shoreline.client.impl.module.render;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.world.level.biome.AmbientParticleSettings;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.biome.BiomeEffectsEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.util.string.EnumFormatter;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

/**
 * @author linus
 * @since 1.0
 */
public class NoWeatherModule extends ToggleModule
{

    Config<Weather> weatherConfig = register(new EnumConfig<>("Weather", "The world weather", Weather.CLEAR, Weather.values()));
    // The current weather mode
    private Weather weather;

    public NoWeatherModule()
    {
        super("NoWeather", "Prevents weather rendering", ModuleCategory.RENDER);
    }

    @Override
    public String getModuleData()
    {
        return EnumFormatter.formatEnum(weatherConfig.getValue());
    }

    @Override
    public void onEnable()
    {
        if (mc.level != null)
        {
            if (mc.level.isThundering())
            {
                weather = Weather.THUNDER;
            }
            else if (mc.level.isRaining())
            {
                weather = Weather.RAIN;
            }
            else
            {
                weather = Weather.CLEAR;
            }
            setWeather(weatherConfig.getValue());
        }
    }

    @Override
    public void onDisable()
    {
        if (mc.level != null && weather != null)
        {
            setWeather(weather);
        }
    }

    @EventListener
    public void onTick(TickEvent event)
    {
        if (event.getStage() == StageEvent.EventStage.POST)
        {
            setWeather(weatherConfig.getValue());
        }
    }

    @EventListener
    public void onBiomeEffects(BiomeEffectsEvent event)
    {
        if (weatherConfig.getValue() == Weather.ASH)
        {
            event.cancel();
            event.setParticleConfig(new AmbientParticleSettings(ParticleTypes.WHITE_ASH, 0.118093334f));
        }
    }

    private void setWeather(Weather weather)
    {
        switch (weather)
        {
            case CLEAR, ASH ->
            {
                mc.level.getLevelData().setRaining(false);
                mc.level.setRainLevel(0.0f);
                mc.level.setThunderLevel(0.0f);
            }
            case RAIN ->
            {
                mc.level.getLevelData().setRaining(true);
                mc.level.setRainLevel(1.0f);
                mc.level.setThunderLevel(0.0f);
            }
            case THUNDER ->
            {
                mc.level.getLevelData().setRaining(true);
                mc.level.setRainLevel(2.0f);
                mc.level.setThunderLevel(1.0f);
            }
        }
    }

    @EventListener
    public void onPacketInbound(PacketEvent.Inbound event)
    {
        if (event.getPacket() instanceof ClientboundGameEventPacket packet)
        {
            if (packet.getEvent() == ClientboundGameEventPacket.START_RAINING
                    || packet.getEvent() == ClientboundGameEventPacket.STOP_RAINING
                    || packet.getEvent() == ClientboundGameEventPacket.RAIN_LEVEL_CHANGE
                    || packet.getEvent() == ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE)
            {
                event.cancel();
            }
        }
    }

    public enum Weather
    {
        CLEAR,
        RAIN,
        THUNDER,
        ASH
    }
}
