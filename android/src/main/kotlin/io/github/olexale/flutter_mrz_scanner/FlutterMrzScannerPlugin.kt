package io.github.olexale.flutter_mrz_scanner

import android.content.Context
import androidx.core.content.ContextCompat
import android.view.View
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory

import io.github.olexale.flutter_mrz_scanner.R
import io.github.olexale.flutter_mrz_scanner.mlkit.camera.CameraSource
import io.github.olexale.flutter_mrz_scanner.mlkit.camera.CameraSourcePreview
import io.github.olexale.flutter_mrz_scanner.mlkit.other.GraphicOverlay
import io.github.olexale.flutter_mrz_scanner.mlkit.text.TextRecognitionProcessor
import io.github.olexale.flutter_mrz_scanner.model.DocType
import android.util.Log
import org.jmrtd.lds.icao.MRZInfo

import android.view.LayoutInflater
import java.io.IOException


class FlutterMrzScannerPlugin : FlutterPlugin {

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        flutterPluginBinding.applicationContext
        flutterPluginBinding.platformViewRegistry.registerViewFactory("mrzscanner", MRZScannerFactory(flutterPluginBinding))
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {}
}

class MRZScannerFactory(private val flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {
    override fun create(context: Context?, id: Int, o: Any?): PlatformView {

        val ctx = if (context != null) context else flutterPluginBinding.applicationContext;

        return MRZScannerView(ctx, flutterPluginBinding.binaryMessenger, id)
    }
}

class MRZScannerView internal constructor(context: Context, messenger: BinaryMessenger, id: Int) : PlatformView, MethodChannel.MethodCallHandler, TextRecognitionProcessor.ResultListener {
    private val methodChannel: MethodChannel = MethodChannel(messenger, "mrzscanner_$id")
    
    private var context: Context = context
    private var mainExecutor = ContextCompat.getMainExecutor(context)
    lateinit private var cameraSource: CameraSource
    lateinit private var preview: CameraSourcePreview
    lateinit private var graphicOverlay: GraphicOverlay

    val MRZ_RESULT = "MRZ_RESULT"
    val DOC_TYPE = "DOC_TYPE"

    private val docType: DocType = DocType.PASSPORT
    private val TAG: String = "MRZScannerView.TPS"

    private var view: View? = null    

    override fun getView(): View {
        view = LayoutInflater.from(context).inflate(R.layout.capture, null)

        preview = view!!.findViewById(R.id.camera_source_preview)
        if (preview == null) {
            Log.d(TAG, "Preview is null")
        }
        graphicOverlay = view!!.findViewById(R.id.graphics_overlay)
        if (graphicOverlay == null) {
            Log.d(TAG, "graphicOverlay is null")
        }

        createCameraSource()
        startCameraSource()

        return view!!
    }

    init {
        methodChannel.setMethodCallHandler(this)
    }

    private fun createCameraSource() {
        
        cameraSource = CameraSource(context, graphicOverlay)
        cameraSource.setFacing(CameraSource.CAMERA_FACING_BACK)
        cameraSource.setMachineLearningFrameProcessor(TextRecognitionProcessor(docType, this))
    }

    private fun startCameraSource() {
        preview.start(cameraSource, graphicOverlay)
    }

    override fun onSuccess(mrzInfo: MRZInfo?) {
        Log.d(TAG, "mrzInfo")
        Log.d(TAG, mrzInfo!!.toString())
        Log.d(TAG, mrzInfo.dateOfExpiry)
        Log.d(TAG, mrzInfo.optionalData2)
        cameraSource.stop()
        cameraSource.release()
        mainExecutor.execute {
            methodChannel.invokeMethod("onParsed", mrzInfo!!.toString())
        }
    }

    override fun onError(exp: java.lang.Exception?) {
        Log.d(TAG, "exp")
    }

    override fun dispose() {
        cameraSource.release()
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: MethodChannel.Result) {
        when (call.method) {
            "start" -> {
                result.success(null)
            }
            "stop" -> {
                result.success(null)
            }
            "flashlightOn" -> {
                result.success(null)
            }
            "flashlightOff" -> {
                result.success(null)
            }
            "takePhoto" -> {
                result.success(null)
            }
            else -> {
                result.notImplemented()
            }
        }
    }
}
