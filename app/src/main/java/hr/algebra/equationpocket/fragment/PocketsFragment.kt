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
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import hr.algebra.equationpocket.R
import hr.algebra.equationpocket.adapter.PocketAdapter
import hr.algebra.equationpocket.dao.AppDatabase
import hr.algebra.equationpocket.databinding.FragmentPocketsBinding
import hr.algebra.equationpocket.model.Equation
import hr.algebra.equationpocket.model.Pocket
import kotlinx.coroutines.launch

class PocketsFragment : Fragment() {

    private var _binding: FragmentPocketsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var pocketAdapter: PocketAdapter
    private val database by lazy { AppDatabase.getDatabase(requireContext()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.pockets_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.miImportPocket -> {
                showImportPocketDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        pocketAdapter = PocketAdapter(
            emptyList(),
            onPocketClick = { pocket ->
                val bundle = Bundle().apply {
                    putLong("pocketId", pocket._id ?: -1L)
                    putString("pocketName", pocket.name)
                }
                findNavController().navigate(R.id.action_pockets_to_equations, bundle)
            },
            onPocketLongClick = { pocket ->
                showOptionsDialog(pocket)
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

    private fun showImportPocketDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "Zalijepite kod mape ovdje"
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Uvezi cijelu mapu")
            .setView(editText)
            .setPositiveButton("Uvezi") { _, _ ->
                val encoded = editText.text.toString().trim()
                if (encoded.isNotBlank()) {
                    importPocket(encoded)
                }
            }
            .setNegativeButton("Odustani", null)
            .show()
    }

    private fun importPocket(encoded: String) {
        lifecycleScope.launch {
            try {
                val decodedBytes = Base64.decode(encoded, Base64.DEFAULT)
                val decodedString = String(decodedBytes, Charsets.UTF_8)
                
                if (decodedString.startsWith("POCKET:")) {
                    val parts = decodedString.substring(7).split("::")
                    if (parts.isNotEmpty()) {
                        val pocketName = parts[0]
                        val pocketId = database.equationDao().insertPocket(Pocket(name = pocketName))
                        
                        // Ostali dijelovi su jednadžbe u formatu "Naziv|Formula"
                        for (i in 1 until parts.size) {
                            val eqParts = parts[i].split("|")
                            if (eqParts.size == 2) {
                                database.equationDao().insertEquation(Equation(
                                    name = eqParts[0],
                                    formula = eqParts[1],
                                    pocketId = pocketId
                                ))
                            }
                        }
                        Toast.makeText(requireContext(), "Mapa '$pocketName' uspješno uvezena!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Neispravan kôd mape!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Greška pri uvozu mape!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun savePocket(pocket: Pocket) {
        lifecycleScope.launch {
            database.equationDao().insertPocket(pocket)
        }
    }

    private fun showOptionsDialog(pocket: Pocket) {
        val options = arrayOf("Kopiraj cijelu mapu", "Obriši")
        AlertDialog.Builder(requireContext())
            .setTitle(pocket.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> copyPocketToClipboard(pocket)
                    1 -> confirmDelete(pocket)
                }
            }
            .show()
    }

    private fun copyPocketToClipboard(pocket: Pocket) {
        lifecycleScope.launch {
            val equations = database.equationDao().getEquationsForPocketList(pocket._id ?: -1L)
            
            // Format: POCKET:PocketName::Eq1Name|Eq1Formula::Eq2Name|Eq2Formula...
            val sb = StringBuilder("POCKET:")
            sb.append(pocket.name)
            
            equations.forEach { eq ->
                sb.append("::").append(eq.name).append("|").append(eq.formula)
            }
            
            val encoded = Base64.encodeToString(sb.toString().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("EquationPocket Folder", encoded)
            clipboard.setPrimaryClip(clip)
            
            Toast.makeText(requireContext(), "Kôd mape kopiran!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDelete(pocket: Pocket) {
        AlertDialog.Builder(requireContext())
            .setTitle("Obriši mapu")
            .setMessage("Želite li zaista obrisati mapu '${pocket.name}'?")
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
