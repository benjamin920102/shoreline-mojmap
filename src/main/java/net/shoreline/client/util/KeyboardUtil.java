package net.shoreline.client.util;

import org.lwjgl.glfw.GLFW;

public class KeyboardUtil {
   public static String getKeyName(int keycode, int scancode) {
      return switch (keycode) {
         case 32 -> "SPACE";
         case 257 -> "ENTER";
         case 258 -> "TAB";
         case 259 -> "BACKSPACE";
         case 260 -> "INSERT";
         case 261 -> "DELETE";
         case 262 -> "RIGHT";
         case 263 -> "LEFT";
         case 264 -> "DOWN";
         case 265 -> "UP";
         case 266 -> "PAGE_UP";
         case 267 -> "PAGE_DOWN";
         case 268 -> "HOME";
         case 269 -> "END";
         case 280 -> "CAPS_LOCK";
         case 340 -> "LSHIFT";
         case 341 -> "LCONTROL";
         case 342 -> "LALT";
         case 344 -> "RSHIFT";
         case 345 -> "RCONTROL";
         case 346 -> "RALT";
         case 1000 -> "MOUSE0";
         case 1001 -> "MOUSE1";
         case 1002 -> "MOUSE2";
         case 1003 -> "MOUSE3";
         case 1004 -> "MOUSE4";
         case 1005 -> "MOUSE5";
         case 1006 -> "MOUSE6";
         case 1007 -> "MOUSE7";
         default -> GLFW.glfwGetKeyName(keycode, scancode);
      };
   }

   public static String getKeyName(int keycode) {
      return getKeyName(keycode, keycode < 1000 ? GLFW.glfwGetKeyScancode(keycode) : 0);
   }

   public static int getKeyCode(String key) {
      if (key.equalsIgnoreCase("NONE")) {
         return -1;
      } else {
         for (int i = 32; i < 97; i++) {
            if (key.equalsIgnoreCase(getKeyName(i, GLFW.glfwGetKeyScancode(i)))) {
               return i;
            }
         }

         for (int ix = 256; ix < 349; ix++) {
            if (key.equalsIgnoreCase(getKeyName(ix, GLFW.glfwGetKeyScancode(ix)))) {
               return ix;
            }
         }

         for (int ixx = 1000; ixx < 1010; ixx++) {
            if (key.equalsIgnoreCase(getKeyName(ixx, GLFW.glfwGetKeyScancode(ixx)))) {
               return ixx;
            }
         }

         return -1;
      }
   }
}
