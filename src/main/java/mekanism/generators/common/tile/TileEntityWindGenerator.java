package mekanism.generators.common.tile;

import io.netty.buffer.ByteBuf;
import javax.annotation.Nonnull;
import mekanism.api.Coord4D;
import mekanism.api.TileNetworkList;
import mekanism.common.base.IBoundingBlock;
import mekanism.common.config.MekanismConfig;
import mekanism.common.util.ChargeUtils;
import mekanism.common.util.MekanismUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;

public class TileEntityWindGenerator extends TileEntityGenerator implements IBoundingBlock {

    private static final int[] SLOTS = {0};

    public static final float SPEED = 32F;
    public static final float SPEED_SCALED = 256F / SPEED;
    static final String[] methods = new String[]{"getEnergy", "getOutput", "getMaxEnergy", "getEnergyNeeded", "getMultiplier"};

    private double angle;
    private float currentMultiplier;
    private boolean isBlacklistDimension = false;

    public TileEntityWindGenerator() {
        super("wind", "WindGenerator", 200000, MekanismConfig.current().generators.windGenerationMax.val() * 2);
        inventory = NonNullList.withSize(SLOTS.length, ItemStack.EMPTY);
    }

    @Override
    public void onLoad() {
        super.onLoad();

        // Check the blacklist and force an update if we're in the blacklist. Otherwise, we'll never send
        // an initial activity status and the client (in MP) will show the windmills turning while not
        // generating any power
        isBlacklistDimension = MekanismConfig.current().generators.windGenerationDimBlacklist.val().contains(world.provider.getDimension());
        if (isBlacklistDimension) {
            setActive(false);
        }
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (!world.isRemote) {
            ChargeUtils.charge(0, this);
            // If we're in a blacklisted dimension, there's nothing more to do
            if (isBlacklistDimension) {
                return;
            }
            if (ticker % 20 == 0) {
                // Recalculate the current multiplier once a second
                currentMultiplier = getMultiplier();
                setActive(currentMultiplier > 0);
            }
            if (getActive()) {
                setEnergy(electricityStored + (MekanismConfig.current().generators.windGenerationMin.val() * currentMultiplier));
            }
        } else if (getActive()) {
            angle = (angle + (getPos().getY() + 4F) / SPEED_SCALED) % 360;
        }
    }

    @Override
    public void handlePacketData(ByteBuf dataStream) {
        super.handlePacketData(dataStream);

        if (world.isRemote) {
            currentMultiplier = dataStream.readFloat();
            isBlacklistDimension = dataStream.readBoolean();
        }
    }

    @Override
    public TileNetworkList getNetworkedData(TileNetworkList data) {
        super.getNetworkedData(data);
        data.add(currentMultiplier);
        data.add(isBlacklistDimension);
        return data;
    }

    /**
     * Determines the current output multiplier, taking sky visibility and height into account.
     **/
    public float getMultiplier() {
        if (world.canSeeSky(getPos().add(0, 4, 0))) {
            final float minY = (float) MekanismConfig.current().generators.windGenerationMinY.val();
            final float maxY = (float) MekanismConfig.current().generators.windGenerationMaxY.val();
            final float minG = (float) MekanismConfig.current().generators.windGenerationMin.val();
            final float maxG = (float) MekanismConfig.current().generators.windGenerationMax.val();
            final float slope = (maxG - minG) / (maxY - minY);
            final float intercept = minG - slope * minY;
            final float clampedY = Math.min(maxY, Math.max(minY, (float) (getPos().getY() + 4)));
            final float toGen = slope * clampedY + intercept;
            return toGen / minG;
        }
        return 0;
    }

    @Override
    public String[] getMethods() {
        return methods;
    }

    @Override
    public Object[] invoke(int method, Object[] arguments) throws Exception {
        switch (method) {
            case 0:
                return new Object[]{electricityStored};
            case 1:
                return new Object[]{output};
            case 2:
                return new Object[]{BASE_MAX_ENERGY};
            case 3:
                return new Object[]{BASE_MAX_ENERGY - electricityStored};
            case 4:
                return new Object[]{getMultiplier()};
            default:
                throw new NoSuchMethodException();
        }
    }

    @Override
    public boolean canOperate() {
        return electricityStored < BASE_MAX_ENERGY && getMultiplier() > 0 && MekanismUtils.canFunction(this);
    }

    @Override
    public void onPlace() {
        Coord4D current = Coord4D.get(this);
        MekanismUtils.makeBoundingBlock(world, getPos().offset(EnumFacing.UP, 1), current);
        MekanismUtils.makeBoundingBlock(world, getPos().offset(EnumFacing.UP, 2), current);
        MekanismUtils.makeBoundingBlock(world, getPos().offset(EnumFacing.UP, 3), current);
        MekanismUtils.makeBoundingBlock(world, getPos().offset(EnumFacing.UP, 4), current);
        // Check to see if the placement is happening in a blacklisted dimension
        isBlacklistDimension = MekanismConfig.current().generators.windGenerationDimBlacklist.val().contains(world.provider.getDimension());
    }

    @Override
    public void onBreak() {
        world.setBlockToAir(getPos().add(0, 1, 0));
        world.setBlockToAir(getPos().add(0, 2, 0));
        world.setBlockToAir(getPos().add(0, 3, 0));
        world.setBlockToAir(getPos().add(0, 4, 0));
        world.setBlockToAir(getPos());
    }

    @Override
    public boolean renderUpdate() {
        return false;
    }

    @Override
    public boolean lightUpdate() {
        return false;
    }

    public float getCurrentMultiplier() {
        return currentMultiplier;
    }

    public double getAngle() {
        return angle;
    }

    public boolean isBlacklistDimension() {
        return isBlacklistDimension;
    }

    @Nonnull
    @Override
    public int[] getSlotsForFace(@Nonnull EnumFacing side) {
        return SLOTS;
    }

    @Override
    public boolean isItemValidForSlot(int slot, @Nonnull ItemStack stack) {
        return ChargeUtils.canBeCharged(stack);
    }
}