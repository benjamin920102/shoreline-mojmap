package net.shoreline.client.impl.module.render;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.event.gui.RenderTooltipEvent;
import net.shoreline.client.impl.module.client.ColorsModule;
import net.shoreline.client.util.world.ItemUtil;
import net.shoreline.eventbus.annotation.EventListener;

import java.util.List;

/**
 * @author linus
 * @since 1.0
 */
public class TooltipsModule extends ToggleModule
{

    // Config<Boolean> enderChestsConfig = register(new BooleanConfig("EnderChests", "Renders all the contents of ender chests in tooltips", false);
    Config<Boolean> shulkersConfig = register(new BooleanConfig("Shulkers", "Renders all the contents of shulkers in tooltips", true));
    Config<Boolean> mapsConfig = register(new BooleanConfig("Maps", "Renders a preview of maps in tooltips", false));

    public TooltipsModule()
    {
        super("Tooltips", "Renders detailed tooltips showing items", ModuleCategory.RENDER);
    }

    @EventListener
    public void onRenderTooltip(RenderTooltipEvent event)
    {
        final ItemStack stack = event.getStack();
        if (stack.isEmpty())
        {
            return;
        }

        if (shulkersConfig.getValue() && ItemUtil.isShulker(stack.getItem())) {
            ItemContainerContents container = stack.get(DataComponents.CONTAINER);
            if (container == null || container.nonEmptyStream().findAny().isEmpty()) {
                return;
            }

            event.cancel();
            event.context.pose().pushPose();
            event.context.pose().translate(0.0f, 0.0f, 600.0f);

            NonNullList<ItemStack> shulkerInventory = NonNullList.withSize(27, ItemStack.EMPTY);

            container.stream()
                    .filter(itemStack -> !itemStack.isEmpty())
                    .forEach(itemStack -> {
                        for (int i = 0; i < shulkerInventory.size(); i++) {
                            if (shulkerInventory.get(i).isEmpty()) {
                                shulkerInventory.set(i, itemStack.copy());
                                break;
                            }
                        }
                    });

            RenderManager.rect(event.context.pose(), event.getX() + 8.0,
                    event.getY() - 21.0, 150.0, 13.0, ColorsModule.getInstance().getRGB(170));

            RenderManager.enableScissor(event.getX() + 8.0, event.getY() - 21.0, event.getX() + 158.0, event.getY() - 8.0);
            RenderManager.renderText(event.getContext(), stack.getHoverName().getString(), event.getX() + 11.0f, event.getY() - 18.0f, -1);
            RenderManager.disableScissor();

            RenderManager.rect(event.context.pose(), event.getX() + 8.0, event.getY() - 8.0, 150.0, 55.0, 0x77000000);

            for (int i = 0; i < shulkerInventory.size(); i++) {
                ItemStack itemStack = shulkerInventory.get(i);
                if (!itemStack.isEmpty()) {
                    int x = event.getX() + (i % 9) * 16 + 9;
                    int y = event.getY() + (i / 9) * 16 - 5;
                    event.context.renderItem(itemStack, x, y);
                    event.context.renderItemDecorations(mc.font, itemStack, x, y);
                }
            }
            event.context.pose().popPose();
        }
        else if (mapsConfig.getValue() && stack.getItem() instanceof MapItem)
        {
            event.cancel();
            event.context.pose().pushPose();
            event.context.pose().translate(0.0f, 0.0f, 600.0f);
            RenderManager.rect(event.context.pose(), event.getX() + 8.0,
                    event.getY() - 21.0, 128.0, 13.0, ColorsModule.getInstance().getRGB(170));

            RenderManager.enableScissor(event.getX() + 8.0,
                    event.getY() - 21.0, event.getX() + 132.0, event.getY() - 8.0);
            RenderManager.renderText(event.getContext(), stack.getHoverName().getString(),
                    event.getX() + 11.0f, event.getY() - 18.0f, -1);
            RenderManager.disableScissor();

            event.getContext().pose().translate(event.getX() + 8.0f, event.getY() - 8.0f, 0.0f);
            MapId mapIdComponent = stack.get(DataComponents.MAP_ID);
            MapItemSavedData mapState = MapItem.getSavedData(mapIdComponent, mc.level);
            if (mapState != null)
            {
                mc.gameRenderer.getMapRenderer().render(event.getContext().pose(), event.getContext().bufferSource(), mapIdComponent, mapState, true, 1);
            }

            event.context.pose().popPose();
        }
    }
}
