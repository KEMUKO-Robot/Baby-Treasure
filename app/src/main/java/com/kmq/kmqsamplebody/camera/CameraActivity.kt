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
    private lateinit var tvFaceInfo: TextView
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
        tvFaceInfo = findViewById(R.id.tv_face_info)
        tvFaceStatus = findViewById(R.id.tv_face_status)
        val btnStart = findViewById<Button>(R.id.btn_face_start)
        val btnStop = findViewById<Button>(R.id.btn_face_stop)
        val btnRefresh = findViewById<Button>(R.id.btn_face_refresh)
        val btnSdkFront = findViewById<Button>(R.id.btn_sdk_switch_front)
        val btnSdkBack = findViewById<Button>(R.id.btn_sdk_switch_back)

        btnStart.setOnClickListener { startFaceDetection() }
        btnStop.setOnClickListener { stopFaceDetection() }
        btnRefresh.setOnClickListener { refreshPersonList() }

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
            return
        }

        personListener = object : PersonListener() {
            override fun personChanged() {
                super.personChanged()
                runOnUiThread { refreshPersonList() }
            }
        }

        PersonApi.getInstance().registerPersonListener(personListener)
        isDetecting = true
        tvFaceStatus.text = "人脸检测已开启，等待人员出现..."
        refreshPersonList()
    }

    private fun stopFaceDetection() {
        if (!isDetecting) {
            tvFaceStatus.text = "人脸检测未在运行"
            return
        }

        personListener?.let {
            PersonApi.getInstance().unregisterPersonListener(it)
        }
        personListener = null
        isDetecting = false
        tvFaceInfo.text = "尚未检测到人员"
        tvFaceStatus.text = "人脸检测已停止"
    }

    private fun refreshPersonList() {
        try {
            val persons: List<Person>? = PersonApi.getInstance().getAllPersons()
            if (persons.isNullOrEmpty()) {
                tvFaceInfo.text = "当前视野内无人员"
                tvFaceStatus.text = "检测中 — 共 0 人"
                return
            }

            val sb = StringBuilder()
            sb.append("共检测到 ${persons.size} 人：\n\n")
            for ((index, p) in persons.withIndex()) {
                sb.append("【人员 ${index + 1}】\n")
                sb.append("  ID: ${p.id}")
                sb.append("  距离: ${String.format("%.2f", p.distance)}m\n")
                sb.append("  人脸角度: X=${String.format("%.1f", p.faceAngleX)}° Y=${String.format("%.1f", p.faceAngleY)}°\n")
                sb.append("  人脸位置: (${p.faceX}, ${p.faceY}) ")
                if (p.remoteFaceId != null && p.remoteFaceId.isNotEmpty()) {
                    sb.append("  已注册: ${p.remoteFaceId}\n")
                }
                sb.append("\n")
            }

            tvFaceInfo.text = sb.toString().trimEnd()
            tvFaceStatus.text = "检测中 — 共 ${persons.size} 人"
        } catch (e: Exception) {
            tvFaceInfo.text = "获取人员失败: ${e.message}"
            Log.e(TAG, "refreshPersonList error", e)
        }
    }

    private fun switchSdkCamera(camera: String, label: String) {
        tvFaceStatus.text = "切换视觉SDK摄像头到${label}..."
        RobotApi.getInstance().switchCamera(
            reqId++, camera,
            object : CommandListener() {
                override fun onResult(result: Int, message: String?) {
                    runOnUiThread {
                        try {
                            val json = org.json.JSONObject(message ?: "{}")
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

    override fun onPause() {
        super.onPause()
        closeCamera2()
    }

    override fun onDestroy() {
        super.onDestroy()
        closeCamera2()
        stopSurfaceShare()
        stopFaceDetection()
    }
}
