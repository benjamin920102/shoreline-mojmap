package meteordevelopment.discordipc.connection;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.function.Consumer;
import meteordevelopment.discordipc.Opcode;
import meteordevelopment.discordipc.Packet;

public abstract class Connection {
   private static final String[] UNIX_TEMP_PATHS = new String[]{"XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP"};

   public static Connection open(Consumer<Packet> callback) {
      String os = System.getProperty("os.name").toLowerCase();
      if (os.contains("win")) {
         for (int i = 0; i < 10; i++) {
            try {
               return new WinConnection("\\\\?\\pipe\\discord-ipc-" + i, callback);
            } catch (IOException var7) {
            }
         }
      } else {
         String name = null;

         for (String tempPath : UNIX_TEMP_PATHS) {
            name = System.getenv(tempPath);
            if (name != null) {
               break;
            }
         }

         if (name == null) {
            name = "/tmp";
         }

         name = name + "/discord-ipc-";

         for (int i = 0; i < 10; i++) {
            try {
               return new UnixConnection(name + i, callback);
            } catch (IOException var8) {
            }
         }
      }

      return null;
   }

   public void write(Opcode opcode, JsonObject o) {
      o.addProperty("nonce", UUID.randomUUID().toString());
      byte[] d = o.toString().getBytes();
      ByteBuffer packet = ByteBuffer.allocate(d.length + 8);
      packet.putInt(Integer.reverseBytes(opcode.ordinal()));
      packet.putInt(Integer.reverseBytes(d.length));
      packet.put(d);
      packet.rewind();
      this.write(packet);
   }

   protected abstract void write(ByteBuffer var1);

   public abstract void close();
}
