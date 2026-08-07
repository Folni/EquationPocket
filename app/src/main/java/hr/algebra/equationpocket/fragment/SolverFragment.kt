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
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import hr.algebra.equationpocket.R
import hr.algebra.equationpocket.databinding.FragmentSolverBinding
import hr.algebra.equationpocket.model.HistoryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matheclipse.core.eval.ExprEvaluator

class SolverFragment : Fragment() {

    private var _binding: FragmentSolverBinding? = null
    private val binding get() = _binding!!

    private val variableFields = mutableMapOf<String, EditText>()
    private var lastRawResult: String? = null
    private var isDecimalView = false
    private val calculationHistory = mutableListOf<HistoryEntry>()
    private var currentUnknownVar: String? = null

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
        
        // Sakrivamo zeleni gumb za izračun na tastaturi jer imamo ljubičasti gumb na ekranu
        binding.mathKeyboard.btnKeyboardCalculate.visibility = View.GONE
        
        binding.btnCalculate.setOnClickListener {
            calculateResult(formula)
        }
        
        binding.mathKeyboard.btnKeyboardCalculate.setOnClickListener {
            calculateResult(formula)
        }

        binding.btnDecimalToggle.setOnClickListener {
            toggleDecimalView()
        }

        binding.btnViewHistory.setOnClickListener {
            val bundle = Bundle().apply {
                putParcelableArrayList("history", ArrayList(calculationHistory))
            }
            findNavController().navigate(R.id.action_solver_to_history, bundle)
        }
    }

    private fun setupVariableFields(formula: String) {
        val reservedWords = setOf(
            "sin", "cos", "tan", "cot", "sec", "csc",
            "asin", "acos", "atan", "log", "ln", "exp",
            "sqrt", "lim", "D", "Sum", "Integrate", "int",
            "abs", "max", "min", "round", "floor", "ceil",
            "pi", "e", "inf", "infinity"
        )

        val variables = Regex("[a-zA-ZΔα-ωΑ-Ω]+").findAll(formula)
            .map { it.value }
            .distinct()
            .filter { it.isNotEmpty() && !reservedWords.contains(it.lowercase()) }

        // Dinamički računamo što treba maknuti (sve između naslova i gumba)
        val startIndex = binding.containerVariables.indexOfChild(binding.tvFormulaDisplay) + 1
        val endIndex = binding.containerVariables.indexOfChild(binding.layoutButtons)
        if (endIndex > startIndex) {
            binding.containerVariables.removeViews(startIndex, endIndex - startIndex)
        }
        variableFields.clear()

        variables.forEach { varName ->
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
            // Umećemo polje iznad layoutButtons
            val index = binding.containerVariables.indexOfChild(binding.layoutButtons)
            binding.containerVariables.addView(inputLayout, index)
            variableFields[varName] = editText
        }
    }

    private fun setupKeyboard() {
        val keys123 = listOf("1", "2", "3", "+", "-", "4", "5", "6", "*", "/", "7", "8", "9", "(", ")", "0", ".", "^", "=", "DEL")
        val keysAbc = ('a'..'z').map { it.toString() } + "DEL"
        val keysSymbols = listOf("Δ", "π", "α", "β", "γ", "θ", "λ", "e", "i", "φ", "Ω", "∞", "→", "!", "[", "]", "{", "}", ",", " ", "DEL")
        val keysAdvanced = listOf("D", "lim", "Sum", "∫", "√", "log", "ln", "sin", "cos", "tan", "cot", "sec", "csc", "asin", "acos", "atan", "floor", "ceil", "abs", "DEL")

        fun updateKeyboard(keys: List<String>) {
            binding.mathKeyboard.gridKeyboard.removeAllViews()
            binding.mathKeyboard.gridKeyboard.columnCount = if (keys.size > 20) 6 else 5
            
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
                    height = (48 * resources.displayMetrics.density).toInt()
                }
                
                binding.mathKeyboard.gridKeyboard.addView(button, params)
            }
        }

        updateKeyboard(keys123)

        binding.mathKeyboard.tabLayoutKeyboard.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> updateKeyboard(keys123)
                    1 -> updateKeyboard(keysAbc)
                    2 -> updateKeyboard(keysSymbols)
                    3 -> updateKeyboard(keysAdvanced)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun onKeyPress(key: String) {
        val focusedView = activity?.currentFocus as? EditText ?: return
        if (key == "DEL") {
            val start = focusedView.selectionStart
            if (start > 0) {
                focusedView.text.delete(start - 1, start)
            }
        } else if (key != "=") {
            focusedView.text.insert(focusedView.selectionStart, key)
        } else {
            // Ako je pritisnut "=", on u Solveru pokreće izračun
            val formula = arguments?.getString("formula") ?: "m * a = F"
            calculateResult(formula)
        }
    }

    private fun calculateResult(formula: String) {
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                var expressionToSolve = formula
                var unknownVar: String? = null
                val inputsMap = mutableMapOf<String, String>()
                
                variableFields.forEach { (name, field) ->
                    val value = field.text.toString()
                    if (value.isBlank()) {
                        if (unknownVar != null) {
                            showResult("Greška: Dozvoljena je samo jedna nepoznanica!")
                            return@launch
                        }
                        unknownVar = name
                    } else {
                        // Dopušten unos brojeva, znanstvenog zapisa (10^23) i matematičkih operatora
                        val validMathPattern = Regex("^[0-9.\\+\\-\\*/\\^\\(\\)\\sE]+$")
                        if (!value.matches(validMathPattern) && !value.contains(Regex("[a-zA-ZΔα-ωΑ-Ω]"))) {
                            showResult("Greška: Neispravan unos za '$name'!")
                            return@launch
                        }
                        inputsMap[name] = value
                        expressionToSolve = expressionToSolve.replace(Regex("\\b$name\\b"), "($value)")
                    }
                }

                // Logika resetiranja povijesti
                if (unknownVar != currentUnknownVar) {
                    calculationHistory.clear()
                    currentUnknownVar = unknownVar
                }

                // Provjera koriste li ulazni podaci znanstveni zapis
                val anyScientificInput = variableFields.values.any { 
                    it.text.toString().contains("10^") || it.text.toString().contains("E") 
                }

                val result = if (unknownVar == null) {
                    @Suppress("DEPRECATION")
                    mathEvaluator.evaluate(expressionToSolve.replace("=", "=="))
                } else {
                    val parts = expressionToSolve.split("=")
                    if (parts.size != 2) {
                        showResult("Greška: Jednadžba mora imati znak '='!")
                        return@launch
                    }
                    val solveExpr = "Solve(${parts[0]} == ${parts[1]}, $unknownVar)"
                    @Suppress("DEPRECATION")
                    mathEvaluator.evaluate(solveExpr)
                }

                var resultString = result.toString()
                
                when {
                    resultString.contains("Indeterminate") || 
                    resultString.contains("ComplexInfinity") || 
                    resultString.contains("Infinity") ||
                    resultString.contains("Solve(") -> {
                        showResult("Matematička greška (nedefinirano rješenje)!")
                    }
                    resultString == "{}" || resultString == "None" -> {
                        showResult("Nema rješenja u realnom skupu brojeva.")
                    }
                    else -> {
                        lastRawResult = resultString
                        isDecimalView = false
                        
                        // Ako je ulaz znanstveni ili je broj jako velik/mali, automatski prikaži u znanstvenom zapisu
                        if (anyScientificInput) {
                            val valueOnly = if (resultString.contains("->")) {
                                resultString.substringAfter("->").replace("}", "").trim()
                            } else {
                                resultString
                            }
                            // Provjeravamo je li rezultat broj prije konverzije
                            if (valueOnly.toDoubleOrNull() != null || valueOnly.contains("*10^")) {
                                @Suppress("DEPRECATION")
                                val decimalExpr = mathEvaluator.evaluate("N($valueOnly, 6)")
                                // Popravljeno: spremamo rezultat bez naziva varijable da formatDisplayResult ne napravi dupli ispis
                                lastRawResult = decimalExpr.toString()
                                isDecimalView = true
                            } else {
                                lastRawResult = resultString
                            }
                        } else {
                            lastRawResult = resultString
                        }

                        val displayResult = formatDisplayResult(lastRawResult!!, unknownVar)
                        
                        // Spremanje u povijest (ovdje koristimo već formatirani displayResult za konzistentnost)
                        if (unknownVar != null) {
                            val inputsString = inputsMap.entries.joinToString(", ") { "${it.key}=${it.value}" }
                            calculationHistory.add(0, HistoryEntry(inputsString, displayResult, unknownVar))
                        }

                        showResult(displayResult, showToggle = true, showHistory = calculationHistory.isNotEmpty())
                    }
                }
            } catch (e: Exception) {
                val errorMsg = when {
                    e.message?.contains("Syntax") == true -> "Greška u sintaksi jednadžbe!"
                    else -> "Greška: ${e.localizedMessage}"
                }
                showResult(errorMsg)
            }
        }
    }

    private fun formatDisplayResult(raw: String, unknownVar: String?): String {
        return if (unknownVar == null) {
            raw
        } else {
            val cleanResult = raw
                .replace("{", "")
                .replace("}", "")
                .replace("->", " = ")
                .replace("$unknownVar = ", "")
            "$unknownVar = $cleanResult"
        }
    }

    private fun toggleDecimalView() {
        val raw = lastRawResult ?: return
        isDecimalView = !isDecimalView
        
        val valueOnly = if (raw.contains("->")) {
            raw.substringAfter("->").replace("}", "").trim()
        } else {
            raw
        }

        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val finalValue = if (isDecimalView) {
                    @Suppress("DEPRECATION")
                    val decimalExpr = mathEvaluator.evaluate("N($valueOnly, 6)")
                    decimalExpr.toString()
                } else {
                    valueOnly
                }

                val unknownVar = variableFields.filter { it.value.text.isBlank() }.keys.firstOrNull()
                val displayText = if (unknownVar != null) "$unknownVar = $finalValue" else finalValue
                
                // Ažuriraj i povijest ako postoji zadnji izračun za tu varijablu
                if (calculationHistory.isNotEmpty() && calculationHistory[0].unknownVar == unknownVar) {
                    calculationHistory[0] = calculationHistory[0].copy(result = displayText)
                }

                withContext(Dispatchers.Main) {
                    binding.tvResult.text = displayText
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private suspend fun showResult(res: String, showToggle: Boolean = false, showHistory: Boolean = false) {
        withContext(Dispatchers.Main) {
            binding.tvResult.text = res
            binding.tvResult.visibility = View.VISIBLE
            
            binding.btnDecimalToggle.visibility = if (showToggle) View.VISIBLE else View.GONE
            binding.btnViewHistory.visibility = if (showHistory) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
