package hr.algebra.equationpocket.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import hr.algebra.equationpocket.R
import hr.algebra.equationpocket.databinding.DialogTutorialBinding
import hr.algebra.equationpocket.databinding.FragmentTutorialTriggerBinding

class TutorialFragment : Fragment() {

    private var _binding: FragmentTutorialTriggerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTutorialTriggerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnShowTutorial.setOnClickListener {
            showTutorialDialog()
        }
    }

    private fun showTutorialDialog() {
        val dialogBinding = DialogTutorialBinding.inflate(layoutInflater)
        val tutorialImages = listOf(R.drawable.tutorial_1, R.drawable.tutorial_2, R.drawable.tutorial_3)
        var currentStep = 0

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        fun updateUI() {
            dialogBinding.ivTutorial.setImageResource(tutorialImages[currentStep])
            dialogBinding.btnTutorialPrev.visibility = if (currentStep == 0) View.INVISIBLE else View.VISIBLE
            dialogBinding.btnTutorialNext.text = if (currentStep == tutorialImages.size - 1) "Završi" else "Dalje"
        }

        dialogBinding.btnTutorialNext.setOnClickListener {
            if (currentStep < tutorialImages.size - 1) {
                currentStep++
                updateUI()
            } else {
                dialog.dismiss()
            }
        }

        dialogBinding.btnTutorialPrev.setOnClickListener {
            if (currentStep > 0) {
                currentStep--
                updateUI()
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}