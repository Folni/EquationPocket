package hr.algebra.equationpocket.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import hr.algebra.equationpocket.R
import hr.algebra.equationpocket.adapter.HistoryAdapter
import hr.algebra.equationpocket.databinding.DialogGraphConfigBinding
import hr.algebra.equationpocket.databinding.FragmentHistoryBinding
import hr.algebra.equationpocket.model.HistoryEntry

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val historyList = arguments?.getParcelableArrayList<HistoryEntry>("history") ?: emptyList<HistoryEntry>()
        
        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = HistoryAdapter(historyList)
        }

        if (historyList.isNotEmpty() && historyList.size >= 2) {
            binding.btnPlotGraph.visibility = View.VISIBLE
            binding.btnPlotGraph.setOnClickListener {
                showGraphConfigDialog(ArrayList(historyList))
            }
        }
    }

    private fun showGraphConfigDialog(history: ArrayList<HistoryEntry>) {
        val dialogBinding = DialogGraphConfigBinding.inflate(layoutInflater)
        
        // Ekstrahiramo nazive varijabli iz prve stavke povijesti (npr. "m=10, a=5")
        val firstEntryInputs = history.firstOrNull()?.inputs ?: ""
        val variables = firstEntryInputs.split(", ").map { it.substringBefore("=") }
        
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, variables)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerXAxis.adapter = adapter

        AlertDialog.Builder(requireContext())
            .setTitle("Konfiguracija grafa")
            .setView(dialogBinding.root)
            .setPositiveButton("Iscrtaj") { _, _ ->
                val selectedVar = dialogBinding.spinnerXAxis.selectedItem.toString()
                val bundle = Bundle().apply {
                    // Navigacija očekuje niz (Array), pa pretvaramo listu u niz
                    putParcelableArray("history", history.toTypedArray())
                    putString("xAxisVar", selectedVar)
                }
                findNavController().navigate(R.id.action_history_to_graph, bundle)
            }
            .setNegativeButton("Odustani", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
