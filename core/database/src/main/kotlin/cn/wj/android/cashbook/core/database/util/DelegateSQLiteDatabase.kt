/*
 * Copyright 2021 The Cashbook Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("unused")

package cn.wj.android.cashbook.core.database.util

import android.app.ActivityManager
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteCursor
import android.database.sqlite.SQLiteCursorDriver
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteProgram
import android.database.sqlite.SQLiteQuery
import android.database.sqlite.SQLiteStatement
import android.database.sqlite.SQLiteTransactionListener
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.text.TextUtils
import android.util.Pair
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteProgram
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteStatement
import java.io.File
import java.io.IOException
import java.util.Locale

class DelegateSQLiteDatabase(
    private val delegate: SQLiteDatabase,
) : SupportSQLiteDatabase {
    override fun compileStatement(sql: String): SupportSQLiteStatement {
        return FrameworkSQLiteStatement(delegate.compileStatement(sql))
    }

    override fun beginTransaction() {
        delegate.beginTransaction()
    }

    override fun beginTransactionNonExclusive() {
        delegate.beginTransactionNonExclusive()
    }

    override fun beginTransactionWithListener(
        transactionListener: SQLiteTransactionListener,
    ) {
        delegate.beginTransactionWithListener(transactionListener)
    }

    override fun beginTransactionWithListenerNonExclusive(
        transactionListener: SQLiteTransactionListener,
    ) {
        delegate.beginTransactionWithListenerNonExclusive(transactionListener)
    }

    override fun endTransaction() {
        delegate.endTransaction()
    }

    override fun setTransactionSuccessful() {
        delegate.setTransactionSuccessful()
    }

    override fun inTransaction(): Boolean {
        return delegate.inTransaction()
    }

    override val isDbLockedByCurrentThread: Boolean
        get() = delegate.isDbLockedByCurrentThread

    override fun yieldIfContendedSafely(): Boolean {
        return delegate.yieldIfContendedSafely()
    }

    override fun yieldIfContendedSafely(sleepAfterYieldDelayMillis: Long): Boolean {
        return delegate.yieldIfContendedSafely(sleepAfterYieldDelayMillis)
    }

    override var version: Int
        get() = delegate.version
        set(value) {
            delegate.version = value
        }

    override var maximumSize: Long
        get() = delegate.maximumSize
        set(numBytes) {
            delegate.maximumSize = numBytes
        }

    override fun setMaximumSize(numBytes: Long): Long {
        delegate.maximumSize = numBytes
        return delegate.maximumSize
    }

    override val isExecPerConnectionSQLSupported: Boolean
        @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    override fun execPerConnectionSQL(sql: String, bindArgs: Array<out Any?>?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Api30Impl.execPerConnectionSQL(delegate, sql, bindArgs)
        } else {
            throw UnsupportedOperationException(
                "execPerConnectionSQL is not supported on a " +
                    "SDK version lower than 30, current version is: " + Build.VERSION.SDK_INT,
            )
        }
    }

    override var pageSize: Long
        get() = delegate.pageSize
        set(numBytes) {
            delegate.pageSize = numBytes
        }

    override fun query(query: String): Cursor {
        return query(SimpleSQLiteQuery(query))
    }

    override fun query(query: String, bindArgs: Array<out Any?>): Cursor {
        return query(SimpleSQLiteQuery(query, bindArgs))
    }

    override fun query(query: SupportSQLiteQuery): Cursor {
        val cursorFactory = {
                _: SQLiteDatabase?,
                masterQuery: SQLiteCursorDriver?,
                editTable: String?,
                sqLiteQuery: SQLiteQuery?,
            ->
            query.bindTo(
                FrameworkSQLiteProgram(
                    sqLiteQuery!!,
                ),
            )
            SQLiteCursor(masterQuery, editTable, sqLiteQuery)
        }

        return delegate.rawQueryWithFactory(
            cursorFactory,
            query.sql,
            EMPTY_STRING_ARRAY,
            null,
        )
    }

    override fun query(
        query: SupportSQLiteQuery,
        cancellationSignal: CancellationSignal?,
    ): Cursor {
        return SupportSQLiteCompat.Api16Impl.rawQueryWithFactory(
            delegate,
            query.sql,
            EMPTY_STRING_ARRAY,
            null,
            cancellationSignal!!,
        ) {
                _: SQLiteDatabase?,
                masterQuery: SQLiteCursorDriver?,
                editTable: String?,
                sqLiteQuery: SQLiteQuery?,
            ->
            query.bindTo(
                FrameworkSQLiteProgram(
                    sqLiteQuery!!,
                ),
            )
            SQLiteCursor(masterQuery, editTable, sqLiteQuery)
        }
    }

    @Throws(SQLException::class)
    override fun insert(table: String, conflictAlgorithm: Int, values: ContentValues): Long {
        return delegate.insertWithOnConflict(table, null, values, conflictAlgorithm)
    }

    override fun delete(table: String, whereClause: String?, whereArgs: Array<out Any?>?): Int {
        val query = buildString {
            append("DELETE FROM ")
            append(table)
            if (!whereClause.isNullOrEmpty()) {
                append(" WHERE ")
                append(whereClause)
            }
        }
        val statement = compileStatement(query)
        SimpleSQLiteQuery.bind(statement, whereArgs)
        return statement.executeUpdateDelete()
    }

    override fun update(
        table: String,
        conflictAlgorithm: Int,
        values: ContentValues,
        whereClause: String?,
        whereArgs: Array<out Any?>?,
    ): Int {
        // taken from SQLiteDatabase class.
        require(values.size() != 0) { "Empty values" }

        // move all bind args to one array
        val setValuesSize = values.size()
        val bindArgsSize =
            if (whereArgs == null) setValuesSize else setValuesSize + whereArgs.size
        val bindArgs = arrayOfNulls<Any>(bindArgsSize)
        val sql = buildString {
            append("UPDATE ")
            append(CONFLICT_VALUES[conflictAlgorithm])
            append(table)
            append(" SET ")

            var i = 0
            for (colName in values.keySet()) {
                append(if (i > 0) "," else "")
                append(colName)
                bindArgs[i++] = values[colName]
                append("=?")
            }
            if (whereArgs != null) {
                i = setValuesSize
                while (i < bindArgsSize) {
                    bindArgs[i] = whereArgs[i - setValuesSize]
                    i++
                }
            }
            if (!TextUtils.isEmpty(whereClause)) {
                append(" WHERE ")
                append(whereClause)
            }
        }
        val stmt = compileStatement(sql)
        SimpleSQLiteQuery.bind(stmt, bindArgs)
        return stmt.executeUpdateDelete()
    }

    @Throws(SQLException::class)
    override fun execSQL(sql: String) {
        delegate.execSQL(sql)
    }

    @Throws(SQLException::class)
    override fun execSQL(sql: String, bindArgs: Array<out Any?>) {
        delegate.execSQL(sql, bindArgs)
    }

    override val isReadOnly: Boolean
        get() = delegate.isReadOnly

    override val isOpen: Boolean
        get() = delegate.isOpen

    override fun needUpgrade(newVersion: Int): Boolean {
        return delegate.needUpgrade(newVersion)
    }

    override val path: String?
        get() = delegate.path

    override fun setLocale(locale: Locale) {
        delegate.setLocale(locale)
    }

    override fun setMaxSqlCacheSize(cacheSize: Int) {
        delegate.setMaxSqlCacheSize(cacheSize)
    }

    override fun setForeignKeyConstraintsEnabled(enabled: Boolean) {
        SupportSQLiteCompat.Api16Impl.setForeignKeyConstraintsEnabled(delegate, enabled)
    }

    override fun enableWriteAheadLogging(): Boolean {
        return delegate.enableWriteAheadLogging()
    }

    override fun disableWriteAheadLogging() {
        SupportSQLiteCompat.Api16Impl.disableWriteAheadLogging(delegate)
    }

    override val isWriteAheadLoggingEnabled: Boolean
        get() = SupportSQLiteCompat.Api16Impl.isWriteAheadLoggingEnabled(delegate)

    override val attachedDbs: List<Pair<String, String>>?
        get() = delegate.attachedDbs

    override val isDatabaseIntegrityOk: Boolean
        get() = delegate.isDatabaseIntegrityOk

    @Throws(IOException::class)
    override fun close() {
        delegate.close()
    }

    /**
     * Checks if this object delegates to the same given database reference.
     */
    fun isDelegate(sqLiteDatabase: SQLiteDatabase): Boolean {
        return delegate == sqLiteDatabase
    }

    @RequiresApi(30)
    internal object Api30Impl {
        @DoNotInline
        fun execPerConnectionSQL(
            sQLiteDatabase: SQLiteDatabase,
            sql: String,
            bindArgs: Array<out Any?>?,
        ) {
            sQLiteDatabase.execPerConnectionSQL(sql, bindArgs)
        }
    }

    companion object {
        private val CONFLICT_VALUES =
            arrayOf(
                "",
                " OR ROLLBACK ",
                " OR ABORT ",
                " OR FAIL ",
                " OR IGNORE ",
                " OR REPLACE ",
            )
        private val EMPTY_STRING_ARRAY = arrayOfNulls<String>(0)
    }
}

open class FrameworkSQLiteProgram(
    private val delegate: SQLiteProgram,
) : SupportSQLiteProgram {
    override fun bindNull(index: Int) {
        delegate.bindNull(index)
    }

    override fun bindLong(index: Int, value: Long) {
        delegate.bindLong(index, value)
    }

    override fun bindDouble(index: Int, value: Double) {
        delegate.bindDouble(index, value)
    }

    override fun bindString(index: Int, value: String) {
        delegate.bindString(index, value)
    }

    override fun bindBlob(index: Int, value: ByteArray) {
        delegate.bindBlob(index, value)
    }

    override fun clearBindings() {
        delegate.clearBindings()
    }

    override fun close() {
        delegate.close()
    }
}

class FrameworkSQLiteStatement(
    private val delegate: SQLiteStatement,
) : FrameworkSQLiteProgram(
    delegate,
),
    SupportSQLiteStatement {
    override fun execute() {
        delegate.execute()
    }

    override fun executeUpdateDelete(): Int {
        return delegate.executeUpdateDelete()
    }

    override fun executeInsert(): Long {
        return delegate.executeInsert()
    }

    override fun simpleQueryForLong(): Long {
        return delegate.simpleQueryForLong()
    }

    override fun simpleQueryForString(): String? {
        return delegate.simpleQueryForString()
    }
}

class SupportSQLiteCompat private constructor() {
    /**
     * Class for accessing functions that require SDK version 16 and higher.
     *
     * @hide
     */
    object Api16Impl {
        /**
         * Cancels the operation and signals the cancellation listener. If the operation has not yet
         * started, then it will be canceled as soon as it does.
         *
         * @hide
         */
        @JvmStatic
        fun cancel(cancellationSignal: CancellationSignal) {
            cancellationSignal.cancel()
        }

        /**
         * Creates a cancellation signal, initially not canceled.
         *
         * @return a new cancellation signal
         *
         * @hide
         */
        @JvmStatic
        fun createCancellationSignal(): CancellationSignal {
            return CancellationSignal()
        }

        /**
         * Deletes a database including its journal file and other auxiliary files
         * that may have been created by the database engine.
         *
         * @param file The database file path.
         * @return True if the database was successfully deleted.
         *
         * @hide
         */
        @JvmStatic
        fun deleteDatabase(file: File): Boolean {
            return SQLiteDatabase.deleteDatabase(file)
        }

        /**
         * Runs the provided SQL and returns a cursor over the result set.
         *
         * @param sql the SQL query. The SQL string must not be ; terminated
         * @param selectionArgs You may include ?s in where clause in the query,
         * which will be replaced by the values from selectionArgs. The
         * values will be bound as Strings.
         * @param editTable the name of the first table, which is editable
         * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
         * If the operation is canceled, then `OperationCanceledException` will be thrown
         * when the query is executed.
         * @param cursorFactory the cursor factory to use, or null for the default factory
         * @return A [Cursor] object, which is positioned before the first entry. Note that
         * [Cursor]s are not synchronized, see the documentation for more details.
         *
         * @hide
         */
        @JvmStatic
        fun rawQueryWithFactory(
            sQLiteDatabase: SQLiteDatabase,
            sql: String,
            selectionArgs: Array<out String?>,
            editTable: String?,
            cancellationSignal: CancellationSignal,
            cursorFactory: SQLiteDatabase.CursorFactory,
        ): Cursor {
            return sQLiteDatabase.rawQueryWithFactory(
                cursorFactory,
                sql,
                selectionArgs,
                editTable,
                cancellationSignal,
            )
        }

        /**
         * Sets whether foreign key constraints are enabled for the database.
         *
         * @param enable True to enable foreign key constraints, false to disable them.
         *
         * @throws [IllegalStateException] if the are transactions is in progress
         * when this method is called.
         *
         * @hide
         */
        @JvmStatic
        fun setForeignKeyConstraintsEnabled(
            sQLiteDatabase: SQLiteDatabase,
            enable: Boolean,
        ) {
            sQLiteDatabase.setForeignKeyConstraintsEnabled(enable)
        }

        /**
         * This method disables the features enabled by
         * [SQLiteDatabase.enableWriteAheadLogging].
         *
         * @throws - if there are transactions in progress at the
         * time this method is called.  WAL mode can only be changed when there are no
         * transactions in progress.
         *
         * @hide
         */
        @JvmStatic
        fun disableWriteAheadLogging(sQLiteDatabase: SQLiteDatabase) {
            sQLiteDatabase.disableWriteAheadLogging()
        }

        /**
         * Returns true if [SQLiteDatabase.enableWriteAheadLogging] logging has been enabled for
         * this database.
         *
         * For details, see [SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING].
         *
         * @return True if write-ahead logging has been enabled for this database.
         *
         * @hide
         */
        @JvmStatic
        fun isWriteAheadLoggingEnabled(sQLiteDatabase: SQLiteDatabase): Boolean {
            return sQLiteDatabase.isWriteAheadLoggingEnabled
        }

        /**
         * Sets [SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING] flag if `enabled` is `true`, unsets
         * otherwise.
         *
         * @hide
         */
        @JvmStatic
        fun setWriteAheadLoggingEnabled(
            sQLiteOpenHelper: SQLiteOpenHelper,
            enabled: Boolean,
        ) {
            sQLiteOpenHelper.setWriteAheadLoggingEnabled(enabled)
        }
    }

    /**
     * Helper for accessing functions that require SDK version 19 and higher.
     *
     * @hide
     */
    object Api19Impl {
        /**
         * Return the URI at which notifications of changes in this Cursor's data
         * will be delivered.
         *
         * @return Returns a URI that can be used with [ContentResolver.registerContentObserver] to
         * find out about changes to this Cursor's data. May be null if no notification URI has been
         * set.
         *
         * @hide
         */
        @JvmStatic
        fun getNotificationUri(cursor: Cursor): Uri {
            return cursor.notificationUri
        }

        /**
         * Returns true if this is a low-RAM device.  Exactly whether a device is low-RAM
         * is ultimately up to the device configuration, but currently it generally means
         * something with 1GB or less of RAM.  This is mostly intended to be used by apps
         * to determine whether they should turn off certain features that require more RAM.
         *
         * @hide
         */
        @JvmStatic
        fun isLowRamDevice(activityManager: ActivityManager): Boolean {
            return activityManager.isLowRamDevice
        }
    }

    /**
     * Helper for accessing functions that require SDK version 21 and higher.
     *
     * @hide
     */
    object Api21Impl {
        /**
         * Returns the absolute path to the directory on the filesystem.
         *
         * @return The path of the directory holding application files that will not be
         * automatically backed up to remote storage.
         *
         * @hide
         */
        @JvmStatic
        fun getNoBackupFilesDir(context: Context): File {
            return context.noBackupFilesDir
        }
    }

    /**
     * Helper for accessing functions that require SDK version 23 and higher.
     *
     * @hide
     */
    object Api23Impl {
        /**
         * Sets a [Bundle] that will be returned by [Cursor.getExtras].
         *
         * @param extras [Bundle] to set, or null to set an empty bundle.
         *
         * @hide
         */
        @JvmStatic
        fun setExtras(cursor: Cursor, extras: Bundle) {
            cursor.extras = extras
        }
    }

    /**
     * Helper for accessing functions that require SDK version 29 and higher.
     *
     * @hide
     */
    @RequiresApi(29)
    object Api29Impl {
        /**
         * Similar to [Cursor.setNotificationUri], except this version
         * allows to watch multiple content URIs for changes.
         *
         * @param cr The content resolver from the caller's context. The listener attached to
         * this resolver will be notified.
         * @param uris The content URIs to watch.
         *
         * @hide
         */
        @JvmStatic
        fun setNotificationUris(
            cursor: Cursor,
            cr: ContentResolver,
            uris: List<Uri?>,
        ) {
            cursor.setNotificationUris(cr, uris)
        }

        /**
         * Return the URIs at which notifications of changes in this Cursor's data
         * will be delivered, as previously set by [setNotificationUris].
         *
         * @return Returns URIs that can be used with [ContentResolver.registerContentObserver]
         * to find out about changes to this Cursor's data. May be null if no notification URI has
         * been set.
         *
         * @hide
         */
        @JvmStatic
        fun getNotificationUris(cursor: Cursor): List<Uri> {
            return cursor.notificationUris!!
        }
    }
}
