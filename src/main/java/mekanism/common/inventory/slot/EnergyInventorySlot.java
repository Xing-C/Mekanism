package mekanism.common.inventory.slot;

import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import mekanism.api.annotations.FieldsAreNonnullByDefault;
import mekanism.api.annotations.NonNull;
import mekanism.api.energy.IEnergizedItem;
import mekanism.api.energy.IMekanismStrictEnergyHandler;
import mekanism.api.energy.IStrictEnergyStorage;
import mekanism.api.inventory.AutomationType;
import mekanism.api.inventory.IMekanismInventory;
import mekanism.common.config.MekanismConfig;
import mekanism.common.integration.forgeenergy.ForgeEnergyIntegration;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.util.MekanismUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EnergyInventorySlot extends BasicInventorySlot {

    //Cache the predicates as we only really need one instance of them
    private static final BiPredicate<@NonNull ItemStack, @NonNull AutomationType> dischargeExtractPredicate = (stack, automationType) -> {
        if (automationType == AutomationType.MANUAL) {
            //Always allow extracting manually
            return true;
        }
        //Used to be ChargeUtils#canBeOutputted(stack, false)
        if (stack.getItem() instanceof IEnergizedItem) {
            return ((IEnergizedItem) stack.getItem()).getEnergy(stack) == 0;
        }
        if (MekanismUtils.useForge()) {
            Optional<IEnergyStorage> forgeCapability = MekanismUtils.toOptional(stack.getCapability(CapabilityEnergy.ENERGY));
            if (forgeCapability.isPresent()) {
                IEnergyStorage storage = forgeCapability.get();
                return !storage.canExtract() || storage.extractEnergy(1, true) == 0;
            }
        }
        return true;
    };
    private static final BiPredicate<@NonNull ItemStack, @NonNull AutomationType> dischargeInsertPredicate = (stack, automationType) -> {
        //Used to be contained in ChargeUtils#canBeDischarged
        if (stack.getItem() instanceof IEnergizedItem) {
            IEnergizedItem energizedItem = (IEnergizedItem) stack.getItem();
            if (energizedItem.canSend(stack) && energizedItem.getEnergy(stack) > 0) {
                return true;
            }
        }
        if (MekanismUtils.useForge()) {
            Optional<IEnergyStorage> forgeCapability = MekanismUtils.toOptional(stack.getCapability(CapabilityEnergy.ENERGY));
            if (forgeCapability.isPresent() && forgeCapability.get().extractEnergy(1, true) > 0) {
                return true;
            }
        }
        return stack.getItem() == Items.REDSTONE;
    };
    private static final BiPredicate<@NonNull ItemStack, @NonNull AutomationType> chargeExtractPredicate = (stack, automationType) -> {
        if (automationType == AutomationType.MANUAL) {
            //Always allow extracting manually
            return true;
        }
        //Used to be ChargeUtils#canBeOutputted(stack, true)
        if (stack.getItem() instanceof IEnergizedItem) {
            IEnergizedItem energized = (IEnergizedItem) stack.getItem();
            return energized.getEnergy(stack) == energized.getMaxEnergy(stack);
        }
        if (MekanismUtils.useForge()) {
            Optional<IEnergyStorage> forgeCapability = MekanismUtils.toOptional(stack.getCapability(CapabilityEnergy.ENERGY));
            if (forgeCapability.isPresent()) {
                IEnergyStorage storage = forgeCapability.get();
                return !storage.canReceive() || storage.receiveEnergy(1, true) == 0;
            }
        }
        return true;
    };
    private static final BiPredicate<@NonNull ItemStack, @NonNull AutomationType> chargeInsertPredicate = (stack, automationType) -> {
        //Used to be ChargeUtils#canBeCharged
        if (stack.getItem() instanceof IEnergizedItem) {
            IEnergizedItem energizedItem = (IEnergizedItem) stack.getItem();
            if (energizedItem.canReceive(stack)) {
                //TODO: FIX THIS IN 1.12 as well, it can only be charged if we have less energy than the max energy we can store
                if (energizedItem.getEnergy(stack) < energizedItem.getMaxEnergy(stack)) {
                    return true;
                }
            }
        }
        if (MekanismUtils.useForge()) {
            Optional<IEnergyStorage> forgeCapability = MekanismUtils.toOptional(stack.getCapability(CapabilityEnergy.ENERGY));
            if (forgeCapability.isPresent() && forgeCapability.get().receiveEnergy(1, true) > 0) {
                return true;
            }
        }
        return false;
    };
    private static final Predicate<@NonNull ItemStack> validPredicate = stack -> {
        //Used to be ChargeUtils#isEnergyItem
        if (stack.getItem() instanceof IEnergizedItem) {
            //Always return true, even if it cannot currently receive or send energy, as it might have drained/charged and not support the other
            return true;
        }
        if (MekanismUtils.useForge()) {
            Optional<IEnergyStorage> forgeCapability = MekanismUtils.toOptional(stack.getCapability(CapabilityEnergy.ENERGY));
            if (forgeCapability.isPresent() && forgeCapability.get().canExtract()) {
                return true;
            }
        }
        return stack.getItem() == Items.REDSTONE;
    };

    /**
     * Takes energy from the item
     */
    public static EnergyInventorySlot discharge(@Nullable IMekanismInventory inventory, int x, int y) {
        return new EnergyInventorySlot(dischargeExtractPredicate, dischargeInsertPredicate, inventory, x, y);
    }

    /**
     * Gives energy to the item
     */
    public static EnergyInventorySlot charge(@Nullable IMekanismInventory inventory, int x, int y) {
        return new EnergyInventorySlot(chargeExtractPredicate, chargeInsertPredicate, inventory, x, y);
    }

    private EnergyInventorySlot(BiPredicate<@NonNull ItemStack, @NonNull AutomationType> canExtract, BiPredicate<@NonNull ItemStack, @NonNull AutomationType> canInsert,
          @Nullable IMekanismInventory inventory, int x, int y) {
        super(canExtract, canInsert, validPredicate, inventory, x, y);
        setSlotType(ContainerSlotType.POWER);
        setSlotOverlay(SlotOverlay.POWER);
    }

    //TODO: Should we make this slot keep track of an IStrictEnergyStorage AND also then make some sort of "ITickableSlot" or something that lets us tick a bunch
    // of slots at once instead of having to manually call the relevant methods
    public void discharge(IMekanismStrictEnergyHandler handler) {
        if (!isEmpty() && handler.getEnergy() < handler.getMaxEnergy()) {
            if (current.getItem() instanceof IEnergizedItem) {
                IEnergizedItem energizedItem = (IEnergizedItem) current.getItem();
                if (energizedItem.canSend(current)) {
                    double currentStoredEnergy = energizedItem.getEnergy(current);
                    double energyToTransfer = Math.min(energizedItem.getMaxTransfer(current), Math.min(currentStoredEnergy, handler.getMaxEnergy() - handler.getEnergy()));
                    if (energyToTransfer > 0) {
                        //Update the energy in the item
                        energizedItem.setEnergy(current, currentStoredEnergy - energyToTransfer);
                        //Update the energy in the IStrictEnergyStorage
                        handler.setEnergy(handler.getEnergy() + energyToTransfer);
                        onContentsChanged();
                        return;
                    }
                }
            }
            if (MekanismUtils.useForge()) {
                Optional<IEnergyStorage> forgeCapability = MekanismUtils.toOptional(current.getCapability(CapabilityEnergy.ENERGY));
                if (forgeCapability.isPresent()) {
                    IEnergyStorage storage = forgeCapability.get();
                    if (storage.canExtract()) {
                        int needed = ForgeEnergyIntegration.toForge(handler.getMaxEnergy() - handler.getEnergy());
                        handler.setEnergy(handler.getEnergy() + ForgeEnergyIntegration.fromForge(storage.extractEnergy(needed, false)));
                        onContentsChanged();
                        //Exit early as we successfully discharged
                        return;
                    }
                }
            }
            if (current.getItem() == Items.REDSTONE && handler.getEnergy() + MekanismConfig.general.ENERGY_PER_REDSTONE.get() <= handler.getMaxEnergy()) {
                handler.setEnergy(handler.getEnergy() + MekanismConfig.general.ENERGY_PER_REDSTONE.get());
                current.shrink(1);
                onContentsChanged();
            }
        }
    }

    public void charge(IMekanismStrictEnergyHandler handler) {
        if (!isEmpty() && handler.getEnergy() > 0) {
            if (current.getItem() instanceof IEnergizedItem) {
                IEnergizedItem energizedItem = (IEnergizedItem) current.getItem();
                if (energizedItem.canReceive(current)) {
                    double storedEnergy = handler.getEnergy();
                    double itemStoredEnergy = energizedItem.getEnergy(current);
                    double energyToTransfer = Math.min(energizedItem.getMaxTransfer(current), Math.min(energizedItem.getMaxEnergy(current) - itemStoredEnergy, storedEnergy));
                    if (energyToTransfer > 0) {
                        //Update the energy in the item
                        energizedItem.setEnergy(current, itemStoredEnergy + energyToTransfer);
                        //Update the energy in the IStrictEnergyStorage
                        handler.setEnergy(storedEnergy - energyToTransfer);
                        onContentsChanged();
                        return;
                    }
                }
            }
            if (MekanismUtils.useForge()) {
                Optional<IEnergyStorage> forgeCapability = MekanismUtils.toOptional(current.getCapability(CapabilityEnergy.ENERGY));
                if (forgeCapability.isPresent()) {
                    IEnergyStorage storage = forgeCapability.get();
                    if (storage.canReceive()) {
                        int stored = ForgeEnergyIntegration.toForge(handler.getEnergy());
                        handler.setEnergy(handler.getEnergy() - ForgeEnergyIntegration.fromForge(storage.receiveEnergy(stored, false)));
                        onContentsChanged();
                        //Exit early as we successfully discharged
                        return;
                    }
                }
            }
        }
    }
}