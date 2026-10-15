.class public abstract Lcom/mi/onextbox/port/c16/DrawCompat;
.super Ljava/lang/Object;

.method public static synthetic drawCircle-VaOC9Bg$default(Landroidx/compose/ui/graphics/drawscope/DrawScope;JFJII)V
    .registers 18
    and-int/lit8 v0, p7, 0x2
    if-eqz v0, :cond_f
    invoke-interface {p0}, Landroidx/compose/ui/graphics/drawscope/DrawScope;->getSize-NH-jbRc()J
    move-result-wide v0
    invoke-static {v0, v1}, Landroidx/compose/ui/geometry/Size;->getMinDimension-impl(J)F
    move-result p3
    const/high16 v0, 0x40000000    # 2.0f
    div-float/2addr p3, v0
    :cond_f
    move v3, p3
    and-int/lit8 p3, p7, 0x4
    if-eqz p3, :cond_1a
    invoke-interface {p0}, Landroidx/compose/ui/graphics/drawscope/DrawScope;->getCenter-F1C5BW0()J
    move-result-wide v0
    move-wide v4, v0
    goto :goto_1b
    :cond_1a
    move-wide v4, p4
    :goto_1b
    sget-object v7, Landroidx/compose/ui/graphics/drawscope/Fill;->INSTANCE:Landroidx/compose/ui/graphics/drawscope/Fill;
    and-int/lit8 p3, p7, 0x40
    if-eqz p3, :cond_29
    sget-object p3, Landroidx/compose/ui/graphics/drawscope/DrawScope;->Companion:Landroidx/compose/ui/graphics/drawscope/DrawScope$Companion;
    invoke-virtual {p3}, Landroidx/compose/ui/graphics/drawscope/DrawScope$Companion;->getDefaultBlendMode-0nO6VwU()I
    move-result p3
    move v9, p3
    goto :goto_2b
    :cond_29
    move/from16 v9, p6
    :goto_2b
    const/high16 v6, 0x3f800000    # 1.0f
    const/4 v8, 0x0
    move-object v0, p0
    move-wide v1, p1
    invoke-interface/range {v0 .. v9}, Landroidx/compose/ui/graphics/drawscope/DrawScope;->drawCircle-VaOC9Bg(JFJFLandroidx/compose/ui/graphics/drawscope/DrawStyle;Landroidx/compose/ui/graphics/ColorFilter;I)V
    return-void
.end method
.method public static synthetic drawRoundRect-u-Aw5IA$default(Landroidx/compose/ui/graphics/drawscope/DrawScope;JJJJLandroidx/compose/ui/graphics/drawscope/Stroke;FI)V
    .registers 27
    and-int/lit8 v0, p11, 0x2
    if-eqz v0, :cond_c
    sget-object v0, Landroidx/compose/ui/geometry/Offset;->Companion:Landroidx/compose/ui/geometry/Offset$Companion;
    invoke-virtual {v0}, Landroidx/compose/ui/geometry/Offset$Companion;->getZero-F1C5BW0()J
    move-result-wide v0
    move-wide v5, v0
    goto :goto_e
    :cond_c
    move-wide/from16 v5, p3
    :goto_e
    and-int/lit8 v0, p11, 0x4
    if-eqz v0, :cond_1c
    invoke-interface {p0}, Landroidx/compose/ui/graphics/drawscope/DrawScope;->getSize-NH-jbRc()J
    move-result-wide v0
    invoke-static {v0, v1, v5, v6}, Landroidx/compose/ui/graphics/drawscope/DrawScope;->offsetSize-PENXr5M(JJ)J
    move-result-wide v0
    move-wide v7, v0
    goto :goto_1e
    :cond_1c
    move-wide/from16 v7, p5
    :goto_1e
    and-int/lit8 v0, p11, 0x10
    if-eqz v0, :cond_26
    sget-object v0, Landroidx/compose/ui/graphics/drawscope/Fill;->INSTANCE:Landroidx/compose/ui/graphics/drawscope/Fill;
    move-object v11, v0
    goto :goto_28
    :cond_26
    move-object/from16 v11, p9
    :goto_28
    and-int/lit8 v0, p11, 0x20
    if-eqz v0, :cond_30
    const/high16 v0, 0x3f800000    # 1.0f
    move v12, v0
    goto :goto_32
    :cond_30
    move/from16 v12, p10
    :goto_32
    sget-object v0, Landroidx/compose/ui/graphics/drawscope/DrawScope;->Companion:Landroidx/compose/ui/graphics/drawscope/DrawScope$Companion;
    invoke-virtual {v0}, Landroidx/compose/ui/graphics/drawscope/DrawScope$Companion;->getDefaultBlendMode-0nO6VwU()I
    move-result v14
    const/4 v13, 0x0
    move-object v2, p0
    move-wide/from16 v3, p1
    move-wide/from16 v9, p7
    invoke-interface/range {v2 .. v14}, Landroidx/compose/ui/graphics/drawscope/DrawScope;->drawRoundRect-u-Aw5IA(JJJJLandroidx/compose/ui/graphics/drawscope/DrawStyle;FLandroidx/compose/ui/graphics/ColorFilter;I)V
    return-void
.end method
