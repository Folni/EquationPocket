package hr.algebra.equationpocket.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "equations",
    foreignKeys = [
        ForeignKey(
            entity = Pocket::class,
            parentColumns = ["_id"],
            childColumns = ["pocketId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Equation(
    @PrimaryKey(autoGenerate = true)
    var _id: Long? = null,
    val name: String,
    val formula: String,
    val description: String? = null,
    val pocketId: Long,
    val isFavorite: Boolean = false
)
