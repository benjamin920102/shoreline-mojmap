package net.shoreline.client.impl.gui.click.component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import net.minecraft.client.gui.navigation.ScreenRectangle;

public class ScissorStack
{
    //
    private final Deque<ScreenRectangle> stack = new ArrayDeque<>();

    /**
     * @param rect
     * @return
     */
    public ScreenRectangle push(ScreenRectangle rect)
    {
        ScreenRectangle screenRect = stack.peekLast();
        if (screenRect != null)
        {
            ScreenRectangle screenRect2 = Objects.requireNonNullElse(rect.intersection(screenRect), ScreenRectangle.empty());
            stack.addLast(screenRect2);
            return screenRect2;
        }
        else
        {
            stack.addLast(rect);
            return rect;
        }
    }

    /**
     * @return
     */
    public ScreenRectangle pop()
    {
        if (stack.isEmpty())
        {
            throw new IllegalStateException("Scissor stack underflow");
        }
        else
        {
            stack.removeLast();
            return stack.peekLast();
        }
    }
}
