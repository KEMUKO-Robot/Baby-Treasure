package com.kmq.kmqsamplebody.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.Image
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ainirobot.coreservice.client.Definition
import com.ainirobot.coreservice.client.RobotApi
import com.ainirobot.coreservice.client.listener.CommandListener
import com.ainirobot.coreservice.client.listener.Person
import com.ainirobot.coreservice.client.person.PersonApi
import com.ainirobot.coreservice.client.person.PersonListener
import com.ainirobot.coreservice.client.surfaceshare.SurfaceShareApi
import com.ainirobot.coreservice.client.surfaceshare.SurfaceShareBean
import com.ainirobot.coreservice.client.surfaceshare.SurfaceShareListener
import com.kmq.kmqsamplebody.R
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer

/**
 * 摄像头 & 人脸识别页面
 *
 * 包含三个模块：
 *
 * 【模块一：Android Camera2 原生摄像头】
 * - 使用 Android Camera2 API 打开摄像头并在 SurfaceView 上显示预览
 * - 支持前后摄像头切换（通过 CameraManager.getCameraIdList 枚举）
 * - 注意：使用 Camera2 打开摄像头期间，机器人视觉能力（人脸检测等）暂不可用，
 *   释放后一段时间可恢复
 * - 横屏机器人首次打开可能需要旋转 270° 纠正方向
 * - 首次授权 APP 使用摄像头时可能崩溃，之后正常
 *
 * 【模块二：摄像头数据流共享（SurfaceShare）】
 * - 通过 SurfaceShareApi 获取 VisionSDK 的摄像头共享数据流
 * - 不占用摄像头，不影响机器人视觉能力
 * - 使用 ImageReader 接收 YUV_420_888 数据，转换为 Bitmap 在 ImageView 上显示
 * - API:
 *   SurfaceShareApi.getInstance().requestImageFrame(surface, bean, listener) — 开始
 *   SurfaceShareApi.getInstance().abandonImageFrame(bean) — 停止
 *
 * 【模块三：人脸识别】
 * - 基于 VisionSDK 的人员检测（本地能力）与人脸识别（需联网）
 * - PersonApi.getInstance().registerPersonListener(listener) — 注册人员变化监听
 * - PersonApi.getInstance().getAllPersons() — 获取当前视野内所有人员
 * - PersonApi.getInstance().getAllFaceList() — 获取有人脸信息的人员
 * - PersonApi.getInstance().getCompleteFaceList() — 获取有完整人脸的人员
 * - RobotApi.getInstance().switchCamera(reqId, camera, listener) — 切换视觉SDK摄像头
 *   camera: Definition.JSON_HEAD_FORWARD(前置) / Definition.JSON_HEAD_BACKWARD(后置)
 * - 注意：不可同时使用 Camera2 和视觉能力，否则视觉能力报错失效
 */
class CameraActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CameraActivity"
        private const val REQUEST_CAMERA_PERMISSION = 100
        private const val SHARE_IMAGE_WIDTH = 640
        private const val SHARE_IMAGE_HEIGHT = 480
        private const val MAX_CACHE_IMAGES = 4
    }

    private var reqId = 0

    // ==================== Camera2 ====================
    private lateinit var cameraManager: CameraManager
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var cameraThread: HandlerThread? = null
    private var cameraHandler: Handler? = null
    private var isFrontCamera = true
    private lateinit var surfaceView: SurfaceView
    private var surfaceReady = false
    private lateinit var tvCamera2Status: TextView

    // ==================== SurfaceShare ====================
    private var shareImageReader: ImageReader? = null
    private var shareThread: HandlerThread? = null
    private var shareHandler: Handler? = null
    private var surfaceShareBean: SurfaceShareBean? = null
    private var isSharing = false
    private lateinit var ivSurfaceShare: ImageView
    private lateinit var tvShareStatus: TextView

    // ==================== 人脸识别 ====================
    private var personListener: PersonListener? = null
    private var isDetecting = false
    private var isRecognizingFaceFromNet = false
    private var facePreviewImageReader: ImageReader? = null
    private var facePreviewThread: HandlerThread? = null
    private var facePreviewHandler: Handler? = null
    private var facePreviewBean: SurfaceShareBean? = null
    private var isFacePreviewing = false
    private var facePreviewFrameCount = 0
    private val facePollingHandler = Handler(Looper.getMainLooper())
    private var facePollingRunnable: Runnable? = null
    private lateinit var ivFacePreview: ImageView
    private lateinit var tvFaceInfo: TextView
    private lateinit var tvFaceNetResult: TextView
    private lateinit var tvFaceStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager

        findViewById<Button>(R.id.btn_back_home).setOnClickListener { finish() }

        initCamera2Module()
        initSurfaceShareModule()
        initFaceModule()
    }

    // ================================================================
    //  模块一：Camera2 原生摄像头
    // ================================================================

    private fun initCamera2Module() {
        surfaceView = findViewById(R.id.surface_camera_preview)
        tvCamera2Status = findViewById(R.id.tv_camera2_status)
        val btnOpen = findViewById<Button>(R.id.btn_camera_open)
        val btnClose = findViewById<Button>(R.id.btn_camera_close)
        val btnSwitch = findViewById<Button>(R.id.btn_camera_switch)

        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                surfaceReady = true
                tvCamera2Status.text = "预览表面就绪，可点击「打开摄像头」"
            }
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                surfaceReady = false
                closeCamera2()
            }
        })

        btnOpen.setOnClickListener {
            if (checkCameraPermission()) {
                openCamera2()
            }
        }

        btnClose.setOnClickListener {
            closeCamera2()
            tvCamera2Status.text = "摄像头已关闭"
        }

        btnSwitch.setOnClickListener {
            isFrontCamera = !isFrontCamera
            if (cameraDevice != null) {
                closeCamera2()
                openCamera2()
            } else {
                tvCamera2Status.text = "已切换到${if (isFrontCamera) "前置" else "后置"}，点击「打开摄像头」预览"
            }
        }
    }

    private fun checkCameraPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION
            )
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera2()
            } else {
                tvCamera2Status.text = "摄像头权限被拒绝"
            }
        }
    }

    private fun openCamera2() {
        if (!surfaceReady) {
            tvCamera2Status.text = "预览表面尚未就绪"
            return
        }

        startCameraThread()

        val cameraId = findCameraId(isFrontCamera)
        if (cameraId == null) {
            tvCamera2Status.text = "未找到${if (isFrontCamera) "前置" else "后置"}摄像头"
            return
        }

        tvCamera2Status.text = "正在打开${if (isFrontCamera) "前置" else "后置"}摄像头..."

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) return

            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createPreviewSession()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                    runOnUiThread { tvCamera2Status.text = "摄像头已断开" }
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cameraDevice = null
                    runOnUiThread { tvCamera2Status.text = "摄像头错误: $error" }
                }
            }, cameraHandler)
        } catch (e: Exception) {
            tvCamera2Status.text = "打开失败: ${e.message}"
            Log.e(TAG, "openCamera2 error", e)
        }
    }

    private fun findCameraId(front: Boolean): String? {
        val facing = if (front) CameraCharacteristics.LENS_FACING_FRONT
        else CameraCharacteristics.LENS_FACING_BACK
        for (id in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(id)
            if (characteristics.get(CameraCharacteristics.LENS_FACING) == facing) {
                return id
            }
        }
        return cameraManager.cameraIdList.firstOrNull()
    }

    private fun createPreviewSession() {
        val surface = surfaceView.holder.surface ?: return

        try {
            val requestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                ?: return
            requestBuilder.addTarget(surface)

            cameraDevice?.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        requestBuilder.set(
                            CaptureRequest.CONTROL_AF_MODE,
                            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                        )
                        session.setRepeatingRequest(requestBuilder.build(), null, cameraHandler)
                        runOnUiThread {
                            tvCamera2Status.text = "${if (isFrontCamera) "前置" else "后置"}摄像头预览中"
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        runOnUiThread { tvCamera2Status.text = "预览配置失败" }
                    }
                },
                cameraHandler
            )
        } catch (e: Exception) {
            Log.e(TAG, "createPreviewSession error", e)
            runOnUiThread { tvCamera2Status.text = "创建预览失败: ${e.message}" }
        }
    }

    private fun closeCamera2() {
        try {
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
        } catch (e: Exception) {
            Log.e(TAG, "closeCamera2 error", e)
        }
        stopCameraThread()
    }

    private fun startCameraThread() {
        if (cameraThread == null) {
            cameraThread = HandlerThread("Camera2Thread").also { it.start() }
            cameraHandler = Handler(cameraThread!!.looper)
        }
    }

    private fun stopCameraThread() {
        cameraThread?.quitSafely()
        try {
            cameraThread?.join(100)
        } catch (_: InterruptedException) {}
        cameraThread = null
        cameraHandler = null
    }

    // ================================================================
    //  模块二：摄像头数据流共享 (SurfaceShare)
    // ================================================================

    private fun initSurfaceShareModule() {
        ivSurfaceShare = findViewById(R.id.iv_surface_share)
        tvShareStatus = findViewById(R.id.tv_share_status)
        val btnStart = findViewById<Button>(R.id.btn_share_start)
        val btnStop = findViewById<Button>(R.id.btn_share_stop)

        btnStart.setOnClickListener { startSurfaceShare() }
        btnStop.setOnClickListener { stopSurfaceShare() }
    }

    private fun startSurfaceShare() {
        if (isSharing) {
            tvShareStatus.text = "共享流已在运行中"
            return
        }

        startShareThread()

        shareImageReader = ImageReader.newInstance(
            SHARE_IMAGE_WIDTH, SHARE_IMAGE_HEIGHT,
            ImageFormat.YUV_420_888, MAX_CACHE_IMAGES
        )
        shareImageReader?.setOnImageAvailableListener({ reader ->
            var image: Image? = null
            try {
                image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                val bitmap = yuvImageToBitmap(image)
                if (bitmap != null) {
                    runOnUiThread { ivSurfaceShare.setImageBitmap(bitmap) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "onImageAvailable error", e)
            } finally {
                image?.close()
            }
        }, shareHandler)

        if (surfaceShareBean == null) {
            surfaceShareBean = SurfaceShareBean().apply { name = "KmqSampleBody" }
        }

        val surface = shareImageReader!!.surface
        tvShareStatus.text = "正在请求共享流..."

        SurfaceShareApi.getInstance().requestImageFrame(
            surface, surfaceShareBean,
            object : SurfaceShareListener() {
                override fun onError(error: Int, message: String?) {
                    super.onError(error, message)
                    runOnUiThread {
                        tvShareStatus.text = "共享流错误: code=$error $message"
                        isSharing = false
                    }
                }

                override fun onStatusUpdate(status: Int, message: String?) {
                    super.onStatusUpdate(status, message)
                    runOnUiThread { tvShareStatus.text = "共享流状态: $status" }
                }
            }
        )

        isSharing = true
        tvShareStatus.text = "共享流已开启，等待图像数据..."
    }

    private fun stopSurfaceShare() {
        if (!isSharing) {
            tvShareStatus.text = "共享流未在运行"
            return
        }

        surfaceShareBean?.let {
            SurfaceShareApi.getInstance().abandonImageFrame(it)
        }
        shareImageReader?.close()
        shareImageReader = null
        stopShareThread()
        surfaceShareBean = null
        isSharing = false
        tvShareStatus.text = "共享流已停止"
    }

    private fun startShareThread() {
        if (shareThread == null) {
            shareThread = HandlerThread("SurfaceShareThread").also { it.start() }
            shareHandler = Handler(shareThread!!.looper)
        }
    }

    private fun stopShareThread() {
        shareThread?.quitSafely()
        try {
            shareThread?.join(100)
        } catch (_: InterruptedException) {}
        shareThread = null
        shareHandler = null
    }

    /**
     * 将 YUV_420_888 Image 转换为 ARGB Bitmap
     */
    private fun yuvImageToBitmap(image: Image): Bitmap? {
        val width = image.width
        val height = image.height
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val yRowStride = yPlane.rowStride
        val uvRowStride = uPlane.rowStride
        val uvPixelStride = uPlane.pixelStride

        val argb = IntArray(width * height)

        for (j in 0 until height) {
            for (i in 0 until width) {
                val yIndex = j * yRowStride + i
                val uvIndex = (j / 2) * uvRowStride + (i / 2) * uvPixelStride

                val y = (yBuffer.get(yIndex).toInt() and 0xFF) - 16
                val u = (uBuffer.get(uvIndex).toInt() and 0xFF) - 128
                val v = (vBuffer.get(uvIndex).toInt() and 0xFF) - 128

                val yy = if (y < 0) 0 else y
                var r = (1.164f * yy + 1.596f * v).toInt()
                var g = (1.164f * yy - 0.813f * v - 0.391f * u).toInt()
                var b = (1.164f * yy + 2.018f * u).toInt()

                r = r.coerceIn(0, 255)
                g = g.coerceIn(0, 255)
                b = b.coerceIn(0, 255)

                argb[j * width + i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        return Bitmap.createBitmap(argb, width, height, Bitmap.Config.ARGB_8888)
    }

    // ================================================================
    //  模块三：人脸识别 (PersonApi)
    // ================================================================

    private fun initFaceModule() {
        ivFacePreview = findViewById(R.id.iv_face_preview)
        tvFaceInfo = findViewById(R.id.tv_face_info)
        tvFaceNetResult = findViewById(R.id.tv_face_net_result)
        tvFaceStatus = findViewById(R.id.tv_face_status)
        val btnStart = findViewById<Button>(R.id.btn_face_start)
        val btnStop = findViewById<Button>(R.id.btn_face_stop)
        val btnRefresh = findViewById<Button>(R.id.btn_face_refresh)
        val btnNetRecognize = findViewById<Button>(R.id.btn_face_net_recognize)
        val btnSdkFront = findViewById<Button>(R.id.btn_sdk_switch_front)
        val btnSdkBack = findViewById<Button>(R.id.btn_sdk_switch_back)

        btnStart.setOnClickListener { startFaceDetection() }
        btnStop.setOnClickListener { stopFaceDetection() }
        btnRefresh.setOnClickListener { refreshPersonList() }
        btnNetRecognize.setOnClickListener { recognizeFaceFromNet() }

        btnSdkFront.setOnClickListener {
            switchSdkCamera(Definition.JSON_HEAD_FORWARD, "前置")
        }
        btnSdkBack.setOnClickListener {
            switchSdkCamera(Definition.JSON_HEAD_BACKWARD, "后置")
        }
    }

    private fun startFaceDetection() {
        if (isDetecting) {
            tvFaceStatus.text = "人脸检测已在运行中"
            Log.i(TAG, "startFaceDetection ignored: already detecting")
            return
        }

        Log.i(TAG, "startFaceDetection")
        if (cameraDevice != null || captureSession != null) {
            Log.w(TAG, "startFaceDetection: Camera2 is open, closing it before VisionSDK detection")
            closeCamera2()
            tvCamera2Status.text = "Camera2 已关闭，避免占用视觉 SDK 摄像头"
        }
        startFacePreview()
        personListener = object : PersonListener() {
            override fun personChanged() {
                super.personChanged()
                Log.d(TAG, "personChanged callback")
                runOnUiThread { refreshPersonList() }
            }
        }

        PersonApi.getInstance().registerPersonListener(personListener)
        Log.i(TAG, "PersonListener registered")
        isDetecting = true
        tvFaceStatus.text = "人脸检测已开启，等待人员出现..."
        startFacePolling()
        refreshPersonList()
    }

    private fun stopFaceDetection() {
        if (!isDetecting) {
            tvFaceStatus.text = "人脸检测未在运行"
            Log.i(TAG, "stopFaceDetection ignored: not detecting")
            return
        }

        Log.i(TAG, "stopFaceDetection")
        personListener?.let {
            PersonApi.getInstance().unregisterPersonListener(it)
            Log.i(TAG, "PersonListener unregistered")
        }
        personListener = null
        isDetecting = false
        stopFacePolling()
        stopFacePreview()
        tvFaceInfo.text = "尚未检测到人员"
        tvFaceNetResult.text = "尚未进行云端识别"
        tvFaceStatus.text = "人脸检测已停止"
    }

    private fun refreshPersonList() {
        try {
            val persons: List<Person>? = PersonApi.getInstance().getAllPersons()
            logPersonApiSnapshot(persons)
            if (persons.isNullOrEmpty()) {
                Log.d(TAG, "refreshPersonList: no persons")
                tvFaceInfo.text = "当前视野内无人员"
                if (!isRecognizingFaceFromNet) {
                    tvFaceStatus.text = "检测中 — 共 0 人，预览帧 $facePreviewFrameCount"
                }
                return
            }

            Log.i(TAG, "refreshPersonList: persons=${persons.size}")
            val sb = StringBuilder()
            sb.append("共检测到 ${persons.size} 人：\n\n")
            for ((index, p) in persons.withIndex()) {
                Log.d(TAG, "person[$index]: ${personToLogString(p)}")
                sb.append("【人员 ${index + 1}】\n")
                sb.append("  本地人脸ID: ${p.id}\n")
                sb.append("  距离: ${String.format("%.2f", p.distance)}m\n")
                sb.append("  人脸角度: X=${String.format("%.1f", p.faceAngleX)}° Y=${String.format("%.1f", p.faceAngleY)}°\n")
                sb.append("  人脸位置: (${p.faceX}, ${p.faceY}) ")
                if (p.remoteFaceId != null && p.remoteFaceId.isNotEmpty()) {
                    sb.append("\n  云端人脸ID: ${p.remoteFaceId}\n")
                } else {
                    sb.append("\n  云端人脸ID: 未返回\n")
                }
                sb.append("\n")
            }

            tvFaceInfo.text = sb.toString().trimEnd()
            if (!isRecognizingFaceFromNet) {
                tvFaceStatus.text = "检测中 — 共 ${persons.size} 人"
            }
        } catch (e: Exception) {
            tvFaceInfo.text = "获取人员失败: ${e.message}"
            Log.e(TAG, "refreshPersonList error", e)
        }
    }

    private fun startFacePolling() {
        stopFacePolling()
        facePollingRunnable = object : Runnable {
            override fun run() {
                if (!isDetecting) return
                refreshPersonList()
                facePollingHandler.postDelayed(this, 1000L)
            }
        }
        facePollingHandler.postDelayed(facePollingRunnable!!, 1000L)
        Log.i(TAG, "startFacePolling")
    }

    private fun stopFacePolling() {
        facePollingRunnable?.let { facePollingHandler.removeCallbacks(it) }
        facePollingRunnable = null
        Log.i(TAG, "stopFacePolling")
    }

    private fun startFacePreview() {
        if (isFacePreviewing) {
            Log.i(TAG, "startFacePreview ignored: already previewing")
            return
        }

        Log.i(TAG, "startFacePreview")
        facePreviewFrameCount = 0
        startFacePreviewThread()
        facePreviewImageReader = ImageReader.newInstance(
            SHARE_IMAGE_WIDTH, SHARE_IMAGE_HEIGHT,
            ImageFormat.YUV_420_888, MAX_CACHE_IMAGES
        )
        facePreviewImageReader?.setOnImageAvailableListener({ reader ->
            var image: Image? = null
            try {
                image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                facePreviewFrameCount++
                if (facePreviewFrameCount == 1 || facePreviewFrameCount % 30 == 0) {
                    Log.d(TAG, "face preview frame received: count=$facePreviewFrameCount size=${image.width}x${image.height}")
                }
                val bitmap = yuvImageToBitmap(image)
                if (bitmap != null) {
                    runOnUiThread { ivFacePreview.setImageBitmap(bitmap) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "face preview onImageAvailable error", e)
            } finally {
                image?.close()
            }
        }, facePreviewHandler)

        if (facePreviewBean == null) {
            facePreviewBean = SurfaceShareBean().apply { name = "KmqSampleBodyFacePreview" }
        }

        SurfaceShareApi.getInstance().requestImageFrame(
            facePreviewImageReader!!.surface, facePreviewBean,
            object : SurfaceShareListener() {
                override fun onError(error: Int, message: String?) {
                    super.onError(error, message)
                    Log.e(TAG, "face preview error: code=$error message=$message")
                    runOnUiThread {
                        tvFaceStatus.text = "人脸预览错误: code=$error $message"
                        isFacePreviewing = false
                    }
                }

                override fun onStatusUpdate(status: Int, message: String?) {
                    super.onStatusUpdate(status, message)
                    Log.d(TAG, "face preview status: status=$status message=$message")
                }
            }
        )

        isFacePreviewing = true
    }

    private fun stopFacePreview() {
        if (!isFacePreviewing && facePreviewImageReader == null) {
            Log.i(TAG, "stopFacePreview ignored: not previewing")
            return
        }

        Log.i(TAG, "stopFacePreview")
        facePreviewBean?.let {
            SurfaceShareApi.getInstance().abandonImageFrame(it)
        }
        facePreviewImageReader?.close()
        facePreviewImageReader = null
        stopFacePreviewThread()
        facePreviewBean = null
        isFacePreviewing = false
        facePreviewFrameCount = 0
    }

    private fun startFacePreviewThread() {
        if (facePreviewThread == null) {
            facePreviewThread = HandlerThread("FacePreviewThread").also { it.start() }
            facePreviewHandler = Handler(facePreviewThread!!.looper)
        }
    }

    private fun stopFacePreviewThread() {
        facePreviewThread?.quitSafely()
        try {
            facePreviewThread?.join(100)
        } catch (_: InterruptedException) {}
        facePreviewThread = null
        facePreviewHandler = null
    }

    private fun recognizeFaceFromNet() {
        if (isRecognizingFaceFromNet) {
            tvFaceStatus.text = "云端识别正在进行中"
            Log.i(TAG, "recognizeFaceFromNet ignored: already recognizing")
            return
        }

        val person = try {
            val completeFaces = PersonApi.getInstance().getCompleteFaceList()
            Log.i(TAG, "recognizeFaceFromNet: completeFaces=${completeFaces?.size ?: 0}")
            completeFaces?.forEachIndexed { index, face ->
                Log.d(TAG, "completeFace[$index]: ${personToLogString(face)}")
            }
            completeFaces?.firstOrNull { it.id >= 0 }
        } catch (e: Exception) {
            val text = "获取完整人脸失败: ${e.message}"
            tvFaceStatus.text = text
            tvFaceNetResult.text = text
            Log.e(TAG, "getCompleteFaceList error", e)
            return
        }

        if (person == null) {
            val text = "未找到完整人脸，请保持 1~3 米并正对摄像头"
            tvFaceStatus.text = text
            tvFaceNetResult.text = text
            Log.w(TAG, "recognizeFaceFromNet: no complete face with id >= 0")
            return
        }

        Log.i(TAG, "recognizeFaceFromNet: selected face ${personToLogString(person)}")
        isRecognizingFaceFromNet = true
        tvFaceStatus.text = "正在获取人脸照片..."
        tvFaceNetResult.text = "云端识别中...\n本地人脸ID: ${person.id}\n正在获取人脸照片"
        RobotApi.getInstance().getPictureById(
            reqId++, person.id, 1,
            object : CommandListener() {
                override fun onResult(result: Int, message: String?) {
                    Log.i(TAG, "getPictureById result=$result message=$message")
                    runOnUiThread {
                        try {
                            val json = JSONObject(message ?: "{}")
                            if (Definition.RESPONSE_OK != json.optString("status")) {
                                isRecognizingFaceFromNet = false
                                tvFaceStatus.text = "获取人脸照片失败"
                                tvFaceNetResult.text = "获取人脸照片失败:\n$message"
                                Log.w(TAG, "getPictureById failed: $message")
                                return@runOnUiThread
                            }

                            val pictures = json.optJSONArray("pictures")
                            if (pictures == null || pictures.length() == 0) {
                                isRecognizingFaceFromNet = false
                                tvFaceStatus.text = "获取人脸照片为空"
                                tvFaceNetResult.text = "获取人脸照片为空:\n$message"
                                Log.w(TAG, "getPictureById returned empty pictures")
                                return@runOnUiThread
                            }

                            val picturePaths = jsonArrayToStringList(pictures)
                            Log.i(TAG, "getPictureById pictures=$picturePaths")
                            tvFaceStatus.text = "已获取人脸照片，正在云端识别..."
                            tvFaceNetResult.text = "云端识别中...\n本地人脸ID: ${person.id}\n照片数量: ${picturePaths.size}\n正在请求云端"
                            requestPersonInfoFromNet(person.id.toString(), picturePaths)
                        } catch (e: Exception) {
                            isRecognizingFaceFromNet = false
                            tvFaceStatus.text = "解析人脸照片结果失败"
                            tvFaceNetResult.text = "解析人脸照片结果失败: ${e.message}\nraw: $message"
                            Log.e(TAG, "getPictureById result parse error", e)
                        }
                    }
                }
            }
        )
    }

    private fun requestPersonInfoFromNet(faceId: String, pictures: List<String>) {
        Log.i(TAG, "requestPersonInfoFromNet: faceId=$faceId pictures=$pictures")
        RobotApi.getInstance().getPersonInfoFromNet(
            reqId++, faceId, pictures,
            object : CommandListener() {
                override fun onResult(result: Int, message: String?) {
                    Log.i(TAG, "getPersonInfoFromNet result=$result message=$message")
                    runOnUiThread {
                        isRecognizingFaceFromNet = false
                        deletePictureFiles(pictures)
                        try {
                            val json = JSONObject(message ?: "{}")
                            val people = json.optJSONObject("people")
                            val sb = StringBuilder()
                            sb.append("云端识别返回：\n")
                            sb.append("本地人脸ID: $faceId\n")
                            sb.append("status: ${json.optString("status")}\n")
                            sb.append("result: $result\n")
                            if (people != null) {
                                sb.append("name: ${people.optString("name")}\n")
                                sb.append("gender: ${people.optString("gender")}\n")
                                sb.append("age: ${people.optString("age")}\n")
                            }
                            sb.append("raw: $message")
                            tvFaceNetResult.text = sb.toString()
                            tvFaceStatus.text = "云端识别完成"
                        } catch (e: Exception) {
                            tvFaceNetResult.text = "云端识别原始返回:\n$message"
                            tvFaceStatus.text = "解析云端识别结果失败"
                            Log.e(TAG, "getPersonInfoFromNet result parse error", e)
                        }
                    }
                }
            }
        )
    }

    private fun jsonArrayToStringList(array: JSONArray): List<String> {
        val result = mutableListOf<String>()
        for (i in 0 until array.length()) {
            val value = array.optString(i)
            if (value.isNotEmpty()) {
                result.add(value)
            }
        }
        return result
    }

    private fun deletePictureFiles(pictures: List<String>) {
        for (path in pictures) {
            if (path.isNotEmpty()) {
                val deleted = runCatching { File(path).delete() }.getOrDefault(false)
                Log.d(TAG, "deletePictureFile path=$path deleted=$deleted")
            }
        }
    }

    private fun logPersonApiSnapshot(persons: List<Person>?) {
        val faceList = runCatching { PersonApi.getInstance().getAllFaceList() }.getOrNull()
        val bodyList = runCatching { PersonApi.getInstance().getAllBodyList() }.getOrNull()
        val completeFaceList = runCatching { PersonApi.getInstance().getCompleteFaceList() }.getOrNull()
        val focusPerson = runCatching { PersonApi.getInstance().getFocusPerson() }.getOrNull()
        Log.i(
            TAG,
            "PersonApi snapshot: all=${persons?.size ?: 0}, " +
                "face=${faceList?.size ?: 0}, body=${bodyList?.size ?: 0}, " +
                "completeFace=${completeFaceList?.size ?: 0}, " +
                "focus=${focusPerson?.let { personToLogString(it) } ?: "null"}, " +
                "previewing=$isFacePreviewing, previewFrames=$facePreviewFrameCount"
        )
        faceList?.forEachIndexed { index, person ->
            Log.d(TAG, "faceList[$index]: ${personToLogString(person)}")
        }
        bodyList?.forEachIndexed { index, person ->
            Log.d(TAG, "bodyList[$index]: ${personToLogString(person)}")
        }
        completeFaceList?.forEachIndexed { index, person ->
            Log.d(TAG, "completeFaceList[$index]: ${personToLogString(person)}")
        }
    }

    private fun personToLogString(person: Person): String {
        return "id=${person.id}, distance=${person.distance}, " +
            "faceAngleX=${person.faceAngleX}, faceAngleY=${person.faceAngleY}, " +
            "faceX=${person.faceX}, faceY=${person.faceY}, " +
            "remoteFaceId=${person.remoteFaceId}"
    }

    private fun switchSdkCamera(camera: String, label: String) {
        tvFaceStatus.text = "切换视觉SDK摄像头到${label}..."
        Log.i(TAG, "switchSdkCamera: camera=$camera label=$label")
        RobotApi.getInstance().switchCamera(
            reqId++, camera,
            object : CommandListener() {
                override fun onResult(result: Int, message: String?) {
                    Log.i(TAG, "switchSdkCamera result=$result message=$message")
                    runOnUiThread {
                        try {
                            val json = JSONObject(message ?: "{}")
                            if ("ok" == json.optString("status")) {
                                tvFaceStatus.text = "视觉SDK已切换到${label}摄像头"
                            } else {
                                tvFaceStatus.text = "切换失败: $message"
                            }
                        } catch (e: Exception) {
                            tvFaceStatus.text = "切换结果: $message"
                        }
                    }
                }
            }
        )
    }

    // ================================================================
    //  生命周期
    // ================================================================

    override fun onResume() {
        super.onResume()
        if (isDetecting && !isFacePreviewing) {
            Log.i(TAG, "onResume: restart face preview")
            startFacePreview()
        }
        if (isDetecting && facePollingRunnable == null) {
            Log.i(TAG, "onResume: restart face polling")
            startFacePolling()
        }
    }

    override fun onPause() {
        super.onPause()
        closeCamera2()
        stopFacePolling()
        stopFacePreview()
    }

    override fun onDestroy() {
        super.onDestroy()
        closeCamera2()
        stopSurfaceShare()
        stopFaceDetection()
        stopFacePreview()
    }
}
