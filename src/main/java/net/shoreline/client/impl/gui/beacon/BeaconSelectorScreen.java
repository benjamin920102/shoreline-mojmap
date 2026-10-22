package net.shoreline.client.impl.gui.beacon;

import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.BeaconMenu;

/**
 * @author Shoreline
 * @since 1.0
 */
public class BeaconSelectorScreen extends BeaconScreen
{
    public BeaconSelectorScreen(BeaconMenu handler,
                                Inventory inventory,
                                Component title)
    {
        super(handler, inventory, title);
    }

    /**
     *
     */
    @Override
    public void init()
    {
        super.init();
    }

    /**
     *
     */
    @Override
    public void containerTick()
    {
        super.containerTick();
    }
}
