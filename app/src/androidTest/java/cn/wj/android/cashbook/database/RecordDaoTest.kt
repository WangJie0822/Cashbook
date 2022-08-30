package cn.wj.android.cashbook.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cn.wj.android.cashbook.data.database.CashbookDatabase
import cn.wj.android.cashbook.data.database.dao.RecordDao
import java.io.IOException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 记录数据库操作测试
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2022/8/10
 */
@RunWith(AndroidJUnit4::class)
class RecordDaoTest {

    private lateinit var recordDao: RecordDao
    private lateinit var db: CashbookDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, CashbookDatabase::class.java
        ).build()
        recordDao = db.recordDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("cn.wj.android.cashbook", appContext.packageName)
    }
}