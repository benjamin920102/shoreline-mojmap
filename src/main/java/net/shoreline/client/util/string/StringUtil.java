package net.shoreline.client.util.string;

public class StringUtil {
   public static String capitalize(String string) {
      return string.length() != 0 ? Character.toTitleCase(string.charAt(0)) + string.substring(1) : "";
   }
}
