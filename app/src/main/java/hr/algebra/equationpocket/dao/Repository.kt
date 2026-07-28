package hr.algebra.equationpocket.dao

import android.content.ContentValues
import android.database.Cursor

interface Repository {
    fun delete(selection: String?, selectionArgs: Array<String>?): Int

    fun update(
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int

    fun query(
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor

    fun insert(values: ContentValues?): Long
}