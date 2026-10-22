package net.shoreline.eventbus.event;

public class StageEvent extends Event {
   private StageEvent.EventStage stage;

   public StageEvent.EventStage getStage() {
      return this.stage;
   }

   public void setStage(StageEvent.EventStage stage) {
      this.stage = stage;
   }

   public static enum EventStage {
      PRE,
      POST;
   }
}
