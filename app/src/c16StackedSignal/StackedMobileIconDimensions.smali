.class public final Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;
.super Ljava/lang/Object;
.source "go/retraceme 1473044a3194ebcf2325f73aadcc13bd0818eff92cf8946e58bc216c2171f01f"

# static fields
.field public static final BarBaseHeightFiveBarsSp:J

.field public static final BarBaseHeightFourBarsSp:J

.field public static final BarsLevelIncrementSp:J

.field public static final BarsVerticalPaddingSp:J

.field public static final ExclamationCutoutRadiusSp:J

.field public static final ExclamationDiameterSp:J

.field public static final ExclamationHeightSp:J

.field public static final ExclamationHorizontalOffset:J

.field public static final ExclamationVerticalSpacing:J

.field public static final HorizontalPaddingFiveBarsSp:J

.field public static final HorizontalPaddingFourBarsSp:J

.field public static final INSTANCE:Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;

.field public static final IconHeightSp:J

.field public static final IconPaddingSp:J

.field public static final IconSpacingSp:J

.field public static final IconWidthFiveBarsSp:J

.field public static final IconWidthFourBarsSp:J

.field public static final SecondaryBarHeightSp:J

# direct methods
.method static constructor <clinit>()V
    .registers 8

    new-instance v0, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;

    invoke-direct {v0}, Ljava/lang/Object;-><init>()V

    sput-object v0, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->INSTANCE:Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;

    const/16 v0, 0xc

    invoke-static {v0}, Landroidx/compose/ui/unit/TextUnitKt;->getSp(I)J

    move-result-wide v0

    sput-wide v0, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->IconHeightSp:J

    const/4 v0, 0x4

    invoke-static {v0}, Landroidx/compose/ui/unit/TextUnitKt;->getSp(I)J

    move-result-wide v0

    sput-wide v0, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->IconPaddingSp:J

    const/4 v0, 0x2

    invoke-static {v0}, Landroidx/compose/ui/unit/TextUnitKt;->getSp(I)J

    move-result-wide v1

    sput-wide v1, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->IconSpacingSp:J

    const-wide/high16 v1, 0x3ff8000000000000L    # 1.5

    invoke-static {v1, v2}, Landroidx/compose/ui/unit/TextUnitKt;->getSp(D)J

    move-result-wide v3

    sput-wide v3, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->BarsVerticalPaddingSp:J

    const/4 v3, 0x1

    invoke-static {v3}, Landroidx/compose/ui/unit/TextUnitKt;->getSp(I)J

    move-result-wide v4

    sput-wide v4, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->BarsLevelIncrementSp:J

    const/4 v4, 0x3

    invoke-static {v4}, Landroidx/compose/ui/unit/TextUnitKt;->getSp(I)J

    move-result-wide v4

    sput-wide v4, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->SecondaryBarHeightSp:J

    const/4 v4, 0x5

    invoke-static {v4}, Landroidx/compose/ui/unit/TextUnitKt;->getSp(I)J

    move-result-wide v4

    sput-wide v4, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->ExclamationCutoutRadiusSp:J

    invoke-static {v1, v2}, Landroidx/compose/ui/unit/TextUnitKt;->getSp(D)J

    move-result-wide v4

    sput-wide v4, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->ExclamationDiameterSp:J

    const-wide/high16 v4, 0x4012000000000000L    # 4.5

    invoke-static {v4, v5}, Landroidx/compose/ui/unit/TextUnitKt;->getSp(D)J

    move-result-wide v6

    sput-wide v6, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->ExclamationHeightSp:J

    invoke-static {v3}, Landroidx/compose/ui/unit/TextUnitKt;->getSp(I)J

    move-result-wide v6

    sput-wide v6, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->ExclamationVerticalSpacing:J

    invoke-static {v3}, Landroidx/compose/ui/unit/TextUnitKt;->getSp(I)J

    move-result-wide v6

    sput-wide v6, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->ExclamationHorizontalOffset:J

    const-wide v6, 0x4032800000000000L    # 18.5

    invoke-static {v6, v7}, Landroidx/compose/ui/unit/TextUnitKt;->getSp(D)J

    move-result-wide v6

    sput-wide v6, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->IconWidthFiveBarsSp:J

    const/16 v3, 0x10

    invoke-static {v3}, Landroidx/compose/ui/unit/TextUnitKt;->getSp(I)J

    move-result-wide v6

    sput-wide v6, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->IconWidthFourBarsSp:J

    invoke-static {v1, v2}, Landroidx/compose/ui/unit/TextUnitKt;->getSp(D)J

    move-result-wide v1

    sput-wide v1, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->HorizontalPaddingFiveBarsSp:J

    invoke-static {v0}, Landroidx/compose/ui/unit/TextUnitKt;->getSp(I)J

    move-result-wide v0

    sput-wide v0, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->HorizontalPaddingFourBarsSp:J

    const-wide/high16 v0, 0x400c000000000000L    # 3.5

    invoke-static {v0, v1}, Landroidx/compose/ui/unit/TextUnitKt;->getSp(D)J

    move-result-wide v0

    sput-wide v0, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->BarBaseHeightFiveBarsSp:J

    invoke-static {v4, v5}, Landroidx/compose/ui/unit/TextUnitKt;->getSp(D)J

    move-result-wide v0

    sput-wide v0, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->BarBaseHeightFourBarsSp:J

    return-void
.end method

# virtual methods
.method public final getBarBaseHeightFiveBarsSp-XSAIIZE()J
    .registers 3

    sget-wide v0, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->BarBaseHeightFiveBarsSp:J

    return-wide v0
.end method

.method public final getBarBaseHeightFourBarsSp-XSAIIZE()J
    .registers 3

    sget-wide v0, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->BarBaseHeightFourBarsSp:J

    return-wide v0
.end method

.method public final getBarsLevelIncrementSp-XSAIIZE()J
    .registers 3

    sget-wide v0, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->BarsLevelIncrementSp:J

    return-wide v0
.end method

.method public final getBarsVerticalPaddingSp-XSAIIZE()J
    .registers 3

    sget-wide v0, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->BarsVerticalPaddingSp:J

    return-wide v0
.end method

.method public final getExclamationCutoutRadiusSp-XSAIIZE()J
    .registers 3

    sget-wide v0, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->ExclamationCutoutRadiusSp:J

    return-wide v0
.end method

.method public final getExclamationDiameterSp-XSAIIZE()J
    .registers 3

    sget-wide v0, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->ExclamationDiameterSp:J

    return-wide v0
.end method

.method public final getExclamationHeightSp-XSAIIZE()J
    .registers 3

    sget-wide v0, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->ExclamationHeightSp:J

    return-wide v0
.end method

.method public final getExclamationHorizontalOffset-XSAIIZE()J
    .registers 3

    sget-wide v0, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->ExclamationHorizontalOffset:J

    return-wide v0
.end method

.method public final getExclamationVerticalSpacing-XSAIIZE()J
    .registers 3

    sget-wide v0, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->ExclamationVerticalSpacing:J

    return-wide v0
.end method

.method public final getHorizontalPaddingFiveBarsSp-XSAIIZE()J
    .registers 3

    sget-wide v0, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->HorizontalPaddingFiveBarsSp:J

    return-wide v0
.end method

.method public final getHorizontalPaddingFourBarsSp-XSAIIZE()J
    .registers 3

    sget-wide v0, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->HorizontalPaddingFourBarsSp:J

    return-wide v0
.end method

.method public final getIconHeightSp-XSAIIZE()J
    .registers 3

    sget-wide v0, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->IconHeightSp:J

    return-wide v0
.end method

.method public final getIconPaddingSp-XSAIIZE()J
    .registers 3

    sget-wide v0, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->IconPaddingSp:J

    return-wide v0
.end method

.method public final getIconSpacingSp-XSAIIZE()J
    .registers 3

    sget-wide v0, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->IconSpacingSp:J

    return-wide v0
.end method

.method public final getIconWidthFiveBarsSp-XSAIIZE()J
    .registers 3

    sget-wide v0, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->IconWidthFiveBarsSp:J

    return-wide v0
.end method

.method public final getIconWidthFourBarsSp-XSAIIZE()J
    .registers 3

    sget-wide v0, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->IconWidthFourBarsSp:J

    return-wide v0
.end method

.method public final getSecondaryBarHeightSp-XSAIIZE()J
    .registers 3

    sget-wide v0, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->SecondaryBarHeightSp:J

    return-wide v0
.end method
