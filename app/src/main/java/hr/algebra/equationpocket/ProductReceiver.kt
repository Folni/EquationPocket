package hr.algebra.equationpocket

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import hr.algebra.equationpocket.framework.setBooleanPreference
import hr.algebra.equationpocket.framework.startActivity

const val DATA_IMPORTED = "hr.algebra.equationpocket.data_imported"

class ProductReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        context.setBooleanPreference(DATA_IMPORTED)
        context.startActivity<HostActivity>()    }
}