package hr.algebra.equationpocket.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pockets")
data class Pocket(
    @PrimaryKey(autoGenerate = true)
    var _id: Long? = null,
    val name: String
) {
    override fun toString(): String = name
}
