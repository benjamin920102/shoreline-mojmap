package net.shoreline.client.util.string;

import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;

public class EnumFormatter {
   public static String formatEnum(Enum<?> in) {
      String name = in.name();
      if (!name.contains("_")) {
         char firstChar = name.charAt(0);
         String suffixChars = name.split(String.valueOf(firstChar), 2)[1];
         return String.valueOf(firstChar).toUpperCase() + suffixChars.toLowerCase();
      } else {
         String[] names = name.split("_");
         StringBuilder nameToReturn = new StringBuilder();

         for (String n : names) {
            char firstChar = n.charAt(0);
            String suffixChars = n.split(String.valueOf(firstChar), 2)[1];
            nameToReturn.append(String.valueOf(firstChar).toUpperCase()).append(suffixChars.toLowerCase());
         }

         return nameToReturn.toString();
      }
   }

   public static String formatDirection(Direction direction) {
      return switch (direction) {
         case UP -> "Up";
         case DOWN -> "Down";
         case NORTH -> "North";
         case SOUTH -> "South";
         case EAST -> "East";
         case WEST -> "West";
      };
   }

   public static String formatAxis(Axis axis) {
      return switch (axis) {
         case X -> "X";
         case Y -> "Y";
         case Z -> "Z";
      };
   }
}
