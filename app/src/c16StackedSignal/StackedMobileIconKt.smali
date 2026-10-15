.class public abstract Lcom/mi/onextbox/port/c16/StackedMobileIconKt;
.super Ljava/lang/Object;

.method public static drawMobileIconBar-n3YQ8UE$default(Landroidx/compose/ui/graphics/drawscope/DrawScope;IIJJJ)V
    .registers 30

    const v0, 0x3e99999a    # 0.3f

    move-wide/from16 v1, p7

    invoke-static {v0, v1, v2}, Landroidx/compose/ui/graphics/Color;->copy-wmQWz5c$default(FJ)J

    move-result-wide v3

    const/16 v0, 0x20

    shr-long v5, p5, v0

    long-to-int v5, v5

    invoke-static {v5}, Ljava/lang/Float;->intBitsToFloat(I)F

    move-result v5

    const/4 v6, 0x2

    int-to-float v6, v6

    div-float/2addr v5, v6

    invoke-static {v5}, Ljava/lang/Float;->floatToRawIntBits(F)I

    move-result v6

    int-to-long v6, v6

    invoke-static {v5}, Ljava/lang/Float;->floatToRawIntBits(F)I

    move-result v5

    int-to-long v8, v5

    shl-long v5, v6, v0

    const-wide v10, 0xffffffffL

    and-long v7, v8, v10

    or-long v16, v5, v7

    move/from16 v0, p1

    move/from16 v5, p2

    if-lt v0, v5, :cond_32

    move-wide v10, v1

    goto :goto_33

    :cond_32
    move-wide v10, v3

    :goto_33
    const/16 v19, 0x0

    const/16 v20, 0xf0

    const/16 v18, 0x0

    move-object/from16 v9, p0

    move-wide/from16 v12, p3

    move-wide/from16 v14, p5

    invoke-static/range {v9 .. v20}, Lcom/mi/onextbox/port/c16/DrawCompat;->drawRoundRect-u-Aw5IA$default(Landroidx/compose/ui/graphics/drawscope/DrawScope;JJJJLandroidx/compose/ui/graphics/drawscope/Stroke;FI)V

    return-void
.end method
