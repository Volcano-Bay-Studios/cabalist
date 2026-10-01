package xyz.volcanobay.cabalist.system.spell;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.system.subject.Subject;

public record TriggerEvent(ResourceLocation type, Subject host, @Nullable Subject cause) {
}
