package net.shoreline.client.impl.module.misc;

import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.eventbus.annotation.EventListener;

import java.util.Set;

/**
 * @author linus
 * @since 1.0
 */
public class NoSoundLagModule extends ToggleModule
{
    //
    private final static Set<SoundEvent> LAG_SOUNDS = Set.of(
            SoundEvents.ARMOR_EQUIP_GENERIC.value(),
            SoundEvents.ARMOR_EQUIP_ELYTRA.value(),
            SoundEvents.ARMOR_EQUIP_NETHERITE.value(),
            SoundEvents.ARMOR_EQUIP_DIAMOND.value(),
            SoundEvents.ARMOR_EQUIP_IRON.value(),
            SoundEvents.ARMOR_EQUIP_GOLD.value(),
            SoundEvents.ARMOR_EQUIP_CHAIN.value(),
            SoundEvents.ARMOR_EQUIP_LEATHER.value()
    );

    /**
     *
     */
    public NoSoundLagModule()
    {
        super("NoSoundLag", "Prevents sound effects from lagging the game",
                ModuleCategory.MISCELLANEOUS);
    }

    @EventListener
    public void onPacketInbound(PacketEvent.Inbound event)
    {
        if (event.getPacket() instanceof ClientboundSoundEntityPacket packet
                && LAG_SOUNDS.contains(packet.getSound().value())
                || event.getPacket() instanceof ClientboundSoundPacket packet2
                && LAG_SOUNDS.contains(packet2.getSound().value()))
        {
            event.cancel();
        }
    }
}
