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
import com.github.mikephil.charting.formatter.ValueFormatter
import hr.algebra.equationpocket.databinding.FragmentGraphBinding
import hr.algebra.equationpocket.model.HistoryEntry
import kotlin.math.log10
import kotlin.math.pow

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
        
        val historyArray = arguments?.getParcelableArray("history")
        val history = historyArray?.filterIsInstance<HistoryEntry>() ?: return
        
        val xAxisVar = arguments?.getString("xAxisVar") ?: "x"
        val useLogScale = arguments?.getBoolean("useLogScale") ?: false
        
        setupGraph(history, xAxisVar, useLogScale)
    }

    private fun setupGraph(history: List<HistoryEntry>, xAxisVar: String, useLogScale: Boolean) {
        val entries = mutableListOf<Entry>()
        
        history.forEach { item ->
            val xVal = extractValue(item.inputs, xAxisVar)
            val yVal = parseSymjaValue(item.result.substringAfter("=").trim())
            
            if (xVal != null && yVal != null) {
                if (useLogScale) {
                    // Logaritamska transformacija: prikazujemo log10(val)
                    // Napomena: log je definiran samo za pozitivne brojeve
                    if (xVal > 0 && yVal > 0) {
                        entries.add(Entry(log10(xVal).toFloat(), log10(yVal).toFloat()))
                    }
                } else {
                    entries.add(Entry(xVal, yVal))
                }
            }
        }
        
        entries.sortBy { it.x }

        val dataSet = LineDataSet(entries, "Rezultat po varijabli $xAxisVar").apply {
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
            color = Color.parseColor("#4472C4")
            setCircleColor(Color.parseColor("#4472C4"))
            lineWidth = 2.5f
            circleRadius = 4f
            setDrawCircleHole(true)
            circleHoleColor = Color.WHITE
            circleHoleRadius = 2f
            setDrawValues(false)
            setDrawFilled(true)
            fillAlpha = 40
            fillColor = Color.parseColor("#4472C4")
        }

        binding.lineChart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBorders(false)
            setBackgroundColor(Color.WHITE)
            setExtraOffsets(10f, 10f, 10f, 20f)

            val scientificFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (useLogScale) {
                        // Pretvaramo log natrag u potenciju za prikaz (npr. 2 -> 10^2)
                        "10^${value.toInt()}"
                    } else if (value >= 10000 || (value > 0 && value < 0.01)) {
                        String.format("%.1E", value).replace("E+0", "E").replace("E+", "E")
                    } else {
                        super.getFormattedValue(value)
                    }
                }
            }

            xAxis.apply {
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(true)
                axisLineColor = Color.LTGRAY
                textColor = Color.DKGRAY
                textSize = 10f
                valueFormatter = scientificFormatter
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#D9D9D9")
                setDrawAxisLine(false)
                textColor = Color.DKGRAY
                textSize = 10f
                valueFormatter = scientificFormatter
            }

            axisRight.isEnabled = false
            legend.apply {
                isEnabled = true
                verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
                textSize = 12f
            }

            animateX(800)
            invalidate()
        }
    }

    private fun extractValue(inputs: String, varName: String): Float? {
        val raw = inputs.replace(" ", "").split(",").find { it.startsWith("$varName=") }?.substringAfter("=")
        return raw?.let { parseSymjaValue(it) }
    }

    private fun parseSymjaValue(value: String): Float? {
        // Podrška za Symja format: 5.*10^10, 1.2E5, 500
        return try {
            if (value.contains("*10^")) {
                val parts = value.split("*10^")
                val base = parts[0].replace(".", "").replace("*", "").toFloat()
                val exp = parts[1].toFloat()
                base * 10f.pow(exp)
            } else {
                value.replace("*", "").toFloatOrNull()
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}