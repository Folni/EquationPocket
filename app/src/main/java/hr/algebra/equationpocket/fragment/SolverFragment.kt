package hr.algebra.equationpocket.fragment

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import hr.algebra.equationpocket.databinding.FragmentSolverBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matheclipse.core.eval.ExprEvaluator

class SolverFragment : Fragment() {

    private var _binding: FragmentSolverBinding? = null
    private val binding get() = _binding!!

    private val variableFields = mutableMapOf<String, EditText>()
    private val mathEvaluator: ExprEvaluator by lazy {
        System.setProperty("log4j2.disable.jmx", "true")
        ExprEvaluator()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSolverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val formula = arguments?.getString("formula") ?: "m * a = F"
        binding.tvFormulaDisplay.text = formula
        
        setupVariableFields(formula)
        setupKeyboard()
        
        binding.btnCalculate.setOnClickListener {
            calculateResult(formula)
        }
        
        binding.mathKeyboard.btnKeyboardCalculate.setOnClickListener {
            calculateResult(formula)
        }
    }

    private fun setupVariableFields(formula: String) {
        val variables = Regex("[a-zA-ZΔα-ωΑ-Ω]+").findAll(formula)
            .map { it.value }
            .distinct()
            .filter { it.isNotEmpty() }

        binding.containerVariables.removeAllViews()
        variableFields.clear()

        binding.containerVariables.addView(binding.tvFormulaDisplay)

        variables.forEach { varName ->
            if (varName == "lim" || varName == "D" || varName == "Sum" || varName == "int") return@forEach

            val inputLayout = TextInputLayout(requireContext()).apply {
                hint = varName
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, 16)
                layoutParams = params
            }
            
            val editText = TextInputEditText(requireContext()).apply {
                inputType = InputType.TYPE_CLASS_TEXT
                showSoftInputOnFocus = false 
            }
            
            inputLayout.addView(editText)
            binding.containerVariables.addView(inputLayout)
            variableFields[varName] = editText
        }
        
        binding.containerVariables.addView(binding.btnCalculate)
        binding.containerVariables.addView(binding.tvResult)
    }

    private fun setupKeyboard() {
        val keys123 = listOf("1", "2", "3", "+", "-", "4", "5", "6", "*", "/", "7", "8", "9", "(", ")", "0", ".", "^", "=", "DEL")
        val keysAbc = listOf("x", "y", "z", "a", "b", "c", "m", "F", "Δ", "π", "α", "β", "γ", "θ", "λ", "e", "i", "φ", "Ω", "DEL")
        val keysAdvanced = listOf("D", "lim", "Sum", "∫", "√", "log", "ln", "sin", "cos", "tan", "∞", "→", "!", "[", "]", "{", "}", ",", " ", "DEL")

        updateKeyboard(keys123)

        binding.mathKeyboard.tabLayoutKeyboard.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> updateKeyboard(keys123)
                    1 -> updateKeyboard(keysAbc)
                    2 -> updateKeyboard(keysAdvanced)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun updateKeyboard(keys: List<String>) {
        binding.mathKeyboard.gridKeyboard.removeAllViews()
        keys.forEach { key ->
            val button = Button(requireContext()).apply {
                text = key
                setOnClickListener {
                    onKeyPress(key)
                }
            }
            
            val params = android.widget.GridLayout.LayoutParams(
                android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f),
                android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f)
            ).apply {
                width = 0
            }
            
            binding.mathKeyboard.gridKeyboard.addView(button, params)
        }
    }

    private fun onKeyPress(key: String) {
        val focusedView = activity?.currentFocus as? EditText ?: return
        if (key == "DEL") {
            val start = focusedView.selectionStart
            if (start > 0) {
                focusedView.text.delete(start - 1, start)
            }
        } else {
            focusedView.text.insert(focusedView.selectionStart, key)
        }
    }

    private fun calculateResult(formula: String) {
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                var expressionToSolve = formula
                var unknownVar: String? = null
                
                variableFields.forEach { (name, field) ->
                    val value = field.text.toString()
                    if (value.isBlank()) {
                        unknownVar = name
                    } else {
                        expressionToSolve = expressionToSolve.replace(Regex("\\b$name\\b"), "($value)")
                    }
                }

                if (unknownVar == null) {
                    @Suppress("DEPRECATION")
                    val result = mathEvaluator.evaluate(expressionToSolve.replace("=", "=="))
                    showResult(result.toString())
                } else {
                    val parts = expressionToSolve.split("=")
                    if (parts.size == 2) {
                        val solveExpr = "Solve(${parts[0]} == ${parts[1]}, $unknownVar)"
                        @Suppress("DEPRECATION")
                        val result = mathEvaluator.evaluate(solveExpr)
                        showResult("$unknownVar = $result")
                    }
                }
            } catch (e: Exception) {
                showResult("Greška: ${e.message}")
            }
        }
    }

    private suspend fun showResult(res: String) {
        withContext(Dispatchers.Main) {
            binding.tvResult.text = res
            binding.tvResult.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
