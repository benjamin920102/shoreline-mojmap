package net.shoreline.client.util.math;

import net.shoreline.client.util.Globals;

public class HexRandom implements Globals {
   private static final String HEX_CHARS = "0123456789abcdef";

   public static String generateRandomHex(int length) {
      StringBuilder hexString = new StringBuilder(length);

      for (int i = 0; i < length; i++) {
         int index = RANDOM.nextInt("0123456789abcdef".length());
         hexString.append("0123456789abcdef".charAt(index));
      }

      return hexString.toString();
   }
}
