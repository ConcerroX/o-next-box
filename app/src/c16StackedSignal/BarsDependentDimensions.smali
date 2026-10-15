.class public abstract Lcom/mi/onextbox/port/c16/BarsDependentDimensions;
.super Ljava/lang/Object;
.source "go/retraceme 1473044a3194ebcf2325f73aadcc13bd0818eff92cf8946e58bc216c2171f01f"

# instance fields
.field public final barBaseHeight:J

.field public final barsHorizontalPadding:J

.field public final totalWidth:J

# direct methods
.method public constructor <init>(JJJ)V
    .registers 7

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-wide p1, p0, Lcom/mi/onextbox/port/c16/BarsDependentDimensions;->totalWidth:J

    iput-wide p3, p0, Lcom/mi/onextbox/port/c16/BarsDependentDimensions;->barsHorizontalPadding:J

    iput-wide p5, p0, Lcom/mi/onextbox/port/c16/BarsDependentDimensions;->barBaseHeight:J

    return-void
.end method

# virtual methods
.method public final getBarBaseHeight-XSAIIZE()J
    .registers 3

    iget-wide v0, p0, Lcom/mi/onextbox/port/c16/BarsDependentDimensions;->barBaseHeight:J

    return-wide v0
.end method

.method public final getBarsHorizontalPadding-XSAIIZE()J
    .registers 3

    iget-wide v0, p0, Lcom/mi/onextbox/port/c16/BarsDependentDimensions;->barsHorizontalPadding:J

    return-wide v0
.end method

.method public final getTotalWidth-XSAIIZE()J
    .registers 3

    iget-wide v0, p0, Lcom/mi/onextbox/port/c16/BarsDependentDimensions;->totalWidth:J

    return-wide v0
.end method
