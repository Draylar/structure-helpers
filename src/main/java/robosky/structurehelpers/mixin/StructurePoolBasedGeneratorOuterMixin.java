package robosky.structurehelpers.mixin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.structure.StructurePiecesHolder;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import robosky.structurehelpers.iface.StructurePoolGeneratorAddition;
import robosky.structurehelpers.structure.pool.ElementRange;
import robosky.structurehelpers.structure.pool.ExtendedStructurePoolFeatureConfig;

import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructureManager;
import net.minecraft.structure.pool.StructurePoolBasedGenerator;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.StructurePoolFeatureConfig;

/**
 * Note: because this class is accessed from multiple threads, there is no mutable
 * static state added to this class. Instead, the state is passed into this class
 * via the {@code List} method parameter.
 */
@Mixin(StructurePoolBasedGenerator.class)
abstract class StructurePoolBasedGeneratorOuterMixin {

    @Unique
    private static final ThreadLocal<ExtendedStructurePoolFeatureConfig> featureConfig = new ThreadLocal<>();

    @Unique
    private static final ThreadLocal<StructurePoolGeneratorAddition> poolGenerator = new ThreadLocal<>();

    /**
     * Extract element placement ranges from the child element list
     * out parameter.
     */
    @Inject(method = "method_30419", at = @At("HEAD"))
    private static void extractRoomMinMax(
        DynamicRegistryManager registryManager,
        StructurePoolFeatureConfig config,
        StructurePoolBasedGenerator.PieceFactory pieceFactory,
        ChunkGenerator generator,
        StructureManager manager,
        BlockPos pos,
        StructurePiecesHolder holder,
        Random rand,
        boolean b,
        boolean generateAtSurface,
        HeightLimitView view,
        CallbackInfo info
    ) {
        if(config instanceof ExtendedStructurePoolFeatureConfig) {
            featureConfig.set((ExtendedStructurePoolFeatureConfig)config);
        }
    }

    @SuppressWarnings("ConstantConditions")
    @ModifyVariable(
        method = "method_30419",
        at = @At(
            value = "STORE",
            ordinal = 0
        )
    )
    private static StructurePoolBasedGenerator.StructurePoolGenerator setElementMinMaxForStructure(
        StructurePoolBasedGenerator.StructurePoolGenerator gen
    ) {
        ExtendedStructurePoolFeatureConfig config = featureConfig.get();
        if(config != null) {
            Map<Identifier, ElementRange> ranges = new HashMap<>();
            for(ElementRange range : config.getRangeConstraints()) {
                ranges.put(range.id, range);
            }
            StructurePoolGeneratorAddition gen1 = (StructurePoolGeneratorAddition)(Object)gen;
            gen1.structhelp_setRoomMinMax(ranges);
            featureConfig.remove();
            poolGenerator.set(gen1);
        }
        return gen;
    }

    /**
     * Generate child elements. Child element generation does not necessarily
     * respect total structure piece count nor placement limits.
     */
    @Inject(method = "method_30419", at = @At("RETURN"), locals = LocalCapture.CAPTURE_FAILHARD)
    private static void generateChildren(
        DynamicRegistryManager registryManager,
        StructurePoolFeatureConfig config,
        StructurePoolBasedGenerator.PieceFactory pieceFactory,
        ChunkGenerator generator,
        StructureManager manager,
        BlockPos pos,
        StructurePiecesHolder holder,
        Random rand,
        boolean b,
        boolean generateAtSurface,
        HeightLimitView view,
        CallbackInfo info,
        List<? super PoolStructurePiece> children
    ) {
        StructurePoolGeneratorAddition gen = poolGenerator.get();
        if(gen != null) {
            poolGenerator.remove();
            gen.structhelp_setGeneratingChildren();
            for(Object piece : new ArrayList<>(children)) {
                if(piece instanceof PoolStructurePiece) {
                    PoolStructurePiece poolPiece = (PoolStructurePiece)piece;
                    BlockBox blockBox = poolPiece.getBoundingBox();
                    int x = (blockBox.getMaxX() + blockBox.getMinX()) / 2;
                    int z = (blockBox.getMaxX() + blockBox.getMinZ()) / 2;
                    int y = generator.getHeightOnGround(x, z, Heightmap.Type.WORLD_SURFACE_WG, view);
                    ((StructurePoolGeneratorAccessor)gen).callGeneratePiece(poolPiece,
                        new MutableObject<>(VoxelShapes.empty()),
                        y + 80,
                        0,
                        b,
                        view);
                }
            }
            if(!gen.structhelp_softCheckMinMaxConstraints()) {
                LogManager.getLogger(StructurePoolBasedGenerator.class)
                    .info("StructHelp - failed to satisfy range constraints");
            }
        }
    }

    @Redirect(
        method = "method_30419",
        at = @At(
            value = "NEW",
            target = "net/minecraft/util/math/Box"
        )
    )
    private static Box expandMaxStructureBounds(
        double minX,
        double minY,
        double minZ,
        double maxX,
        double maxY,
        double maxZ,
        DynamicRegistryManager registryManager,
        StructurePoolFeatureConfig config,
        StructurePoolBasedGenerator.PieceFactory pieceFactory,
        ChunkGenerator generator,
        StructureManager manager,
        BlockPos pos,
        StructurePiecesHolder holder,
        Random rand,
        boolean b,
        boolean generateAtSurface,
        HeightLimitView view
    ) {
        final int vanillaExtent = 80;
        if(config instanceof ExtendedStructurePoolFeatureConfig) {
            ExtendedStructurePoolFeatureConfig data = (ExtendedStructurePoolFeatureConfig)config;
            int extentH = data.getHorizontalExtent() - vanillaExtent;
            int extentV = data.getVerticalExtent() - vanillaExtent;
            if(extentH <= -vanillaExtent) {
                extentH = 0;
            }
            if(extentV <= -vanillaExtent) {
                extentV = 0;
            }
            return new Box(minX - extentH,
                minY - extentV,
                minZ - extentH,
                maxX + extentH,
                maxY + extentV,
                maxZ + extentH);
        }
        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
