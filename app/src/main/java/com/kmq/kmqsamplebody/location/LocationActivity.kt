package com.kmq.kmqsamplebody.location

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ainirobot.coreservice.client.Definition
import com.ainirobot.coreservice.client.RobotApi
import com.ainirobot.coreservice.client.listener.CommandListener
import com.ainirobot.coreservice.client.StatusListener
import com.kmq.kmqsamplebody.R
import org.json.JSONObject

/**
 * 定位信息页面
 *
 * 【定位状态监听】
 * - registerStatusListener(Definition.STATUS_POSE_ESTIMATE, listener)
 *   注册定位状态监听，当定位状态发生改变时回调
 *   data 值说明：
 *     "0" — 未定位（机器人尚未完成定位）
 *     "1" — 已定位（机器人已成功定位）
 *
 * 【获取当前位置】
 * - getPosition(reqId, listener)
 *   获取机器人当前位姿，返回 JSON 包含 x, y, theta
 *   x/y: 坐标位置（米），theta: 朝向角度（弧度）
 */
class LocationActivity : AppCompatActivity() {

    private var reqId = 0
    private var statusListener: StatusListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        findViewById<Button>(R.id.btn_back_home).setOnClickListener { finish() }

        val tvPoseStatus = findViewById<TextView>(R.id.tv_pose_status)
        val tvLocation = findViewById<TextView>(R.id.tv_location_info)
        val btnGetLocation = findViewById<Button>(R.id.btn_get_location)

        // 注册定位状态监听
        statusListener = object : StatusListener() {
            override fun onStatusUpdate(type: String?, data: String?) {
                runOnUiThread {
                    val statusText = when (data) {
                        "0" -> "未定位"
                        "1" -> "已定位"
                        else -> "未知（$data）"
                    }
                    tvPoseStatus.text = "定位状态：$statusText"
                }
            }
        }
        RobotApi.getInstance().registerStatusListener(Definition.STATUS_POSE_ESTIMATE, statusListener)

        // 主动查询一次当前定位状态
        RobotApi.getInstance().getRobotStatus(Definition.STATUS_POSE_ESTIMATE, statusListener)

        btnGetLocation.setOnClickListener {
            tvLocation.text = "正在获取定位信息..."
            RobotApi.getInstance().getPosition(reqId++, object : CommandListener() {
                override fun onResult(result: Int, message: String?) {
                    runOnUiThread {
                        try {
                            val json = JSONObject(message ?: "{}")
                            val x = json.optDouble("x", 0.0)
                            val y = json.optDouble("y", 0.0)
                            val theta = json.optDouble("theta", 0.0)
                            tvLocation.text = "当前位置:\nX: $x\nY: $y\nTheta: $theta"
                        } catch (e: Exception) {
                            tvLocation.text = "解析失败: $message"
                        }
                    }
                }
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        statusListener?.let {
            RobotApi.getInstance().unregisterStatusListener(it)
        }
    }
}
