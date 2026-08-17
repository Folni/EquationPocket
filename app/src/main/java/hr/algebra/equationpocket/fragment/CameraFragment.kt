package hr.algebra.equationpocket.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import hr.algebra.equationpocket.databinding.FragmentCameraBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraExecutor: ExecutorService
    private var cameraControl: CameraControl? = null
    private var isFlashOn = false
    private var detectedFormula: String = ""

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(requireContext(), "Dozvola za kameru je nužna!", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.btnScan.setOnClickListener {
            if (detectedFormula.isNotBlank()) {
                val bundle = Bundle().apply {
                    putString("scanned_formula", detectedFormula)
                }
                parentFragmentManager.setFragmentResult("camera_result", bundle)
                findNavController().popBackStack()
            } else {
                Toast.makeText(requireContext(), "Ništa nije detektirano!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnFlash.setOnClickListener {
            isFlashOn = !isFlashOn
            cameraControl?.enableTorch(isFlashOn)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    viewLifecycleOwner, 
                    CameraSelector.DEFAULT_BACK_CAMERA, 
                    preview, 
                    imageAnalyzer
                )
                cameraControl = camera.cameraControl
                
                // Pinch to zoom podrška
                binding.previewView.setOnTouchListener { _, event ->
                    // Jednostavan zoom na tap za demo
                    if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                        cameraControl?.setLinearZoom(0.5f) // Zoom na 50%
                    }
                    true
                }

            } catch (exc: Exception) {
                Log.e("CameraFragment", "Binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    // ML Kit koordinate su u koordinatnom sustavu rotirane slike
                    // Za portretni mod (90 deg), X slike je zapravo Y na ekranu.
                    val width = imageProxy.width.toFloat()
                    val height = imageProxy.height.toFloat()

                    // ROI kalibracija (za 90 deg rotaciju)
                    // Na ekranu je overlay u sredini (visina oko 35% do 65%)
                    // To u rotiranoj slici ML Kita odgovara centralnom pojasu širine.
                    val roiMinX = width * 0.35f
                    val roiMaxX = width * 0.65f
                    val roiMinY = height * 0.25f
                    val roiMaxY = height * 0.75f

                    val result = visionText.textBlocks
                        .filter { block ->
                            val box = block.boundingBox
                            box != null && 
                            box.centerX() > roiMinX && box.centerX() < roiMaxX &&
                            box.centerY() > roiMinY && box.centerY() < roiMaxY
                        }
                        .flatMap { it.lines }
                        .map { it.text.replace(" ", "") }
                        .filter { it.contains("=") || it.contains("+") || it.contains("-") || it.contains("*") }
                        .maxByOrNull { it.length } ?: ""
                    
                    if (result.isNotBlank()) {
                        // Poboljšani post-processing za matematiku
                        detectedFormula = result
                            .replace(Regex("([a-zA-Z])2"), "$1^2") // x2 -> x^2
                            .replace(Regex("([a-zA-Z])3"), "$1^3") // x3 -> x^3
                            .replace("²", "^2")
                            .replace("³", "^3")
                            .replace("=", " = ")
                        
                        activity?.runOnUiThread {
                            binding.tvDetectedText.text = detectedFormula
                        }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }
}
