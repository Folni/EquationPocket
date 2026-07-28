package hr.algebra.equationpocket.model

data class Item(
    var _id: Long?,
    val title: String,
    val explanation: String,
    val picturePath: String,
    val price: Double,
    val rate: Double,
    val count: Int,
    var read: Boolean
)