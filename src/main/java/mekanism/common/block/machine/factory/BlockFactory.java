package mekanism.common.block.machine.factory;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import mekanism.api.Upgrade;
import mekanism.api.block.FactoryType;
import mekanism.api.block.IBlockElectric;
import mekanism.api.block.IBlockSound;
import mekanism.api.block.IHasFactoryType;
import mekanism.api.block.IHasInventory;
import mekanism.api.block.IHasSecurity;
import mekanism.api.block.IHasTileEntity;
import mekanism.api.block.ISupportsComparator;
import mekanism.api.block.ISupportsRedstone;
import mekanism.api.block.ISupportsUpgrades;
import mekanism.common.Mekanism;
import mekanism.common.base.IActiveState;
import mekanism.common.base.IFactory.RecipeType;
import mekanism.common.block.BlockMekanismContainer;
import mekanism.common.block.interfaces.IHasGui;
import mekanism.common.block.interfaces.ITieredBlock;
import mekanism.common.block.machine.BlockChemicalInjectionChamber;
import mekanism.common.block.machine.BlockCombiner;
import mekanism.common.block.machine.BlockCrusher;
import mekanism.common.block.machine.BlockEnergizedSmelter;
import mekanism.common.block.machine.BlockEnrichmentChamber;
import mekanism.common.block.machine.BlockMetallurgicInfuser;
import mekanism.common.block.machine.BlockOsmiumCompressor;
import mekanism.common.block.machine.BlockPrecisionSawmill;
import mekanism.common.block.machine.BlockPurificationChamber;
import mekanism.common.block.states.IStateActive;
import mekanism.common.block.states.IStateFacing;
import mekanism.common.config.MekanismConfig;
import mekanism.common.inventory.container.ContainerProvider;
import mekanism.common.inventory.container.tile.FactoryContainer;
import mekanism.common.item.block.machine.factory.ItemBlockFactory;
import mekanism.common.tier.FactoryTier;
import mekanism.common.tile.base.MekanismTileEntityTypes;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.base.WrenchResult;
import mekanism.common.tile.factory.TileEntityFactory;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.SecurityUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.particles.RedstoneParticleData;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IEnviromentBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class BlockFactory extends BlockMekanismContainer implements IBlockElectric, ISupportsUpgrades, IHasGui<TileEntityFactory>, IStateFacing, IStateActive, IBlockSound,
      ITieredBlock<FactoryTier>, IHasFactoryType, IHasInventory, IHasSecurity, IHasTileEntity<TileEntityFactory>, ISupportsRedstone, ISupportsComparator {

    private final FactoryTier tier;
    private final FactoryType type;

    public BlockFactory(@Nonnull FactoryTier tier, @Nonnull FactoryType type) {
        super(Block.Properties.create(Material.IRON).hardnessAndResistance(3.5F, 16F));
        this.tier = tier;
        this.type = type;
        String name = tier.getBaseTier().getSimpleName().toLowerCase(Locale.ROOT) + "_" + type.getRegistryNameComponent() + "_factory";
        setRegistryName(new ResourceLocation(Mekanism.MODID, name));
    }

    @Override
    public FactoryTier getTier() {
        return tier;
    }

    @Nonnull
    @Override
    public FactoryType getFactoryType() {
        return type;
    }

    @Override
    public void setTileData(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack, @Nonnull TileEntityMekanism tile) {
        if (tile instanceof TileEntityFactory) {
            RecipeType recipeType = ((ItemBlockFactory) stack.getItem()).getRecipeTypeOrNull(stack);
            if (recipeType != null) {
                ((TileEntityFactory) tile).setRecipeType(recipeType);
            }
            world.notifyNeighborsOfStateChange(pos, tile.getBlockType());
            Mekanism.packetHandler.sendUpdatePacket(tile);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void animateTick(BlockState state, World world, BlockPos pos, Random random) {
        TileEntityMekanism tileEntity = (TileEntityMekanism) world.getTileEntity(pos);
        if (MekanismUtils.isActive(world, pos) && ((IActiveState) tileEntity).renderUpdate() && MekanismConfig.client.machineEffects.get()) {
            float xRandom = (float) pos.getX() + 0.5F;
            float yRandom = (float) pos.getY() + 0.0F + random.nextFloat() * 6.0F / 16.0F;
            float zRandom = (float) pos.getZ() + 0.5F;
            float iRandom = 0.52F;
            float jRandom = random.nextFloat() * 0.6F - 0.3F;
            Direction side = tileEntity.getDirection();

            switch (side) {
                case WEST:
                    world.addParticle(ParticleTypes.SMOKE, xRandom - iRandom, yRandom, zRandom + jRandom, 0.0D, 0.0D, 0.0D);
                    world.addParticle(RedstoneParticleData.REDSTONE_DUST, xRandom - iRandom, yRandom, zRandom + jRandom, 0.0D, 0.0D, 0.0D);
                    break;
                case EAST:
                    world.addParticle(ParticleTypes.SMOKE, xRandom + iRandom, yRandom, zRandom + jRandom, 0.0D, 0.0D, 0.0D);
                    world.addParticle(RedstoneParticleData.REDSTONE_DUST, xRandom + iRandom, yRandom, zRandom + jRandom, 0.0D, 0.0D, 0.0D);
                    break;
                case NORTH:
                    world.addParticle(ParticleTypes.SMOKE, xRandom + jRandom, yRandom, zRandom - iRandom, 0.0D, 0.0D, 0.0D);
                    world.addParticle(RedstoneParticleData.REDSTONE_DUST, xRandom + jRandom, yRandom, zRandom - iRandom, 0.0D, 0.0D, 0.0D);
                    break;
                case SOUTH:
                    world.addParticle(ParticleTypes.SMOKE, xRandom + jRandom, yRandom, zRandom + iRandom, 0.0D, 0.0D, 0.0D);
                    world.addParticle(RedstoneParticleData.REDSTONE_DUST, xRandom + jRandom, yRandom, zRandom + iRandom, 0.0D, 0.0D, 0.0D);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public int getLightValue(BlockState state, IEnviromentBlockReader world, BlockPos pos) {
        if (MekanismConfig.client.enableAmbientLighting.get()) {
            TileEntity tileEntity = MekanismUtils.getTileEntitySafe(world, pos);
            if (tileEntity instanceof IActiveState && ((IActiveState) tileEntity).lightUpdate() && ((IActiveState) tileEntity).wasActiveRecently()) {
                return MekanismConfig.client.ambientLightingLevel.get();
            }
        }
        return 0;
    }

    @Override
    public boolean onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        if (world.isRemote) {
            return true;
        }
        TileEntityMekanism tileEntity = (TileEntityMekanism) world.getTileEntity(pos);
        if (tileEntity == null) {
            return false;
        }
        if (tileEntity.tryWrench(state, player, hand, hit) != WrenchResult.PASS) {
            return true;
        }
        return tileEntity.openGui(player);
    }

    @OnlyIn(Dist.CLIENT)
    @Nonnull
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    @Deprecated
    public float getPlayerRelativeBlockHardness(BlockState state, @Nonnull PlayerEntity player, @Nonnull IBlockReader world, @Nonnull BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        return SecurityUtils.canAccess(player, tile) ? super.getPlayerRelativeBlockHardness(state, player, world, pos) : 0.0F;
    }

    @Override
    public float getExplosionResistance(BlockState state, IWorldReader world, BlockPos pos, @Nullable Entity exploder, Explosion explosion) {
        //TODO: This is how it was before, but should it be divided by 5 like in Block.java
        return blockResistance;
    }

    @Override
    @Deprecated
    public void neighborChanged(BlockState state, World world, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean isMoving) {
        if (!world.isRemote) {
            TileEntity tileEntity = world.getTileEntity(pos);
            if (tileEntity instanceof TileEntityMekanism) {
                ((TileEntityMekanism) tileEntity).onNeighborChange(neighborBlock);
            }
        }
    }

    @Nonnull
    @Override
    public SoundEvent getSoundEvent() {
        switch (type) {
            case ENRICHING:
                return BlockEnrichmentChamber.SOUND_EVENT;
            case CRUSHING:
                return BlockCrusher.SOUND_EVENT;
            case COMPRESSING:
                return BlockOsmiumCompressor.SOUND_EVENT;
            case COMBINING:
                return BlockCombiner.SOUND_EVENT;
            case PURIFYING:
                return BlockPurificationChamber.SOUND_EVENT;
            case INJECTING:
                return BlockChemicalInjectionChamber.SOUND_EVENT;
            case INFUSING:
                return BlockMetallurgicInfuser.SOUND_EVENT;
            case SAWING:
                return BlockPrecisionSawmill.SOUND_EVENT;
            case SMELTING:
            default:
                return BlockEnergizedSmelter.SOUND_EVENT;
        }
    }

    @Override
    public INamedContainerProvider getProvider(TileEntityFactory tile) {
        return new ContainerProvider("mekanism.container.factory", (i, inv, player) -> new FactoryContainer(i, inv, tile));
    }

    @Override
    public double getStorage() {
        //TODO: Fix this
        return tier.processes * 1000;// * Math.max(0.5D * type.getEnergyStorage(), type.getEnergyUsage());
    }

    @Override
    public double getUsage() {
        //TODO: Fix this
        return 10;//type.getEnergyUsage();
    }

    @Override
    public TileEntityType<TileEntityFactory> getTileType() {
        switch (type) {
            case CRUSHING:
                switch (tier) {
                    case ADVANCED:
                        return MekanismTileEntityTypes.ADVANCED_CRUSHING_FACTORY;
                    case ELITE:
                        return MekanismTileEntityTypes.ELITE_CRUSHING_FACTORY;
                    case ULTIMATE:
                        return MekanismTileEntityTypes.ULTIMATE_CRUSHING_FACTORY;
                    case BASIC:
                    default:
                        return MekanismTileEntityTypes.BASIC_CRUSHING_FACTORY;
                }
            case COMBINING:
                switch (tier) {
                    case ADVANCED:
                        return MekanismTileEntityTypes.ADVANCED_COMBINING_FACTORY;
                    case ELITE:
                        return MekanismTileEntityTypes.ELITE_COMBINING_FACTORY;
                    case ULTIMATE:
                        return MekanismTileEntityTypes.ULTIMATE_COMBINING_FACTORY;
                    case BASIC:
                    default:
                        return MekanismTileEntityTypes.BASIC_COMBINING_FACTORY;
                }
            case COMPRESSING:
                switch (tier) {
                    case ADVANCED:
                        return MekanismTileEntityTypes.ADVANCED_COMPRESSING_FACTORY;
                    case ELITE:
                        return MekanismTileEntityTypes.ELITE_COMPRESSING_FACTORY;
                    case ULTIMATE:
                        return MekanismTileEntityTypes.ULTIMATE_COMPRESSING_FACTORY;
                    case BASIC:
                    default:
                        return MekanismTileEntityTypes.BASIC_COMPRESSING_FACTORY;
                }
            case ENRICHING:
                switch (tier) {
                    case ADVANCED:
                        return MekanismTileEntityTypes.ADVANCED_ENRICHING_FACTORY;
                    case ELITE:
                        return MekanismTileEntityTypes.ELITE_ENRICHING_FACTORY;
                    case ULTIMATE:
                        return MekanismTileEntityTypes.ULTIMATE_ENRICHING_FACTORY;
                    case BASIC:
                    default:
                        return MekanismTileEntityTypes.BASIC_ENRICHING_FACTORY;
                }
            case INFUSING:
                switch (tier) {
                    case ADVANCED:
                        return MekanismTileEntityTypes.ADVANCED_INFUSING_FACTORY;
                    case ELITE:
                        return MekanismTileEntityTypes.ELITE_INFUSING_FACTORY;
                    case ULTIMATE:
                        return MekanismTileEntityTypes.ULTIMATE_INFUSING_FACTORY;
                    case BASIC:
                    default:
                        return MekanismTileEntityTypes.BASIC_INFUSING_FACTORY;
                }
            case INJECTING:
                switch (tier) {
                    case ADVANCED:
                        return MekanismTileEntityTypes.ADVANCED_INJECTING_FACTORY;
                    case ELITE:
                        return MekanismTileEntityTypes.ELITE_INJECTING_FACTORY;
                    case ULTIMATE:
                        return MekanismTileEntityTypes.ULTIMATE_INJECTING_FACTORY;
                    case BASIC:
                    default:
                        return MekanismTileEntityTypes.BASIC_INJECTING_FACTORY;
                }
            case PURIFYING:
                switch (tier) {
                    case ADVANCED:
                        return MekanismTileEntityTypes.ADVANCED_PURIFYING_FACTORY;
                    case ELITE:
                        return MekanismTileEntityTypes.ELITE_PURIFYING_FACTORY;
                    case ULTIMATE:
                        return MekanismTileEntityTypes.ULTIMATE_PURIFYING_FACTORY;
                    case BASIC:
                    default:
                        return MekanismTileEntityTypes.BASIC_PURIFYING_FACTORY;
                }
            case SAWING:
                switch (tier) {
                    case ADVANCED:
                        return MekanismTileEntityTypes.ADVANCED_SAWING_FACTORY;
                    case ELITE:
                        return MekanismTileEntityTypes.ELITE_SAWING_FACTORY;
                    case ULTIMATE:
                        return MekanismTileEntityTypes.ULTIMATE_SAWING_FACTORY;
                    case BASIC:
                    default:
                        return MekanismTileEntityTypes.BASIC_SAWING_FACTORY;
                }
            case SMELTING:
            default:
                switch (tier) {
                    case ADVANCED:
                        return MekanismTileEntityTypes.ADVANCED_SMELTING_FACTORY;
                    case ELITE:
                        return MekanismTileEntityTypes.ELITE_SMELTING_FACTORY;
                    case ULTIMATE:
                        return MekanismTileEntityTypes.ULTIMATE_SMELTING_FACTORY;
                    case BASIC:
                    default:
                        return MekanismTileEntityTypes.BASIC_SMELTING_FACTORY;
                }
        }
    }

    @Nonnull
    @Override
    public Set<Upgrade> getSupportedUpgrade() {
        return EnumSet.of(Upgrade.SPEED, Upgrade.ENERGY, Upgrade.MUFFLING);
    }
}