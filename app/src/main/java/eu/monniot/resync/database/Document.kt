package eu.monniot.resync.database

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "documents")
data class Document(
    @PrimaryKey(autoGenerate = false)
    val id: String,
    val version: Int,
    val type: String, // DocumentType or CollectionType
    val name: String,
    val bookmarked: Boolean,
    val parent: String?
)


@Dao
interface DocumentsDao {
    @Query("SELECT * from documents")
    fun getAll(): Flow<List<Document>>

    @Query("SELECT * from documents where parent = :parent")
    fun getAllWithParent(parent: String): Flow<List<Document>>

    @Insert
    suspend fun insert(item: Document)

    @Update
    suspend fun update(item: Document)

    @Delete
    suspend fun delete(item: Document)
}

@Database(entities = [Document::class], version = 1)
abstract class RemarkableDatabase : RoomDatabase() {

    abstract fun documentsDao(): DocumentsDao

    companion object {
        // TODO Is the singleton actually useful ?
        private var INSTANCE: RemarkableDatabase? = null

        fun getInstance(context: Context): RemarkableDatabase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        RemarkableDatabase::class.java,
                        "remarkable_database"
                    ).fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}