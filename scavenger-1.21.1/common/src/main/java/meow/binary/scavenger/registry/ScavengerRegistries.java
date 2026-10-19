package meow.binary.scavenger.registry;

import meow.binary.scavenger.data.modifier.ScavengerModifier;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ScavengerRegistries {
    private static final Map<ResourceLocation, ScavengerModifier> MODIFIERS = new HashMap<>();

    public static ScavengerModifier register(ResourceLocation id, ScavengerModifier modifier) {
        modifier.setId(id);
        MODIFIERS.put(id, modifier);
        return modifier;
    }

    public static ScavengerModifier get(ResourceLocation id) {
        return MODIFIERS.get(id);
    }

    public static Set<ResourceLocation> getIds() {
        return MODIFIERS.keySet();
    }
}
