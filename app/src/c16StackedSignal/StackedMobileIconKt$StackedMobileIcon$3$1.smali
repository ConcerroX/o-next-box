.class final Lcom/mi/onextbox/port/c16/StackedMobileIconKt$StackedMobileIcon$3$1;
.super Lkotlin/jvm/internal/Lambda;
.source "go/retraceme 1473044a3194ebcf2325f73aadcc13bd0818eff92cf8946e58bc216c2171f01f"

# interfaces
.implements Lkotlin/jvm/functions/Function1;

# annotations
.annotation system Ldalvik/annotation/Signature;
    value = {
        "Lkotlin/jvm/internal/Lambda;",
        "Lkotlin/jvm/functions/Function1;"
    }
.end annotation

# instance fields
.field final synthetic $color:J

.field final synthetic $dimensions:Lcom/mi/onextbox/port/c16/BarsDependentDimensions;

.field final synthetic $numberOfBars:I

.field final synthetic $viewModel:Lcom/android/systemui/statusbar/pipeline/mobile/ui/model/DualSim;

# direct methods
.method public constructor <init>(Lcom/mi/onextbox/port/c16/BarsDependentDimensions;ILcom/android/systemui/statusbar/pipeline/mobile/ui/model/DualSim;J)V
    .registers 6

    iput-object p1, p0, Lcom/mi/onextbox/port/c16/StackedMobileIconKt$StackedMobileIcon$3$1;->$dimensions:Lcom/mi/onextbox/port/c16/BarsDependentDimensions;

    iput p2, p0, Lcom/mi/onextbox/port/c16/StackedMobileIconKt$StackedMobileIcon$3$1;->$numberOfBars:I

    iput-object p3, p0, Lcom/mi/onextbox/port/c16/StackedMobileIconKt$StackedMobileIcon$3$1;->$viewModel:Lcom/android/systemui/statusbar/pipeline/mobile/ui/model/DualSim;

    iput-wide p4, p0, Lcom/mi/onextbox/port/c16/StackedMobileIconKt$StackedMobileIcon$3$1;->$color:J

    const/4 p1, 0x1

    invoke-direct {p0, p1}, Lkotlin/jvm/internal/Lambda;-><init>(I)V

    return-void
.end method

# virtual methods
.method public final invoke(Ljava/lang/Object;)Ljava/lang/Object;
    .registers 26

    move-object/from16 v0, p0

    move-object/from16 v1, p1

    check-cast v1, Landroidx/compose/ui/graphics/drawscope/DrawScope;

    sget-object v2, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->INSTANCE:Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;

    invoke-virtual {v2}, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->getBarsVerticalPaddingSp-XSAIIZE()J

    move-result-wide v3

    invoke-interface {v1, v3, v4}, Landroidx/compose/ui/unit/Density;->roundToPx--R2X_6o(J)I

    move-result v10

    iget-object v3, v0, Lcom/mi/onextbox/port/c16/StackedMobileIconKt$StackedMobileIcon$3$1;->$dimensions:Lcom/mi/onextbox/port/c16/BarsDependentDimensions;

    invoke-virtual {v3}, Lcom/mi/onextbox/port/c16/BarsDependentDimensions;->getBarsHorizontalPadding-XSAIIZE()J

    move-result-wide v3

    invoke-interface {v1, v3, v4}, Landroidx/compose/ui/unit/Density;->roundToPx--R2X_6o(J)I

    move-result v11

    iget v3, v0, Lcom/mi/onextbox/port/c16/StackedMobileIconKt$StackedMobileIcon$3$1;->$numberOfBars:I

    const/4 v4, 0x1

    sub-int/2addr v3, v4

    mul-int/2addr v3, v11

    invoke-interface {v1}, Landroidx/compose/ui/graphics/drawscope/DrawScope;->getSize-NH-jbRc()J

    move-result-wide v5

    const/16 v12, 0x20

    shr-long/2addr v5, v12

    long-to-int v5, v5

    invoke-static {v5}, Ljava/lang/Float;->intBitsToFloat(I)F

    move-result v5

    int-to-float v3, v3

    sub-float/2addr v5, v3

    iget v3, v0, Lcom/mi/onextbox/port/c16/StackedMobileIconKt$StackedMobileIcon$3$1;->$numberOfBars:I

    int-to-float v3, v3

    div-float v13, v5, v3

    invoke-virtual {v2}, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->getSecondaryBarHeightSp-XSAIIZE()J

    move-result-wide v2

    invoke-interface {v1, v2, v3}, Landroidx/compose/ui/unit/Density;->toPx--R2X_6o(J)F

    move-result v14

    iget-object v2, v0, Lcom/mi/onextbox/port/c16/StackedMobileIconKt$StackedMobileIcon$3$1;->$dimensions:Lcom/mi/onextbox/port/c16/BarsDependentDimensions;

    invoke-virtual {v2}, Lcom/mi/onextbox/port/c16/BarsDependentDimensions;->getBarBaseHeight-XSAIIZE()J

    move-result-wide v2

    invoke-interface {v1, v2, v3}, Landroidx/compose/ui/unit/Density;->toPx--R2X_6o(J)F

    move-result v15

    iget v2, v0, Lcom/mi/onextbox/port/c16/StackedMobileIconKt$StackedMobileIcon$3$1;->$numberOfBars:I

    const-wide v16, 0xffffffffL

    if-gt v4, v2, :cond_fe

    const/4 v2, 0x0

    move/from16 v18, v2

    move v3, v4

    :goto_51
    invoke-interface {v1}, Landroidx/compose/ui/graphics/drawscope/DrawScope;->getSize-NH-jbRc()J

    move-result-wide v4

    and-long v4, v4, v16

    long-to-int v2, v4

    invoke-static {v2}, Ljava/lang/Float;->intBitsToFloat(I)F

    move-result v2

    sub-float v19, v2, v14

    iget-object v2, v0, Lcom/mi/onextbox/port/c16/StackedMobileIconKt$StackedMobileIcon$3$1;->$viewModel:Lcom/android/systemui/statusbar/pipeline/mobile/ui/model/DualSim;

    invoke-virtual {v2}, Lcom/android/systemui/statusbar/pipeline/mobile/ui/model/DualSim;->getSecondary()Lcom/android/systemui/statusbar/pipeline/mobile/domain/model/SignalIconModel$CellularTypeIconModel$Cellular;

    move-result-object v2

    invoke-virtual {v2}, Lcom/android/systemui/statusbar/pipeline/mobile/domain/model/SignalIconModel$CellularTypeIconModel$Cellular;->getNumberOfLevels()I

    move-result v2

    if-gt v3, v2, :cond_95

    iget-object v2, v0, Lcom/mi/onextbox/port/c16/StackedMobileIconKt$StackedMobileIcon$3$1;->$viewModel:Lcom/android/systemui/statusbar/pipeline/mobile/ui/model/DualSim;

    invoke-virtual {v2}, Lcom/android/systemui/statusbar/pipeline/mobile/ui/model/DualSim;->getSecondary()Lcom/android/systemui/statusbar/pipeline/mobile/domain/model/SignalIconModel$CellularTypeIconModel$Cellular;

    move-result-object v2

    invoke-virtual {v2}, Lcom/android/systemui/statusbar/pipeline/mobile/domain/model/SignalIconModel$CellularTypeIconModel$Cellular;->getLevel()I

    move-result v2

    invoke-static/range {v18 .. v18}, Ljava/lang/Float;->floatToRawIntBits(F)I

    move-result v4

    int-to-long v4, v4

    invoke-static/range {v19 .. v19}, Ljava/lang/Float;->floatToRawIntBits(F)I

    move-result v6

    int-to-long v6, v6

    shl-long/2addr v4, v12

    and-long v6, v6, v16

    or-long/2addr v4, v6

    invoke-static {v13}, Ljava/lang/Float;->floatToRawIntBits(F)I

    move-result v6

    int-to-long v6, v6

    invoke-static {v14}, Ljava/lang/Float;->floatToRawIntBits(F)I

    move-result v8

    int-to-long v8, v8

    shl-long/2addr v6, v12

    and-long v8, v8, v16

    or-long/2addr v6, v8

    iget-wide v8, v0, Lcom/mi/onextbox/port/c16/StackedMobileIconKt$StackedMobileIcon$3$1;->$color:J

    invoke-static/range {v1 .. v9}, Lcom/mi/onextbox/port/c16/StackedMobileIconKt;->drawMobileIconBar-n3YQ8UE$default(Landroidx/compose/ui/graphics/drawscope/DrawScope;IIJJJ)V

    :cond_95
    iget-object v2, v0, Lcom/mi/onextbox/port/c16/StackedMobileIconKt$StackedMobileIcon$3$1;->$viewModel:Lcom/android/systemui/statusbar/pipeline/mobile/ui/model/DualSim;

    invoke-virtual {v2}, Lcom/android/systemui/statusbar/pipeline/mobile/ui/model/DualSim;->getPrimary()Lcom/android/systemui/statusbar/pipeline/mobile/domain/model/SignalIconModel$CellularTypeIconModel$Cellular;

    move-result-object v2

    invoke-virtual {v2}, Lcom/android/systemui/statusbar/pipeline/mobile/domain/model/SignalIconModel$CellularTypeIconModel$Cellular;->getNumberOfLevels()I

    move-result v2

    if-gt v3, v2, :cond_e9

    sget-object v2, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->INSTANCE:Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;

    invoke-virtual {v2}, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->getBarsLevelIncrementSp-XSAIIZE()J

    move-result-wide v4

    invoke-interface {v1, v4, v5}, Landroidx/compose/ui/unit/Density;->toPx--R2X_6o(J)F

    move-result v2

    add-int/lit8 v4, v3, -0x1

    int-to-float v4, v4

    mul-float/2addr v2, v4

    add-float/2addr v2, v15

    int-to-float v4, v10

    sub-float v19, v19, v4

    sub-float v19, v19, v2

    iget-object v4, v0, Lcom/mi/onextbox/port/c16/StackedMobileIconKt$StackedMobileIcon$3$1;->$viewModel:Lcom/android/systemui/statusbar/pipeline/mobile/ui/model/DualSim;

    invoke-virtual {v4}, Lcom/android/systemui/statusbar/pipeline/mobile/ui/model/DualSim;->getPrimary()Lcom/android/systemui/statusbar/pipeline/mobile/domain/model/SignalIconModel$CellularTypeIconModel$Cellular;

    move-result-object v4

    invoke-virtual {v4}, Lcom/android/systemui/statusbar/pipeline/mobile/domain/model/SignalIconModel$CellularTypeIconModel$Cellular;->getLevel()I

    move-result v4

    invoke-static/range {v18 .. v18}, Ljava/lang/Float;->floatToRawIntBits(F)I

    move-result v5

    int-to-long v5, v5

    invoke-static/range {v19 .. v19}, Ljava/lang/Float;->floatToRawIntBits(F)I

    move-result v7

    int-to-long v7, v7

    shl-long/2addr v5, v12

    and-long v7, v7, v16

    or-long/2addr v5, v7

    invoke-static {v13}, Ljava/lang/Float;->floatToRawIntBits(F)I

    move-result v7

    int-to-long v7, v7

    invoke-static {v2}, Ljava/lang/Float;->floatToRawIntBits(F)I

    move-result v2

    move/from16 p1, v12

    move/from16 v19, v13

    int-to-long v12, v2

    shl-long v7, v7, p1

    and-long v12, v12, v16

    or-long/2addr v7, v12

    move v2, v4

    move-wide v4, v5

    move-wide v6, v7

    iget-wide v8, v0, Lcom/mi/onextbox/port/c16/StackedMobileIconKt$StackedMobileIcon$3$1;->$color:J

    invoke-static/range {v1 .. v9}, Lcom/mi/onextbox/port/c16/StackedMobileIconKt;->drawMobileIconBar-n3YQ8UE$default(Landroidx/compose/ui/graphics/drawscope/DrawScope;IIJJJ)V

    goto :goto_ed

    :cond_e9
    move/from16 p1, v12

    move/from16 v19, v13

    :goto_ed
    int-to-float v2, v11

    add-float v13, v19, v2

    add-float v18, v13, v18

    iget v2, v0, Lcom/mi/onextbox/port/c16/StackedMobileIconKt$StackedMobileIcon$3$1;->$numberOfBars:I

    if-eq v3, v2, :cond_100

    add-int/lit8 v3, v3, 0x1

    move/from16 v12, p1

    move/from16 v13, v19

    goto/16 :goto_51

    :cond_fe
    move/from16 p1, v12

    :cond_100
    iget-object v2, v0, Lcom/mi/onextbox/port/c16/StackedMobileIconKt$StackedMobileIcon$3$1;->$viewModel:Lcom/android/systemui/statusbar/pipeline/mobile/ui/model/DualSim;

    invoke-virtual {v2}, Lcom/android/systemui/statusbar/pipeline/mobile/ui/model/DualSim;->getPrimary()Lcom/android/systemui/statusbar/pipeline/mobile/domain/model/SignalIconModel$CellularTypeIconModel$Cellular;

    move-result-object v2

    invoke-virtual {v2}, Lcom/android/systemui/statusbar/pipeline/mobile/domain/model/SignalIconModel$CellularTypeIconModel$Cellular;->getShowExclamationMark()Z

    move-result v2

    if-eqz v2, :cond_20a

    iget-wide v8, v0, Lcom/mi/onextbox/port/c16/StackedMobileIconKt$StackedMobileIcon$3$1;->$color:J

    sget-object v10, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->INSTANCE:Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;

    invoke-virtual {v10}, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->getExclamationDiameterSp-XSAIIZE()J

    move-result-wide v2

    invoke-interface {v1, v2, v3}, Landroidx/compose/ui/unit/Density;->toPx--R2X_6o(J)F

    move-result v11

    invoke-virtual {v10}, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->getExclamationDiameterSp-XSAIIZE()J

    move-result-wide v2

    invoke-interface {v1, v2, v3}, Landroidx/compose/ui/unit/Density;->toPx--R2X_6o(J)F

    move-result v0

    const/4 v2, 0x2

    int-to-float v2, v2

    div-float v12, v0, v2

    invoke-virtual {v10}, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->getExclamationHeightSp-XSAIIZE()J

    move-result-wide v3

    invoke-interface {v1, v3, v4}, Landroidx/compose/ui/unit/Density;->toPx--R2X_6o(J)F

    move-result v0

    invoke-virtual {v10}, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->getExclamationVerticalSpacing-XSAIIZE()J

    move-result-wide v3

    invoke-interface {v1, v3, v4}, Landroidx/compose/ui/unit/Density;->toPx--R2X_6o(J)F

    move-result v3

    add-float/2addr v3, v0

    add-float/2addr v3, v11

    invoke-interface {v1}, Landroidx/compose/ui/graphics/drawscope/DrawScope;->getSize-NH-jbRc()J

    move-result-wide v4

    shr-long v4, v4, p1

    long-to-int v0, v4

    invoke-static {v0}, Ljava/lang/Float;->intBitsToFloat(I)F

    move-result v0

    invoke-virtual {v10}, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->getExclamationHorizontalOffset-XSAIIZE()J

    move-result-wide v4

    invoke-interface {v1, v4, v5}, Landroidx/compose/ui/unit/Density;->toPx--R2X_6o(J)F

    move-result v4

    sub-float/2addr v0, v4

    invoke-interface {v1}, Landroidx/compose/ui/graphics/drawscope/DrawScope;->getSize-NH-jbRc()J

    move-result-wide v4

    and-long v4, v4, v16

    long-to-int v4, v4

    invoke-static {v4}, Ljava/lang/Float;->intBitsToFloat(I)F

    move-result v4

    sub-float/2addr v4, v12

    invoke-static {v0}, Ljava/lang/Float;->floatToRawIntBits(F)I

    move-result v0

    int-to-long v5, v0

    invoke-static {v4}, Ljava/lang/Float;->floatToRawIntBits(F)I

    move-result v0

    int-to-long v13, v0

    shl-long v4, v5, p1

    and-long v6, v13, v16

    or-long v13, v4, v6

    shr-long v4, v13, p1

    long-to-int v0, v4

    invoke-static {v0}, Ljava/lang/Float;->intBitsToFloat(I)F

    move-result v4

    sub-float/2addr v4, v12

    invoke-interface {v1}, Landroidx/compose/ui/graphics/drawscope/DrawScope;->getSize-NH-jbRc()J

    move-result-wide v5

    and-long v5, v5, v16

    long-to-int v5, v5

    invoke-static {v5}, Ljava/lang/Float;->intBitsToFloat(I)F

    move-result v5

    sub-float/2addr v5, v3

    invoke-static {v4}, Ljava/lang/Float;->floatToRawIntBits(F)I

    move-result v4

    int-to-long v6, v4

    invoke-static {v5}, Ljava/lang/Float;->floatToRawIntBits(F)I

    move-result v4

    int-to-long v4, v4

    shl-long v6, v6, p1

    and-long v4, v4, v16

    or-long v18, v6, v4

    invoke-static {v12}, Ljava/lang/Float;->floatToRawIntBits(F)I

    move-result v4

    int-to-long v4, v4

    invoke-static {v12}, Ljava/lang/Float;->floatToRawIntBits(F)I

    move-result v6

    int-to-long v6, v6

    shl-long v4, v4, p1

    and-long v6, v6, v16

    or-long v20, v4, v6

    invoke-static {v0}, Ljava/lang/Float;->intBitsToFloat(I)F

    move-result v0

    invoke-interface {v1}, Landroidx/compose/ui/graphics/drawscope/DrawScope;->getSize-NH-jbRc()J

    move-result-wide v4

    and-long v4, v4, v16

    long-to-int v4, v4

    invoke-static {v4}, Ljava/lang/Float;->intBitsToFloat(I)F

    move-result v4

    div-float/2addr v3, v2

    sub-float/2addr v4, v3

    invoke-static {v0}, Ljava/lang/Float;->floatToRawIntBits(F)I

    move-result v0

    int-to-long v2, v0

    invoke-static {v4}, Ljava/lang/Float;->floatToRawIntBits(F)I

    move-result v0

    int-to-long v4, v0

    shl-long v2, v2, p1

    and-long v4, v4, v16

    or-long/2addr v4, v2

    sget-object v0, Landroidx/compose/ui/graphics/Color;->Companion:Landroidx/compose/ui/graphics/Color$Companion;

    invoke-virtual {v0}, Landroidx/compose/ui/graphics/Color$Companion;->getTransparent-0d7_KjU()J

    move-result-wide v2

    invoke-virtual {v10}, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->getExclamationCutoutRadiusSp-XSAIIZE()J

    move-result-wide v6

    invoke-interface {v1, v6, v7}, Landroidx/compose/ui/unit/Density;->toPx--R2X_6o(J)F

    move-result v0

    sget-object v6, Landroidx/compose/ui/graphics/BlendMode;->Companion:Landroidx/compose/ui/graphics/BlendMode$Companion;

    invoke-virtual {v6}, Landroidx/compose/ui/graphics/BlendMode$Companion;->getSrcIn-0nO6VwU()I

    move-result v6

    const/16 v7, 0x38

    move-wide/from16 v22, v2

    move v3, v0

    move-object v0, v1

    move-wide/from16 v1, v22

    invoke-static/range {v0 .. v7}, Lcom/mi/onextbox/port/c16/DrawCompat;->drawCircle-VaOC9Bg$default(Landroidx/compose/ui/graphics/drawscope/DrawScope;JFJII)V

    move-object v1, v0

    invoke-virtual {v10}, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->getExclamationHeightSp-XSAIIZE()J

    move-result-wide v2

    invoke-interface {v1, v2, v3}, Landroidx/compose/ui/unit/Density;->toPx--R2X_6o(J)F

    move-result v0

    invoke-static {v11}, Ljava/lang/Float;->floatToRawIntBits(F)I

    move-result v2

    int-to-long v2, v2

    invoke-static {v0}, Ljava/lang/Float;->floatToRawIntBits(F)I

    move-result v0

    int-to-long v4, v0

    shl-long v2, v2, p1

    and-long v4, v4, v16

    or-long v5, v2, v4

    const/4 v10, 0x0

    const/16 v11, 0xf0

    move-object v0, v1

    move-wide v1, v8

    const/4 v9, 0x0

    move-wide/from16 v3, v18

    move-wide/from16 v7, v20

    invoke-static/range {v0 .. v11}, Lcom/mi/onextbox/port/c16/DrawCompat;->drawRoundRect-u-Aw5IA$default(Landroidx/compose/ui/graphics/drawscope/DrawScope;JJJJLandroidx/compose/ui/graphics/drawscope/Stroke;FI)V

    move-wide v2, v1

    move-object v1, v0

    const/4 v6, 0x0

    const/16 v7, 0x78

    move-wide v1, v2

    move v3, v12

    move-wide v4, v13

    invoke-static/range {v0 .. v7}, Lcom/mi/onextbox/port/c16/DrawCompat;->drawCircle-VaOC9Bg$default(Landroidx/compose/ui/graphics/drawscope/DrawScope;JFJII)V

    :cond_20a
    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;

    return-object v0
.end method
