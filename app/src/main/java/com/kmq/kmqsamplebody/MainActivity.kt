package com.kmq.kmqsamplebody

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kmq.kmqsamplebody.asr.AsrActivity
import com.kmq.kmqsamplebody.camera.CameraActivity
import com.kmq.kmqsamplebody.systeminfo.SystemInfoActivity
import com.kmq.kmqsamplebody.chassis.ChassisActivity
import com.kmq.kmqsamplebody.location.LocationActivity
import com.kmq.kmqsamplebody.position.PositionActivity
import com.kmq.kmqsamplebody.tts.TtsActivity
import com.kmq.kmqsamplebody.tts.TtsStreamActivity

class MainActivity : AppCompatActivity() {

    private val features = listOf(
        FeatureItem("TTS播报", "文本转语音播报", R.drawable.ic_tts, TtsActivity::class.java),
        FeatureItem("TTS流式播报", "带回调的流式播报", R.drawable.ic_tts_stream, TtsStreamActivity::class.java),
        FeatureItem("ASR识别", "语音转文本识别", R.drawable.ic_asr, AsrActivity::class.java),
        FeatureItem("底盘 & 云台", "底盘移动 + 云台上下控制", R.drawable.ic_chassis, ChassisActivity::class.java),
        FeatureItem("定位", "获取机器人位置", R.drawable.ic_location, LocationActivity::class.java),
        FeatureItem("点位获取", "已配置点位列表", R.drawable.ic_position, PositionActivity::class.java),
        FeatureItem("摄像头 & 人脸", "Camera2 / 共享流 / 人脸识别", R.drawable.ic_camera, CameraActivity::class.java),
        FeatureItem("系统信息", "版本 / SN / 电池状态", R.drawable.ic_charging, SystemInfoActivity::class.java)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val recyclerView = findViewById<RecyclerView>(R.id.rv_features)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = FeatureAdapter(features) { item ->
            startActivity(Intent(this, item.targetActivity))
        }
    }
}
