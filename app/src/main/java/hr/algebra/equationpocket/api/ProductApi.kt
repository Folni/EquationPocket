package hr.algebra.equationpocket.api

import hr.algebra.equationpocket.api.ProductItem
import retrofit2.Call
import retrofit2.http.GET

const val API_URL = "https://fakestoreapi.com/"

interface ProductApi {
    @GET("products")
    fun fetchItems() : Call<List<ProductItem>>
}