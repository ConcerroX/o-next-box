package com.mi.onextbox.lsp

import com.mi.onextbox.lsp.compat.HotReloadLifecycle
import org.lsposed.corepatch.XposedHelper
import org.lsposed.corepatch.hook.*

internal object CorePatchIntegration {
    fun hook(loader: ClassLoader) {
        // Inactive means no signing/digest hooks or ART field writes at all.
        if (!CorePatchSettings.captureBootOptions()) return
        // Shared-UID/signing state cannot be rolled back safely during a live module replacement.
        HotReloadLifecycle.register("corepatch.boot-only", canReload = { false })
        XposedHelper.setHostClassLoader(loader)
        listOf(ApkSignatureVerifierHook, ApkSigningBlockUtilsHook, ApplicationInfoHook,
            AssetManagerHook, FrameworkParsingPackageUtilsHook, InstallPackageHelperHook,
            KeySetManagerServiceHook, MessageDigestHook, NtConfigListServiceImplHook,
            PackageManagerServiceHook, PackageManagerServiceUtilsHook, ReconcilePackageUtilsHook,
            ScanPackageUtilsHook, SharedUserSettingHook, SigningDetailsHook, StrictJarVerifierHook,
            VerificationParamsHook, VerifyingSessionHook).forEach { it.init() }
    }
}
