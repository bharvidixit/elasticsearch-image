package org.elasticsearch.index.mapper.image;


import net.semanticmetadata.lire.imageanalysis.features.GlobalFeature;
import net.semanticmetadata.lire.imageanalysis.features.LireFeature;
import net.semanticmetadata.lire.imageanalysis.features.global.*;
import net.semanticmetadata.lire.imageanalysis.features.global.joint.*;
import net.semanticmetadata.lire.imageanalysis.features.global.centrist.*;
import net.semanticmetadata.lire.imageanalysis.features.global.spatialpyramid.*;

/**
 * Global Features supported by LIRE
 * Subclass of {@link LireFeature}
 */
public enum GlobalFeatureEnum {
    //centrist
    SIMPLE_CENTRIST(SimpleCentrist.class),
    SPATIAL_PYRAMID_CENTRIST(SpatialPyramidCentrist.class),

    //joint
    JOINT_HISTOGRAM(JointHistogram.class),
    LOCAL_BINARY_PATTERNS_AND_OPPONENT(LocalBinaryPatternsAndOpponent.class),
    RANK_AND_OPPONENT(RankAndOpponent.class),

    //spatialpyramid
    SPACC(SPACC.class),
    SPCEDD(SPCEDD.class),
    SPFCTH(SPFCTH.class),
    SPJCD(SPJCD.class),
    SPLBP(SPLBP.class),

    AUTO_COLOR_CORRELOGRAM(AutoColorCorrelogram.class),
    BINARY_PATTERNS_PYRAMID(BinaryPatternsPyramid.class),
    CEDD(CEDD.class),
    COLOR_LAYOUT(ColorLayout.class),
    EDGE_HISTOGRAM(EdgeHistogram.class),
    FCTH(FCTH.class),
    FUZZY_COLOR_HISTOGRAM(FuzzyColorHistogram.class),
    FUZZY_OPPONENT_HISTOGRAM(FuzzyOpponentHistogram.class),
    GABOR(Gabor.class),
    JCD(JCD.class),
    JPEG_COEFFICIENT_HISTOGRAM(JpegCoefficientHistogram.class),
    LOCAL_BINARY_PATTERNS(LocalBinaryPatterns.class),
    LUMINANCE_LAYOUT(LuminanceLayout.class),
    OPPONENT_HISTOGRAM(OpponentHistogram.class),
    PHOG(PHOG.class),
    ROTATION_INVARIANT_LOCAL_BINARY_PATTERNS(RotationInvariantLocalBinaryPatterns.class),
    SCALABLE_COLOR(ScalableColor.class),
    SIMPLE_COLOR_HISTOGRAM(SimpleColorHistogram.class),
    TAMURA(Tamura.class),
    ;

    private Class<? extends GlobalFeature> featureClass;

    GlobalFeatureEnum(Class<? extends GlobalFeature> globalfeatureClass) {
        this.featureClass = globalfeatureClass;
    }

    public Class<? extends GlobalFeature> getGlobalFeatureClass() {
        return featureClass;
    }

    public static GlobalFeatureEnum getByName(String name) {
        return valueOf(name.toUpperCase());
    }

}
