package com.mi.onextbox.refresh

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.UserManager
import com.mi.onextbox.ui.common.AppLogStore
import com.mi.onextbox.ui.onboarding.ActivationGate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal object RefreshRateBootScheduler {
    private const val JOB_ID = 0x4F525246
    fun schedule(context: Context) {
        val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
        if (!context.getSystemService(UserManager::class.java).isUserUnlocked) return
        if (!ActivationGate.isActivated(context) || !RefreshRatePreferences.readAutoStartEnabled(context) ||
            RefreshRatePreferences.readSelectedMode(context) == null) {
            scheduler.cancel(JOB_ID)
            return
        }
        // BOOT_COMPLETED and USER_UNLOCKED may arrive together; keep a single job.
        if (scheduler.getPendingJob(JOB_ID) != null) return
        val job = JobInfo.Builder(JOB_ID, ComponentName(context, RefreshRateBootJobService::class.java))
            .setMinimumLatency(1_000).setOverrideDeadline(15_000).build()
        if (scheduler.schedule(job) != JobScheduler.RESULT_SUCCESS)
            AppLogStore.w("RefreshRate", "Unable to schedule boot restore")
    }
}

class RefreshRateBootJobService : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var task: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        task = scope.launch {
            try {
                if (ActivationGate.isActivated(applicationContext)) {
                    // Boot-time display/root readiness is transient. Bound retries and
                    // re-read the setting each time so disabling auto start takes effect.
                    for (attempt in 0..2) {
                        if (attempt > 0) delay(attempt * 2_000L)
                        val applied = try { RefreshRateAutoApplier.applyIfEnabled(applicationContext) }
                        catch (cancel: CancellationException) { throw cancel }
                        catch (error: Exception) {
                            AppLogStore.w("RefreshRate", "Boot attempt failed: ${error.javaClass.simpleName}")
                            false
                        }
                        if (applied) break
                        AppLogStore.w("RefreshRate", "Boot restore attempt ${attempt + 1}/3 unsuccessful")
                    }
                }
                jobFinished(params, false)
            } catch (_: CancellationException) {
                // JobScheduler owns rescheduling after a system stop.
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        task?.cancel()
        task = null
        return true
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
