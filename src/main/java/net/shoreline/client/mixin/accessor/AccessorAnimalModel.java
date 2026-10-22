package net.shoreline.client.mixin.accessor;

import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AgeableListModel.class)
public interface AccessorAnimalModel
{
    @Invoker("bodyParts")
    Iterable<ModelPart> hookGetBodyParts();

    @Invoker("headParts")
    Iterable<ModelPart> hookGetHeadParts();

    @Accessor("scaleHead")
    boolean hookGetHeadScaled();

    @Accessor("babyHeadScale")
    float hookGetInvertedChildHeadScale();

    @Accessor("babyBodyScale")
    float hookGetInvertedChildBodyScale();

    @Accessor("babyYHeadOffset")
    float hookGetChildHeadYOffset();

    @Accessor("babyZHeadOffset")
    float hookGetChildHeadZOffset();

    @Accessor("bodyYOffset")
    float hookGetChildBodyYOffset();
}
