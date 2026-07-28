package hr.algebra.equationpocket

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import hr.algebra.equationpocket.adapter.ItemPagerAdapter
import hr.algebra.equationpocket.databinding.ActivityItemPagerBinding
import hr.algebra.equationpocket.framework.fetchItems
import hr.algebra.equationpocket.model.Item

const val ITEM_POS = "hr.algebra.equationpocket.item_position"
class ItemPagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityItemPagerBinding
    private lateinit var items: MutableList<Item>
    private var position = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityItemPagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()
        initPager()
    }

    private fun initPager() {
        binding.viewPager2.adapter = ItemPagerAdapter(this, items)
        binding.viewPager2.currentItem = position
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return super.onSupportNavigateUp()
    }

    private fun init() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        position = intent.getIntExtra(ITEM_POS, position)
        items = fetchItems()
    }
}