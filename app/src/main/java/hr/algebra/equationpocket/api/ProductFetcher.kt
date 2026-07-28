package hr.algebra.equationpocket.api

import android.content.ContentValues
import android.content.Context
import android.util.Log
import hr.algebra.equationpocket.PRODUCT_PROVIDER_CONTENT_URI
import hr.algebra.equationpocket.ProductReceiver
import hr.algebra.equationpocket.framework.sendBroadcast
import hr.algebra.equationpocket.handler.download
import hr.algebra.equationpocket.model.Item
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ProductFetcher(private val context: Context) {

    private val productApi: ProductApi

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl(API_URL) // Ovo je https://fakestoreapi.com/
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        productApi = retrofit.create(ProductApi::class.java)
    }

    fun fetchItems() {
        val request = productApi.fetchItems()

        request.enqueue(object : Callback<List<ProductItem>> {
            override fun onResponse(
                call: Call<List<ProductItem>>,
                response: Response<List<ProductItem>>
            ) {
                response.body()?.let { populateItems(it) }
            }

            override fun onFailure(call: Call<List<ProductItem>>, t: Throwable) {
                Log.e("ERROR", t.toString(), t)
            }
        })
    }

    private fun populateItems(products: List<ProductItem>) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            products.forEach { product ->
                val picturePath = download(context, product.url)

                val values = ContentValues().apply {
                    put(Item::title.name, product.title)
                    put(Item::explanation.name, product.explanation)
                    put(Item::picturePath.name, picturePath ?: "")
                    put(Item::price.name, product.price)

                    put(Item::rate.name, product.rating.rate)
                    put(Item::count.name, product.rating.count)

                    put(Item::read.name, false)
                }

                context.contentResolver.insert(
                    PRODUCT_PROVIDER_CONTENT_URI,
                    values
                )
            }
            context.sendBroadcast<ProductReceiver>()
        }
    }
}