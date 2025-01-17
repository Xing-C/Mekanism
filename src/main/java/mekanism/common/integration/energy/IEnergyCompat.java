package mekanism.common.integration.energy;

import java.util.Collection;
import java.util.Collections;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.common.config.value.CachedValue;
import mekanism.common.util.CapabilityUtils;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

@NothingNullByDefault
public interface IEnergyCompat {

    /**
     * Whether this energy compat is actually enabled.
     *
     * @return if this energy compat is enabled.
     */
    boolean isUsable();

    /**
     * Gets the configs that back {@link #isUsable()} so that caching for usable and enabled energy types can be done.
     *
     * @implNote If this {@link IEnergyCompat} will never be usable due to missing required mods, this should just return an empty collection to allow the enabled caching
     * to skip listening to the corresponding config settings.
     */
    default Collection<CachedValue<?>> getBackingConfigs() {
        return Collections.emptySet();
    }

    /**
     * Gets the capability this compat integrates with.
     *
     * @return The capability this compat is integrating with.
     */
    Capability<?> getCapability();

    /**
     * Checks if a given capability matches the capability that this {@link IEnergyCompat} is for.
     *
     * @param capability Capability to check
     *
     * @return {@code true} if the capability matches, {@code false} if it doesn't.
     */
    default boolean isMatchingCapability(Capability<?> capability) {
        return capability == getCapability();
    }

    /**
     * Checks if the given provider has this capability.
     *
     * @param provider Capability provider
     * @param side     Side
     *
     * @return {@code true} if the provider has this {@link IEnergyCompat}'s capability, {@code false} otherwise
     *
     * @implNote The capabilities should be kept lazy so that they are not resolved if they are not needed yet.
     */
    default boolean isCapabilityPresent(ICapabilityProvider provider, @Nullable Direction side) {
        return CapabilityUtils.getCapability(provider, getCapability(), side).isPresent();
    }

    /**
     * Gets the {@link IStrictEnergyHandler} as a lazy optional for the capability this energy compat is for.
     *
     * @param handler The handler to wrap
     *
     * @return A lazy optional for this capability
     */
    LazyOptional<?> getHandlerAs(IStrictEnergyHandler handler);

    /**
     * Wraps the capability implemented in the provider into a lazy optional {@link IStrictEnergyHandler}, or returns {@code LazyOptional.empty()} if the capability is
     * not implemented.
     *
     * @param provider Capability provider
     * @param side     Side
     *
     * @return The capability implemented in the provider into an {@link IStrictEnergyHandler}, or {@code null} if the capability is not implemented.
     */
    LazyOptional<IStrictEnergyHandler> getLazyStrictEnergyHandler(ICapabilityProvider provider, @Nullable Direction side);
}