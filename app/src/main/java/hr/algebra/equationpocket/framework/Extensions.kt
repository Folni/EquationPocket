package hr.algebra.equationpocket.framework

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import androidx.preference.PreferenceManager
import android.view.View
import android.view.animation.AnimationUtils
import androidx.core.content.edit
import androidx.core.content.getSystemService
import hr.algebra.equationpocket.PRODUCT_PROVIDER_CONTENT_URI
import hr.algebra.equationpocket.model.Item

fun View.applyAnimation(id: Int) =
    startAnimation(AnimationUtils.loadAnimation(context, id))

fun Context.setBooleanPreference(key: String, value: Boolean = true) {
    PreferenceManager.getDefaultSharedPreferences(this)
        .edit {
            putBoolean(key, value)
        }
}

fun Context.getBooleanPreference(key: String): Boolean {
    return PreferenceManager.getDefaultSharedPreferences(this)
        .getBoolean(key, false)
}

fun Context.isOnline(): Boolean {
    val connectivityManager =
        getSystemService<ConnectivityManager>() // compare with ours -> reified T: Any - returns null!
    connectivityManager?.activeNetwork?.let { network ->
        connectivityManager.getNetworkCapabilities(network)?.let { networkCapabilities ->
            return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        }
    }
    return false
}

@SuppressLint("Range")
fun Context.fetchItems(): MutableList<Item> {
    val items = mutableListOf<Item>()

    contentResolver.query(
        PRODUCT_PROVIDER_CONTENT_URI,
        null,
        null,
        null,
        null
    ).use { rs ->
        while (rs?.moveToNext() == true) {
            items.add(
                Item(
                    rs.getLong(rs.getColumnIndex(Item::_id.name)),
                    rs.getString(rs.getColumnIndex(Item::title.name)),
                    rs.getString(rs.getColumnIndex(Item::explanation.name)),
                    rs.getString(rs.getColumnIndex(Item::picturePath.name)),
                    rs.getDouble(rs.getColumnIndex(Item::price.name)),
                    rs.getDouble(rs.getColumnIndex(Item::rate.name)),
                    rs.getInt(rs.getColumnIndex(Item::count.name)),
                    rs.getInt(rs.getColumnIndex(Item::read.name)) == 1
                )
            )
        }
    }
    return items
}

fun callDelayed(delay: Long, work: Runnable) {
    Handler(Looper.getMainLooper()).postDelayed(
        work,
        delay
    )
}

inline fun <reified T : Activity> Context.startActivity() = startActivity(
    Intent(
        this,
        T::class.java
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })

inline fun <reified T : Activity> Context.startActivity(
    key: String,
    value: Int

) = startActivity(
    Intent(
        this,
        T::class.java
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(key, value)
    })

inline fun <reified T : BroadcastReceiver> Context.sendBroadcast() =
    sendBroadcast(Intent(this, T::class.java))
