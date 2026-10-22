package net.shoreline.client.mixin.network;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.impl.event.gui.chat.ChatMessageEvent;
import net.shoreline.client.impl.event.network.GameJoinEvent;
import net.shoreline.client.impl.event.network.InventoryEvent;
import net.shoreline.client.impl.event.network.ServerRotationEvent;
import net.shoreline.client.impl.event.world.LoadChunkBlockEvent;
import net.shoreline.client.impl.event.world.LoadChunkEvent;
import net.shoreline.client.impl.imixin.IClientPlayNetworkHandler;
import net.shoreline.client.mixin.accessor.AccessorClientConnection;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author linus
 * @since 1.0
 */
@Mixin(ClientPacketListener.class)
public abstract class MixinClientPlayNetworkHandler implements IClientPlayNetworkHandler, Globals
{
    @Shadow
    public abstract Connection getConnection();

    @Shadow
    private ClientLevel level;

    /**
     * @param content
     * @param ci
     */
    @Inject(method = "sendChat", at = @At(value = "HEAD"),
            cancellable = true)
    private void hookSendChatMessage(String content, CallbackInfo ci)
    {
        ChatMessageEvent.Server chatInputEvent =
                new ChatMessageEvent.Server(content);
        EventBus.INSTANCE.dispatch(chatInputEvent);
        // prevent chat packet from sending
        if (chatInputEvent.isCanceled())
        {
            ci.cancel();
        }
    }

    /**
     * @param packet
     * @param ci
     */
    @Inject(method = "handleLogin", at = @At(value = "TAIL"))
    private void hookOnGameJoin(ClientboundLoginPacket packet, CallbackInfo ci)
    {
        GameJoinEvent gameJoinEvent = new GameJoinEvent();
        EventBus.INSTANCE.dispatch(gameJoinEvent);
    }

    /**
     * @param packet
     * @param ci
     */
    @Inject(method = "handleContainerContent", at = @At(value = "TAIL"))
    private void hookOnInventory(ClientboundContainerSetContentPacket packet, CallbackInfo ci)
    {
        InventoryEvent inventoryEvent = new InventoryEvent(packet);
        EventBus.INSTANCE.dispatch(inventoryEvent);
    }

    @Inject(method = "handleLevelChunkWithLight", at = @At(value = "RETURN"))
    private void hookOnChunkData(ClientboundLevelChunkWithLightPacket packet, CallbackInfo ci)
    {
        LevelChunk chunk = level.getChunkSource().getChunk(packet.getX(), packet.getZ(), false);
        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();
        LoadChunkEvent loadChunkEvent = new LoadChunkEvent(chunk);
        EventBus.INSTANCE.dispatch(loadChunkEvent);
        for (int y = chunk.getMinBuildHeight(); y < chunk.getHeight(); y++)
        {
            for (int x1 = startX; x1 < startX + 16; x1++)
            {
                for (int z1 = startZ; z1 < startZ + 16; z1++)
                {
                    BlockPos pos = new BlockPos(x1, y, z1);
                    BlockState state = chunk.getBlockState(pos);
                    LoadChunkBlockEvent loadChunkBlockEvent = new LoadChunkBlockEvent(pos, state);
                    EventBus.INSTANCE.dispatch(loadChunkBlockEvent);
                }
            }
        }
    }

    @Inject(method = "handleMovePlayer", at = @At(value = "HEAD"), cancellable = true)
    private void onPlayerPositionLook(ClientboundPlayerPositionPacket packet, CallbackInfo ci)
    {
        ServerRotationEvent serverRotationEvent = new ServerRotationEvent();
        EventBus.INSTANCE.dispatch(serverRotationEvent);
        if (serverRotationEvent.isCanceled())
        {
            ci.cancel();
            double i;
            double h;
            double g;
            double f;
            double e;
            double d;
            PacketUtils.ensureRunningOnSameThread(packet, (ClientPacketListener) (Object) this, mc);
            LocalPlayer playerEntity = mc.player;
            Vec3 vec3d = playerEntity.getDeltaMovement();
            boolean bl = packet.getRelativeArguments().contains(RelativeMovement.X);
            boolean bl2 = packet.getRelativeArguments().contains(RelativeMovement.Y);
            boolean bl3 = packet.getRelativeArguments().contains(RelativeMovement.Z);
            if (bl) {
                d = vec3d.x();
                e = playerEntity.getX() + packet.getX();
                playerEntity.xOld += packet.getX();
                playerEntity.xo += packet.getX();
            } else {
                d = 0.0;
                playerEntity.xOld = e = packet.getX();
                playerEntity.xo = e;
            }
            if (bl2) {
                f = vec3d.y();
                g = playerEntity.getY() + packet.getY();
                playerEntity.yOld += packet.getY();
                playerEntity.yo += packet.getY();
            } else {
                f = 0.0;
                playerEntity.yOld = g = packet.getY();
                playerEntity.yo = g;
            }
            if (bl3) {
                h = vec3d.z();
                i = playerEntity.getZ() + packet.getZ();
                playerEntity.zOld += packet.getZ();
                playerEntity.zo += packet.getZ();
            } else {
                h = 0.0;
                playerEntity.zOld = i = packet.getZ();
                playerEntity.zo = i;
            }
            float yaw = serverRotationEvent.getYaw();
            float pitch = serverRotationEvent.getPitch();
            playerEntity.setPos(e, g, i);
            playerEntity.setDeltaMovement(d, f, h);
            getConnection().send(new ServerboundAcceptTeleportationPacket(packet.getId()));
            getConnection().send(new ServerboundMovePlayerPacket.PosRot(playerEntity.getX(), playerEntity.getY(),
                    playerEntity.getZ(), Float.isNaN(yaw) ? playerEntity.getYRot() : yaw,
                    Float.isNaN(pitch) ? playerEntity.getXRot() : pitch, false));
        }
    }

    @Override
    public void sendQuietPacket(Packet<?> packet)
    {
        ((AccessorClientConnection) getConnection()).hookSendInternal(packet, null, true);
    }
}
