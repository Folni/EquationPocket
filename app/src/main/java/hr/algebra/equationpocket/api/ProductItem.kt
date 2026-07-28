package hr.algebra.equationpocket.api

import com.google.gson.annotations.SerializedName

data class ProductItem(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("price") val price: Double,
    @SerializedName("description") val explanation: String,
    @SerializedName("image") val url: String,
    @SerializedName("category") val category: String,
    @SerializedName("rating") val rating: Rating
)

// DODANO: Nova pomoćna klasa za ugniježđeni JSON objekt
data class Rating(
    @SerializedName("rate") val rate: Double,
    @SerializedName("count") val count: Int
)