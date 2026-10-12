package com.mi.onextbox.errors

import androidx.test.ext.junit.runners.AndroidJUnit4
import android.database.sqlite.SQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppErrorDatabaseTest {
    @Test fun versionOneHistorySurvivesMetadataMigration() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "test-errors-migration-${UUID.randomUUID()}.db"
        try {
            SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name).also { it.parentFile?.mkdirs() }, null).use { old ->
                old.execSQL("CREATE TABLE errors (id INTEGER PRIMARY KEY AUTOINCREMENT, time INTEGER NOT NULL, package TEXT NOT NULL, process TEXT NOT NULL, uid INTEGER NOT NULL, pid INTEGER NOT NULL, kind TEXT NOT NULL, summary TEXT NOT NULL, detail TEXT NOT NULL, truncated INTEGER NOT NULL, app_name TEXT NOT NULL, version TEXT NOT NULL)")
                old.execSQL("INSERT INTO errors VALUES (1,1,'com.example.test','com.example.test',10123,123,'CRASH','Old summary','Old stack',0,'Old app','1.0')")
                old.version = 1
            }
            AppErrorDatabase(context, name).use { upgraded ->
                val old = requireNotNull(upgraded.detail(1))
                assertEquals("Old stack", old.detail)
                assertEquals("Old app", old.appName)
                assertEquals(AppErrorMetadata(), old.metadata)
                val metadata = AppErrorMetadata(exceptionClass = "SampleError", throwFile = "Test.kt", throwLine = 5, sdk = 37)
                val id = upgraded.append(old.copy(id = 0, metadata = metadata))
                assertEquals(metadata, upgraded.detail(id)?.metadata)
                upgraded.list().use { assertEquals(2, it.count) }
            }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun boundedHistoryPreservesDetailsAcrossReopening() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "test-errors-${UUID.randomUUID()}.db"
        try {
            val db = AppErrorDatabase(context, name)
            var last = 0L
            repeat(AppErrorRules.MAX_RECORDS + 3) { index ->
                last = db.append(AppErrorRecord(time = index + 1L, packageName = "com.example.test", processName = "com.example.test:worker", uid = 10123, pid = 123, kind = AppErrorKind.ANR, summary = "Timeout $index", detail = "Stack $index", appName = "Test App", version = "1.2 (123)"))
            }
            db.list().use { cursor ->
                assertEquals(AppErrorRules.MAX_RECORDS, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertEquals(last, cursor.readRecord().id)
            }
            db.close()
            AppErrorDatabase(context, name).use { reopened ->
                assertEquals("Stack 202", reopened.detail(last)?.detail)
                assertEquals("Test App", reopened.detail(last)?.appName)
                assertEquals("1.2 (123)", reopened.detail(last)?.version)
                assertEquals(1, reopened.remove(last))
                assertNull(reopened.detail(last))
                assertEquals(AppErrorRules.MAX_RECORDS - 1, reopened.clear())
                reopened.list().use { assertEquals(0, it.count) }
            }
        } finally { context.deleteDatabase(name) }
    }
}
