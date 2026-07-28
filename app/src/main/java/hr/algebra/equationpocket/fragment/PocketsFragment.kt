package hr.algebra.equationpocket.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import hr.algebra.equationpocket.R
import hr.algebra.equationpocket.adapter.PocketAdapter
import hr.algebra.equationpocket.dao.AppDatabase
import hr.algebra.equationpocket.databinding.FragmentPocketsBinding
import hr.algebra.equationpocket.model.Pocket
import kotlinx.coroutines.launch

class PocketsFragment : Fragment() {

    private var _binding: FragmentPocketsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var pocketAdapter: PocketAdapter
    private val database by lazy { AppDatabase.getDatabase(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPocketsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observePockets()
        setupListeners()
    }

    private fun setupRecyclerView() {
        pocketAdapter = PocketAdapter(
            emptyList(),
            onPocketClick = { pocket ->
                // Ovdje ćemo kasnije otvoriti listu jednadžbi za ovu mapu
            },
            onPocketLongClick = { pocket ->
                showDeleteDialog(pocket)
            }
        )
        binding.rvPockets.apply {
            adapter = pocketAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observePockets() {
        lifecycleScope.launch {
            database.equationDao().getAllPockets().collect { pockets ->
                pocketAdapter.updateData(pockets)
            }
        }
    }

    private fun setupListeners() {
        binding.fabAddPocket.setOnClickListener {
            showAddPocketDialog()
        }
    }

    private fun showAddPocketDialog() {
        val editText = EditText(requireContext())
        AlertDialog.Builder(requireContext())
            .setTitle("Nova mapa")
            .setMessage("Unesite naziv mape (npr. Fizika 1):")
            .setView(editText)
            .setPositiveButton("Dodaj") { _, _ ->
                val name = editText.text.toString()
                if (name.isNotBlank()) {
                    savePocket(Pocket(name = name))
                }
            }
            .setNegativeButton("Odustani", null)
            .show()
    }

    private fun savePocket(pocket: Pocket) {
        lifecycleScope.launch {
            database.equationDao().insertPocket(pocket)
        }
    }

    private fun showDeleteDialog(pocket: Pocket) {
        AlertDialog.Builder(requireContext())
            .setTitle("Obriši mapu")
            .setMessage("Želite li zaista obrisati mapu '${pocket.name}' i sve jednadžbe u njoj?")
            .setPositiveButton("Obriši") { _, _ ->
                lifecycleScope.launch {
                    database.equationDao().deletePocket(pocket)
                }
            }
            .setNegativeButton("Odustani", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
