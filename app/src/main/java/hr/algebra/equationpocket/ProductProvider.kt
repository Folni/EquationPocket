package hr.algebra.equationpocket

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import androidx.core.net.toUri
import hr.algebra.equationpocket.dao.Repository
import hr.algebra.equationpocket.dao.getRepository
import hr.algebra.equationpocket.model.Item

private const val AUTHORITY = "hr.algebra.equationpocket.provider"
private const val PATH = "items"
val PRODUCT_PROVIDER_CONTENT_URI: Uri = "content://$AUTHORITY/$PATH".toUri()

private const val ITEMS = 10
private const val ITEM_ID = 20

private val URI_MATCHER = with(UriMatcher(UriMatcher.NO_MATCH)) {
    addURI(AUTHORITY, PATH, ITEMS)
    addURI(AUTHORITY, "$PATH/#", ITEM_ID)
    this
}

class ProductProvider : ContentProvider() {

    private lateinit var repository: Repository

    override fun onCreate(): Boolean {
        repository = getRepository(context)
        return true
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val id = repository.insert(values)
        return ContentUris.withAppendedId(PRODUCT_PROVIDER_CONTENT_URI, id)
    }

    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor = repository.query(
        projection,
        selection,
        selectionArgs,
        sortOrder
    )

    override fun update(
        uri: Uri, values: ContentValues?, selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        when (URI_MATCHER.match(uri)) {
            ITEMS -> return repository.update(values, selection, selectionArgs)
            ITEM_ID -> {
                val id = uri.lastPathSegment
                if (id != null) {
                    return repository.update(values, "${Item::_id.name}=?", arrayOf(id))
                }
            }
        }
        throw IllegalArgumentException("Wrong URI")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        when (URI_MATCHER.match(uri)) {
            ITEMS -> return repository.delete(selection, selectionArgs)
            ITEM_ID -> {
                val id = uri.lastPathSegment
                if (id != null) {
                    return repository.delete("${Item::_id.name}=?", arrayOf(id))
                }
            }
        }
        throw IllegalArgumentException("Wrong URI")
    }

    override fun getType(uri: Uri): String? = null
}