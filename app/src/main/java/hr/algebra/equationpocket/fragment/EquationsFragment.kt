package hr.algebra.equationpocket.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
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
                equationAdapter.updateData(equations)
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
        
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.equation_name)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.items) { _, _ ->
                val name = dialogBinding.etEquationName.text.toString()
                val formula = dialogBinding.etEquationFormula.text.toString()
                val desc = dialogBinding.etEquationDescription.text.toString()
                
                if (name.isNotBlank() && formula.isNotBlank()) {
                    saveEquation(Equation(
                        name = name,
                        formula = formula,
                        description = desc,
                        pocketId = pocketId
                    ))
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun saveEquation(equation: Equation) {
        lifecycleScope.launch {
            database.equationDao().insertEquation(equation)
        }
    }

    private fun showDeleteDialog(equation: Equation) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.exit) // Iskoristit ćemo postojeći string za naslov ili dodati novi
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
