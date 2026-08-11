package hr.algebra.equationpocket.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
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
    private var isCapsLock = false
    
    private var pocketId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
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
        setupFragmentResultListeners()
    }

    private fun setupFragmentResultListeners() {
        parentFragmentManager.setFragmentResultListener("camera_result", viewLifecycleOwner) { _, bundle ->
            val scannedFormula = bundle.getString("scanned_formula")
            if (!scannedFormula.isNullOrBlank()) {
                showAddEquationDialog(prefilledFormula = scannedFormula)
            }
        }
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

    private fun showAddEquationDialog(prefilledFormula: String? = null) {
        val dialogBinding = DialogAddEquationBinding.inflate(layoutInflater)
        
        if (!prefilledFormula.isNullOrBlank()) {
            dialogBinding.etEquationFormula.setText(prefilledFormula)
        }

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
        val keysAbc = ('a'..'z').map { it.toString() } + listOf("CAPS", "DEL")
        val keysSymbols = listOf("Δ", "π", "α", "β", "γ", "δ", "ε", "ζ", "η", "θ", "λ", "μ", "ν", "ξ", "ρ", "σ", "τ", "φ", "χ", "ψ", "ω", "e", "i", "Ω", "∞", "→", "!", "[", "]", "{", "}", ",", " ", "DEL")
        val keysAdvanced = listOf("D", "lim", "Sum", "∫", "√", "log", "ln", "sin", "cos", "tan", "cot", "sec", "csc", "asin", "acos", "atan", "floor", "ceil", "abs", "DEL")

        fun updateKeyboard(keys: List<String>) {
            dialogBinding.mathKeyboardDialog.gridKeyboard.removeAllViews()
            // Prilagođavamo broj stupaca ovisno o količini tipki za bolji prikaz
            dialogBinding.mathKeyboardDialog.gridKeyboard.columnCount = if (keys.size > 20) 6 else 5
            
            keys.forEach { key ->
                val displayKey = if (isCapsLock && key.length == 1 && key[0].isLowerCase()) key.uppercase() else key
                val button = Button(requireContext()).apply {
                    text = displayKey
                    if (key == "CAPS" && isCapsLock) {
                        setBackgroundColor(android.graphics.Color.parseColor("#3700B3"))
                        setTextColor(android.graphics.Color.WHITE)
                    }
                    setOnClickListener {
                        if (key == "CAPS") {
                            isCapsLock = !isCapsLock
                            updateKeyboard(keysAbc)
                        } else {
                            val focusedView = dialogBinding.etEquationFormula
                            val editable = focusedView.text
                            if (key == "DEL") {
                                val start = focusedView.selectionStart
                                if (start > 0 && editable != null) {
                                    editable.delete(start - 1, start)
                                }
                            } else if (editable != null) {
                                editable.insert(focusedView.selectionStart, displayKey)
                            }
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.equations_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.miImport -> {
                showImportDialog()
                true
            }
            R.id.miCamera -> {
                findNavController().navigate(R.id.action_equations_to_camera)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showImportDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "Zalijepite kod ovdje"
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Uvezi jednadžbu")
            .setView(editText)
            .setPositiveButton("Uvezi") { _, _ ->
                val encoded = editText.text.toString().trim()
                if (encoded.isNotBlank()) {
                    importEquation(encoded)
                }
            }
            .setNegativeButton("Odustani", null)
            .show()
    }

    private fun importEquation(encoded: String) {
        try {
            val decodedBytes = Base64.decode(encoded, Base64.DEFAULT)
            val decodedString = String(decodedBytes, Charsets.UTF_8)
            val parts = decodedString.split("|")
            if (parts.size == 2) {
                val name = parts[0]
                val formula = parts[1]
                saveEquation(Equation(name = name, formula = formula, pocketId = pocketId))
                Toast.makeText(requireContext(), "Uspješno uvezeno!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Neispravan kod!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Greška pri dekodiranju!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteDialog(equation: Equation) {
        val options = arrayOf("Uredi", "Postavi kao favorit", "Kopiraj za dijeljenje", "Obriši")
        
        AlertDialog.Builder(requireContext())
            .setTitle(equation.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditEquationDialog(equation)
                    1 -> setAsFavorite(equation)
                    2 -> copyToClipboard(equation)
                    3 -> confirmDelete(equation)
                }
            }
            .show()
    }

    private fun copyToClipboard(equation: Equation) {
        val rawString = "${equation.name}|${equation.formula}"
        val encoded = Base64.encodeToString(rawString.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("EquationPocket Code", encoded)
        clipboard.setPrimaryClip(clip)
        
        Toast.makeText(requireContext(), "Kôd kopiran u međuspremnik!", Toast.LENGTH_SHORT).show()
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
