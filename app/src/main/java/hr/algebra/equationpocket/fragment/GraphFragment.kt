package hr.algebra.equationpocket.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import hr.algebra.equationpocket.databinding.FragmentGraphBinding
import hr.algebra.equationpocket.model.HistoryEntry

class GraphFragment : Fragment() {

    private var _binding: FragmentGraphBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGraphBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Dohvaćamo niz iz argumenata
        val historyArray = arguments?.getParcelableArray("history")
        val history = historyArray?.filterIsInstance<HistoryEntry>() ?: return

        val xAxisVar = arguments?.getString("xAxisVar") ?: "x"
        
        setupGraph(history, xAxisVar)
    }

    private fun setupGraph(history: List<HistoryEntry>, xAxisVar: String) {
        val entries = mutableListOf<Entry>()
        
        history.forEach { item ->
            // Parsiramo vrijednost X varijable iz stringa "var1=val1, var2=val2"
            val xVal = extractValue(item.inputs, xAxisVar)
            // Parsiramo rezultat (Y os) iz stringa "var = val"
            val yVal = item.result.substringAfter("=").trim().toFloatOrNull()
            
            if (xVal != null && yVal != null) {
                entries.add(Entry(xVal, yVal))
            }
        }
        
        // Sortiramo po X osi za ispravan LineChart
        entries.sortBy { it.x }

        val dataSet = LineDataSet(entries, "Rezultat po varijabli $xAxisVar").apply {
            color = Color.BLUE
            setCircleColor(Color.RED)
            lineWidth = 2f
            circleRadius = 4f
            setDrawValues(true)
            valueTextSize = 10f
        }

        binding.lineChart.apply {
            data = LineData(dataSet)
            description.text = "Ovisnost rezultata o $xAxisVar"
            animateX(1000)
            invalidate()
        }
    }

    private fun extractValue(inputs: String, varName: String): Float? {
        val pattern = Regex("$varName=([0-9.-]+)")
        return pattern.find(inputs.replace(" ", ""))?.groupValues?.get(1)?.toFloatOrNull()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}