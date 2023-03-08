package cn.wj.android.cashbook.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.Before

class DatabaseTest {

    private lateinit var db: CashbookDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            CashbookDatabase::class.java,
        ).build()
    }
}