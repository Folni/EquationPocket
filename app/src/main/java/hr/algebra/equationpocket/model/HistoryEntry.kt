package hr.algebra.equationpocket.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class HistoryEntry(
    val inputs: String,
    val result: String,
    val unknownVar: String
) : Parcelable