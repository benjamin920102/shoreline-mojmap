package net.shoreline.client.impl.module.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.EndCrystalItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.event.network.PlayerTickEvent;
import net.shoreline.client.impl.event.world.AddEntityEvent;
import net.shoreline.client.impl.event.world.RemoveEntityEvent;
import net.shoreline.client.impl.module.RotationModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.player.RotationUtil;
import net.shoreline.eventbus.annotation.EventListener;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ClickCrystalModule extends RotationModule
{

    Config<Float> breakDelayConfig = register(new NumberConfig<>("SpawnDelay", "Speed to break crystals after spawning", 0.0f, 0.0f, 20.0f));
    Config<Float> randomDelayConfig = register(new NumberConfig<>("RandomDelay", "Randomized break delay", 0.0f, 0.0f, 5.0f));
    Config<Boolean> rotateConfig = register(new BooleanConfig("Rotate", "Rotate before breaking", false));
    Config<Boolean> randomRotateConfig = register(new BooleanConfig("RotateJitter", "Slightly randomizes rotations", false, () -> rotateConfig.getValue()));
    private final Set<BlockPos> placedCrystals = new HashSet<>();
    private final Map<EndCrystal, Long> spawnedCrystals = new LinkedHashMap<>();
    private float randomDelay = -1;

    public ClickCrystalModule()
    {
        super("ClickCrystal", "Automatically breaks placed crystals", ModuleCategory.COMBAT);
    }

    @EventListener
    public void onPlayerTick(PlayerTickEvent event)
    {
        if (spawnedCrystals.isEmpty())
        {
            return;
        }
        Map.Entry<EndCrystal, Long> e = spawnedCrystals.entrySet().iterator().next();
        EndCrystal crystalEntity = e.getKey();
        Long time = e.getValue();
        if (randomDelay == -1)
        {
            randomDelay = randomDelayConfig.getValue() == 0.0f ? 0.0f : RANDOM.nextFloat(randomDelayConfig.getValue() * 25.0f);
        }
        float breakDelay = breakDelayConfig.getValue() * 50.0f + randomDelay;
        double dist = mc.player.getEyePosition().distanceToSqr(crystalEntity.position());
        if (dist <= 12.25 && System.currentTimeMillis() - time >= breakDelay)
        {
            if (rotateConfig.getValue())
            {
                Vec3 rotatePos = crystalEntity.position();
                if (randomRotateConfig.getValue())
                {
                    AABB bb = crystalEntity.getBoundingBox();
                    rotatePos = new Vec3(RANDOM.nextDouble(bb.minX, bb.maxX), RANDOM.nextDouble(bb.minY, bb.maxY), RANDOM.nextDouble(bb.minZ, bb.maxZ));
                }
                float[] rotations = RotationUtil.getRotationsTo(mc.player.getEyePosition(), rotatePos);
                setRotation(rotations[0], rotations[1]);
            }
            Managers.NETWORK.sendPacket(ServerboundInteractPacket.createAttackPacket(crystalEntity, mc.player.isShiftKeyDown()));
            mc.player.swing(InteractionHand.MAIN_HAND);
            randomDelay = -1;
        }
    }

    @EventListener
    public void onPacketOutbound(PacketEvent.Outbound event)
    {
        if (event.getPacket() instanceof ServerboundUseItemOnPacket packet && !event.isClientPacket() && mc.player.getItemInHand(packet.getHand()).getItem() instanceof EndCrystalItem)
        {
            placedCrystals.add(packet.getHitResult().getBlockPos());
        }
    }

    @EventListener
    public void onAddEntity(AddEntityEvent event)
    {
        if (event.getEntity() instanceof EndCrystal crystalEntity)
        {
            BlockPos base = crystalEntity.blockPosition().below();
            if (placedCrystals.contains(base))
            {
                spawnedCrystals.put(crystalEntity, System.currentTimeMillis());
                placedCrystals.remove(base);
            }
        }
    }

    @EventListener
    public void onRemoveEntity(RemoveEntityEvent event)
    {
        if (event.getEntity() instanceof EndCrystal crystalEntity)
        {
            spawnedCrystals.remove(crystalEntity);
        }
    }
}
