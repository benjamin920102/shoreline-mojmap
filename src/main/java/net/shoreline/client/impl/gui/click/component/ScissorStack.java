package net.shoreline.client.impl.gui.click.component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import net.minecraft.client.gui.navigation.ScreenRectangle;

public class ScissorStack {
   private final Deque<ScreenRectangle> stack = new ArrayDeque<>();

   public ScreenRectangle push(ScreenRectangle rect) {
      ScreenRectangle screenRect = this.stack.peekLast();
      if (screenRect != null) {
         ScreenRectangle screenRect2 = Objects.requireNonNullElse(rect.intersection(screenRect), ScreenRectangle.empty());
         this.stack.addLast(screenRect2);
         return screenRect2;
      } else {
         this.stack.addLast(rect);
         return rect;
      }
   }

   public ScreenRectangle pop() {
      if (this.stack.isEmpty()) {
         throw new IllegalStateException("Scissor stack underflow");
      } else {
         this.stack.removeLast();
         return this.stack.peekLast();
      }
   }
}
