package eu.monniot.resync.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class RemarkableDatabaseTest {

    private lateinit var todoDao: DocumentsDao
    private lateinit var db: RemarkableDatabase

    @Before
    fun createDb() {
        val context =               InstrumentationRegistry.getInstrumentation().targetContext

        db = Room.inMemoryDatabaseBuilder(context, RemarkableDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        todoDao = db.documentsDao()
    }

    @After
    @Throws(IOException::class)
    fun deleteDb() {
        db.close()
    }

    // TODO Run it at least once to make sure it works as expected :)
    @Test
    @Throws(Exception::class)
    fun insertAndGetTodo() = runBlocking {
        val todoItem = Document(
            id = "xxxxx",
            version = 1,
            type = "DocumentType",
            name = "Doc",
            bookmarked = true,
            parent = null
        )
        todoDao.upsert(todoItem)
        val items = todoDao.getAll().single()
        assertEquals(items, listOf(todoItem))
    }
}