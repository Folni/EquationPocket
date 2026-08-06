package hr.algebra.equationpocket.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import hr.algebra.equationpocket.R
import hr.algebra.equationpocket.adapter.EquationAdapter
import hr.algebra.equationpocket.dao.AppDatabase
import hr.algebra.equationpocket.databinding.DialogAddEquationBinding
import hr.algebra.equationpocket.databinding.FragmentEquationsBinding
import hr.algebra.equationpocket.model.Equation
import kotlinx.coroutines.launch

class EquationsFragment : Fragment() {

    private var _binding: FragmentEquationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var equationAdapter: EquationAdapter
    private val database by lazy { AppDatabase.getDatabase(requireContext()) }
    
    private var pocketId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pocketId = arguments?.getLong("pocketId") ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEquationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeEquations()
        setupListeners()
    }

    private fun setupRecyclerView() {
        equationAdapter = EquationAdapter(
            emptyList(),
            onEquationClick = { equation ->
                val bundle = Bundle().apply {
                    putString("formula", equation.formula)
                }
                findNavController().navigate(R.id.action_equations_to_solver, bundle)
            },
            onEquationLongClick = { equation ->
                showDeleteDialog(equation)
            }
        )
        binding.rvEquations.apply {
            adapter = equationAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeEquations() {
        lifecycleScope.launch {
            database.equationDao().getEquationsForPocket(pocketId).collect { equations ->
                // Sortiranje: Favorit ide na vrh
                val sortedList = equations.sortedByDescending { it.isFavorite }
                equationAdapter.updateData(sortedList)
            }
        }
    }

    private fun setupListeners() {
        binding.fabAddEquation.setOnClickListener {
            showAddEquationDialog()
        }
    }

    private fun showAddEquationDialog() {
        val dialogBinding = DialogAddEquationBinding.inflate(layoutInflater)
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        
        dialogBinding.keyboardContainerDialog.visibility = View.GONE
        
        dialogBinding.etEquationFormula.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                dialogBinding.etEquationFormula.showSoftInputOnFocus = false
                dialogBinding.keyboardContainerDialog.visibility = View.VISIBLE
            } else {
                dialogBinding.keyboardContainerDialog.visibility = View.GONE
            }
        }
        
        val textFields = listOf(dialogBinding.etEquationName)
        textFields.forEach { field ->
            field.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    dialogBinding.keyboardContainerDialog.visibility = View.GONE
                    field.showSoftInputOnFocus = true
                    imm.showSoftInput(v, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                }
            }
        }

        setupDialogKeyboard(dialogBinding)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.equation_name)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.items) { _, _ ->
                val name = dialogBinding.etEquationName.text.toString()
                val formula = dialogBinding.etEquationFormula.text.toString()
                
                if (name.isNotBlank() && formula.isNotBlank()) {
                    saveEquation(Equation(
                        name = name,
                        formula = formula,
                        pocketId = pocketId
                    ))
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun setupDialogKeyboard(dialogBinding: DialogAddEquationBinding) {
        val keys123 = listOf("1", "2", "3", "+", "-", "4", "5", "6", "*", "/", "7", "8", "9", "(", ")", "0", ".", "^", "=", "DEL")
        val keysAbc = ('a'..'z').map { it.toString() } + "DEL"
        val keysSymbols = listOf("Δ", "π", "α", "β", "γ", "θ", "λ", "e", "i", "φ", "Ω", "∞", "→", "!", "[", "]", "{", "}", ",", " ", "DEL")
        val keysAdvanced = listOf("D", "lim", "Sum", "∫", "√", "log", "ln", "sin", "cos", "tan", "cot", "sec", "csc", "asin", "acos", "atan", "floor", "ceil", "abs", "DEL")

        fun updateKeyboard(keys: List<String>) {
            dialogBinding.mathKeyboardDialog.gridKeyboard.removeAllViews()
            // Prilagođavamo broj stupaca ovisno o količini tipki za bolji prikaz
            dialogBinding.mathKeyboardDialog.gridKeyboard.columnCount = if (keys.size > 20) 6 else 5
            
            keys.forEach { key ->
                val button = Button(requireContext()).apply {
                    text = key
                    setOnClickListener {
                        val focusedView = dialogBinding.etEquationFormula
                        val editable = focusedView.text
                        if (key == "DEL") {
                            val start = focusedView.selectionStart
                            if (start > 0 && editable != null) {
                                editable.delete(start - 1, start)
                            }
                        } else if (editable != null) {
                            editable.insert(focusedView.selectionStart, key)
                        }
                    }
                }
                val params = android.widget.GridLayout.LayoutParams(
                    android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f),
                    android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f)
                ).apply { 
                    width = 0 
                    height = (48 * resources.displayMetrics.density).toInt()
                }
                dialogBinding.mathKeyboardDialog.gridKeyboard.addView(button, params)
            }
        }

        updateKeyboard(keys123)
        dialogBinding.mathKeyboardDialog.btnKeyboardCalculate.visibility = View.GONE

        dialogBinding.mathKeyboardDialog.tabLayoutKeyboard.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
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

    private fun saveEquation(equation: Equation) {
        lifecycleScope.launch {
            database.equationDao().insertEquation(equation)
        }
    }

    private fun showDeleteDialog(equation: Equation) {
        val options = arrayOf("Uredi", "Postavi kao favorit", "Obriši")
        
        AlertDialog.Builder(requireContext())
            .setTitle(equation.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditEquationDialog(equation)
                    1 -> setAsFavorite(equation)
                    2 -> confirmDelete(equation)
                }
            }
            .show()
    }

    private fun showEditEquationDialog(equation: Equation) {
        val dialogBinding = DialogAddEquationBinding.inflate(layoutInflater)
        dialogBinding.etEquationName.setText(equation.name)
        dialogBinding.etEquationFormula.setText(equation.formula)
        
        AlertDialog.Builder(requireContext())
            .setTitle("Uredi jednadžbu")
            .setView(dialogBinding.root)
            .setPositiveButton("Spremi") { _, _ ->
                val newName = dialogBinding.etEquationName.text.toString()
                val newFormula = dialogBinding.etEquationFormula.text.toString()
                if (newName.isNotBlank() && newFormula.isNotBlank()) {
                    val updated = equation.copy(name = newName, formula = newFormula)
                    lifecycleScope.launch {
                        database.equationDao().updateEquation(updated)
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun setAsFavorite(equation: Equation) {
        lifecycleScope.launch {
            // Prvo očisti sve favorite u ovoj mapi
            database.equationDao().clearFavoritesForPocket(pocketId)
            // Postavi novu jednadžbu kao jedini favorit
            val updated = equation.copy(isFavorite = true)
            database.equationDao().updateEquation(updated)
        }
    }

    private fun confirmDelete(equation: Equation) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.exit)
            .setMessage("Želite li obrisati jednadžbu '${equation.name}'?")
            .setPositiveButton("Obriši") { _, _ ->
                lifecycleScope.launch {
                    database.equationDao().deleteEquation(equation)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
