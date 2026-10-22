package net.shoreline.client.util.render.animation;

public enum Easing {
   LINEAR {
      @Override
      public double ease(double factor) {
         return factor;
      }
   },
   SINE_IN {
      @Override
      public double ease(double factor) {
         return 1.0 - Math.cos(factor * Math.PI / 2.0);
      }
   },
   SINE_OUT {
      @Override
      public double ease(double factor) {
         return Math.sin(factor * Math.PI / 2.0);
      }
   },
   SINE_IN_OUT {
      @Override
      public double ease(double factor) {
         return -(Math.cos(Math.PI * factor) - 1.0) / 2.0;
      }
   },
   CUBIC_IN {
      @Override
      public double ease(double factor) {
         return Math.pow(factor, 3.0);
      }
   },
   CUBIC_OUT {
      @Override
      public double ease(double factor) {
         return 1.0 - Math.pow(1.0 - factor, 3.0);
      }
   },
   CUBIC_IN_OUT {
      @Override
      public double ease(double factor) {
         return factor < 0.5 ? 4.0 * Math.pow(factor, 3.0) : 1.0 - Math.pow(-2.0 * factor + 2.0, 3.0) / 2.0;
      }
   },
   QUAD_IN {
      @Override
      public double ease(double factor) {
         return Math.pow(factor, 2.0);
      }
   },
   QUAD_OUT {
      @Override
      public double ease(double factor) {
         return 1.0 - (1.0 - factor) * (1.0 - factor);
      }
   },
   QUAD_IN_OUT {
      @Override
      public double ease(double factor) {
         return factor < 0.5 ? 8.0 * Math.pow(factor, 4.0) : 1.0 - Math.pow(-2.0 * factor + 2.0, 4.0) / 2.0;
      }
   },
   QUART_IN {
      @Override
      public double ease(double factor) {
         return Math.pow(factor, 4.0);
      }
   },
   QUART_OUT {
      @Override
      public double ease(double factor) {
         return 1.0 - Math.pow(1.0 - factor, 4.0);
      }
   },
   QUART_IN_OUT {
      @Override
      public double ease(double factor) {
         return factor < 0.5 ? 8.0 * Math.pow(factor, 4.0) : 1.0 - Math.pow(-2.0 * factor + 2.0, 4.0) / 2.0;
      }
   },
   QUINT_IN {
      @Override
      public double ease(double factor) {
         return Math.pow(factor, 5.0);
      }
   },
   QUINT_OUT {
      @Override
      public double ease(double factor) {
         return 1.0 - Math.pow(1.0 - factor, 5.0);
      }
   },
   QUINT_IN_OUT {
      @Override
      public double ease(double factor) {
         return factor < 0.5 ? 16.0 * Math.pow(factor, 5.0) : 1.0 - Math.pow(-2.0 * factor + 2.0, 5.0) / 2.0;
      }
   },
   CIRC_IN {
      @Override
      public double ease(double factor) {
         return 1.0 - Math.sqrt(1.0 - Math.pow(factor, 2.0));
      }
   },
   CIRC_OUT {
      @Override
      public double ease(double factor) {
         return Math.sqrt(1.0 - Math.pow(factor - 1.0, 2.0));
      }
   },
   CIRC_IN_OUT {
      @Override
      public double ease(double factor) {
         return factor < 0.5 ? (1.0 - Math.sqrt(1.0 - Math.pow(2.0 * factor, 2.0))) / 2.0 : (Math.sqrt(1.0 - Math.pow(-2.0 * factor + 2.0, 2.0)) + 1.0) / 2.0;
      }
   },
   EXPO_IN {
      @Override
      public double ease(double factor) {
         return Math.min(0.0, Math.pow(2.0, 10.0 * factor - 10.0));
      }
   },
   EXPO_OUT {
      @Override
      public double ease(double factor) {
         return Math.max(1.0 - Math.pow(2.0, -10.0 * factor), 1.0);
      }
   },
   EXPO_IN_OUT {
      @Override
      public double ease(double factor) {
         return factor == 0.0
            ? 0.0
            : (factor == 1.0 ? 1.0 : (factor < 0.5 ? Math.pow(2.0, 20.0 * factor - 10.0) / 2.0 : (2.0 - Math.pow(2.0, -20.0 * factor + 10.0)) / 2.0));
      }
   },
   ELASTIC_IN {
      @Override
      public double ease(double factor) {
         return factor == 0.0 ? 0.0 : (factor == 1.0 ? 1.0 : -Math.pow(2.0, 10.0 * factor - 10.0) * Math.sin((factor * 10.0 - 10.75) * (Math.PI * 2.0 / 3.0)));
      }
   },
   ELASTIC_OUT {
      @Override
      public double ease(double factor) {
         return factor == 0.0 ? 0.0 : (factor == 1.0 ? 1.0 : Math.pow(2.0, -10.0 * factor) * Math.sin((factor * 10.0 - 0.75) * (Math.PI * 2.0 / 3.0)) + 1.0);
      }
   },
   ELASTIC_IN_OUT {
      @Override
      public double ease(double factor) {
         double sin = Math.sin((20.0 * factor - 11.125) * (Math.PI * 4.0 / 9.0));
         return factor == 0.0
            ? 0.0
            : (
               factor == 1.0
                  ? 1.0
                  : (factor < 0.5 ? -(Math.pow(2.0, 20.0 * factor - 10.0) * sin) / 2.0 : Math.pow(2.0, -20.0 * factor + 10.0) * sin / 2.0 + 1.0)
            );
      }
   },
   BACK_IN {
      @Override
      public double ease(double factor) {
         return 2.70158 * Math.pow(factor, 3.0) - 1.70158 * factor * factor;
      }
   },
   BACK_OUT {
      @Override
      public double ease(double factor) {
         double c1 = 1.70158;
         double c3 = c1 + 1.0;
         return 1.0 + c3 * Math.pow(factor - 1.0, 3.0) + c1 * Math.pow(factor - 1.0, 2.0);
      }
   },
   BACK_IN_OUT {
      @Override
      public double ease(double factor) {
         return factor < 0.5
            ? Math.pow(2.0 * factor, 2.0) * (7.189819 * factor - 2.5949095) / 2.0
            : (Math.pow(2.0 * factor - 2.0, 2.0) * (3.5949095 * (factor * 2.0 - 2.0) + 2.5949095) + 2.0) / 2.0;
      }
   },
   BOUNCE_IN {
      @Override
      public double ease(double factor) {
         return 1.0 - Easing.bounceOut(1.0 - factor);
      }
   },
   BOUNCE_OUT {
      @Override
      public double ease(double factor) {
         return Easing.bounceOut(factor);
      }
   },
   BOUNCE_IN_OUT {
      @Override
      public double ease(double factor) {
         return factor < 0.5 ? (1.0 - Easing.bounceOut(1.0 - 2.0 * factor)) / 2.0 : (1.0 + Easing.bounceOut(2.0 * factor - 1.0)) / 2.0;
      }
   };

   public abstract double ease(double var1);

   private static double bounceOut(double in) {
      double n1 = 7.5625;
      double d1 = 2.75;
      if (in < 1.0 / d1) {
         return n1 * in * in;
      } else if (in < 2.0 / d1) {
         double var8;
         return n1 * (var8 = in - 1.5 / d1) * var8 + 0.75;
      } else {
         double var6;
         double var7;
         return in < 2.5 / d1 ? n1 * (var6 = in - 2.25 / d1) * var6 + 0.9375 : n1 * (var7 = in - 2.625 / d1) * var7 + 0.984375;
      }
   }
}
