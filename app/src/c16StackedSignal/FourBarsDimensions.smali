.class public final Lcom/mi/onextbox/port/c16/FourBarsDimensions;
.super Lcom/mi/onextbox/port/c16/BarsDependentDimensions;
.source "go/retraceme 1473044a3194ebcf2325f73aadcc13bd0818eff92cf8946e58bc216c2171f01f"

# static fields
.field public static final INSTANCE:Lcom/mi/onextbox/port/c16/FourBarsDimensions;

# direct methods
.method static constructor <clinit>()V
    .registers 7

    new-instance v0, Lcom/mi/onextbox/port/c16/FourBarsDimensions;

    sget-object v1, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->INSTANCE:Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;

    move-object v3, v1

    invoke-virtual {v3}, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->getIconWidthFourBarsSp-XSAIIZE()J

    move-result-wide v1

    move-object v5, v3

    invoke-virtual {v5}, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->getHorizontalPaddingFourBarsSp-XSAIIZE()J

    move-result-wide v3

    invoke-virtual {v5}, Lcom/mi/onextbox/port/c16/StackedMobileIconDimensions;->getBarBaseHeightFourBarsSp-XSAIIZE()J

    move-result-wide v5

    invoke-direct/range {v0 .. v6}, Lcom/mi/onextbox/port/c16/BarsDependentDimensions;-><init>(JJJ)V

    sput-object v0, Lcom/mi/onextbox/port/c16/FourBarsDimensions;->INSTANCE:Lcom/mi/onextbox/port/c16/FourBarsDimensions;

    return-void
.end method
