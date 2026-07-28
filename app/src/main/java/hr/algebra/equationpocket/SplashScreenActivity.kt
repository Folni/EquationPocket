package hr.algebra.equationpocket

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import hr.algebra.equationpocket.databinding.ActivitySplashScreenBinding
import hr.algebra.equationpocket.framework.applyAnimation
import hr.algebra.equationpocket.framework.callDelayed
import hr.algebra.equationpocket.framework.startActivity

private const val DELAY = 3000L

class SplashScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startAnimations()
        redirect()
    }

    private fun startAnimations() {
        binding.tvSplash.applyAnimation(R.anim.blink)
        binding.ivSplash.applyAnimation(R.anim.rotate)
    }

    private fun redirect() {
        callDelayed(DELAY) { startActivity<HostActivity>() }
    }
}
