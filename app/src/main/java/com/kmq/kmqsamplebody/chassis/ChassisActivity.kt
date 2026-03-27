package com.kmq.kmqsamplebody.chassis

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ainirobot.coreservice.client.RobotApi
import com.ainirobot.coreservice.client.listener.CommandListener
import com.kmq.kmqsamplebody.MainApplication
import com.kmq.kmqsamplebody.R

/**
 * 底盘移动 & 云台控制页面
 *
 * 布局：左侧底盘控制，右侧云台控制
 *
 * ==================== 底盘移动 API ====================
 *
 * 【goForward 前进】— 有三个重载版本：
 *   1. goForward(reqId, speed, motionListener)
 *      持续前进，不限距离，需手动调用 stopMove 停止
 *   2. goForward(reqId, speed, distance, motionListener)        ← 本页面使用
 *      前进指定距离后自动停止
 *   3. goForward(reqId, speed, distance, avoid, motionListener)
 *      前进指定距离，可开启避障（avoid=true 时遇障碍物会避停）
 *
 * 【goBackward 后退】— 有两个重载版本：
 *   1. goBackward(reqId, speed, motionListener)
 *      持续后退，不限距离，需手动调用 stopMove 停止
 *   2. goBackward(reqId, speed, distance, motionListener)       ← 本页面使用
 *      后退指定距离后自动停止
 *   注意：后退没有避障功能，请谨慎使用
 *
 * 【turnLeft / turnRight 左转/右转】— 各有两个重载版本：
 *   1. turnLeft(reqId, speed, rotateListener)
 *      持续左转，需手动调用 stopMove 停止
 *   2. turnLeft(reqId, speed, angle, rotateListener)              ← 本页面使用
 *      左转指定角度后自动停止
 *   turnRight 同理
 *
 * 【stopMove 停止】
 *   stopMove(reqId, listener) — 立即停止所有底盘运动
 *
 * 公共参数说明：
 *   - reqId:    Int     — 请求ID，用于日志追踪，自增即可
 *   - speed:    Float   — 前进/后退速度，单位 m/s，范围 0~1.0，大于1.0按1.0执行
 *                          旋转速度，单位 度/s，范围 0~50 度/s
 *   - distance: Float   — 运动距离，单位 m，值需大于0，到达后自动停止
 *   - angle:    Float   — 旋转角度，单位 度，值需大于0，到达后自动停止
 *   - avoid:    Boolean — 是否避障（仅 goForward 支持，后退无避障）
 *   - listener: CommandListener — 结果/状态回调
 *
 * ==================== 云台控制 API ====================
 * （仅豹二、小宝支持，其他机型无云台硬件）
 *
 * moveHead(reqId, hmode, vmode, hangle, vangle, listener)
 *   - hmode:  String — 水平模式 "relative"(相对偏移) / "absolute"(绝对角度)
 *   - vmode:  String — 垂直模式 "relative"(相对偏移) / "absolute"(绝对角度)
 *   - hangle: Int    — 水平角度值
 *   - vangle: Int    — 垂直角度值，正值向上，负值向下
 *   本页面只控制上下，水平保持不变: hmode="relative", hangle=0
 *
 * moveHead 还有带速度的重载版本：
 * moveHead(reqId, hmode, vmode, hangle, vangle, hMaxSpeed, vMaxSpeed, listener)
 *   - hMaxSpeed: Int — 水平最大转速
 *   - vMaxSpeed: Int — 垂直最大转速
 */
class ChassisActivity : AppCompatActivity() {

    private var reqId = 0

    companion object {
        // 前进/后退速度（m/s），范围 0~1.0，大于1.0按1.0执行
        private const val CHASSIS_SPEED = 0.3f

        // 前进/后退距离（米），到达后自动停止
        private const val MOVE_DISTANCE = 1.0f

        // 旋转速度（度/s），范围 0~50
        private const val TURN_SPEED = 20.0f

        // 旋转角度（度），到达后自动停止
        private const val TURN_ANGLE = 50.0f

        // 云台每次俯仰偏移角度（度）
        private const val HEAD_PITCH_STEP = 10
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chassis)

        findViewById<Button>(R.id.btn_back_home).setOnClickListener { finish() }

        val tvStatus = findViewById<TextView>(R.id.tv_chassis_status)

        val app = application as MainApplication
        if (!app.isRobotApiConnected) {
            tvStatus.text = "RobotApi 未连接，请确认从机器人桌面启动应用"
        }

        // --- 底盘控制按钮 ---
        val btnForward = findViewById<Button>(R.id.btn_forward)
        val btnBackward = findViewById<Button>(R.id.btn_backward)
        val btnTurnLeft = findViewById<Button>(R.id.btn_turn_left)
        val btnTurnRight = findViewById<Button>(R.id.btn_turn_right)
        val btnStop = findViewById<Button>(R.id.btn_chassis_stop)

        // --- 云台控制按钮（仅豹二、小宝支持） ---
        val btnHeadUp = findViewById<Button>(R.id.btn_head_up)
        val btnHeadDown = findViewById<Button>(R.id.btn_head_down)
        val btnHeadReset = findViewById<Button>(R.id.btn_head_reset)
        val btnHeadStop = findViewById<Button>(R.id.btn_head_stop)

        val listener = object : CommandListener() {
            override fun onResult(result: Int, message: String?) {
                runOnUiThread { tvStatus.text = "结果: $message" }
            }

            override fun onStatusUpdate(status: Int, data: String?, extraData: String?) {
                runOnUiThread { tvStatus.text = "状态: status=$status data=$data" }
            }
        }

        // ========================
        // 底盘移动（均使用限距/限角版本，到达后自动停止）
        // 前进/后退：goForward/goBackward(reqId, speed, distance, listener)
        //   速度 0.3 m/s，距离 1 米
        // 左转/右转：turnLeft/turnRight(reqId, speed, angle, listener)
        //   速度 20 度/s，角度 50 度
        // ========================

        // 前进 1 米后自动停止（速度 0.3 m/s）
        btnForward.setOnClickListener {
            tvStatus.text = "前进 ${MOVE_DISTANCE}米（${CHASSIS_SPEED}m/s）..."
            RobotApi.getInstance().goForward(reqId++, CHASSIS_SPEED, MOVE_DISTANCE, listener)
        }

        // 后退 1 米后自动停止（速度 0.3 m/s，无避障）
        btnBackward.setOnClickListener {
            tvStatus.text = "后退 ${MOVE_DISTANCE}米（${CHASSIS_SPEED}m/s）..."
            RobotApi.getInstance().goBackward(reqId++, CHASSIS_SPEED, MOVE_DISTANCE, listener)
        }

        // 左转 50 度后自动停止（速度 20 度/s）
        btnTurnLeft.setOnClickListener {
            tvStatus.text = "左转 ${TURN_ANGLE}°（${TURN_SPEED}°/s）..."
            RobotApi.getInstance().turnLeft(reqId++, TURN_SPEED, TURN_ANGLE, listener)
        }

        // 右转 50 度后自动停止（速度 20 度/s）
        btnTurnRight.setOnClickListener {
            tvStatus.text = "右转 ${TURN_ANGLE}°（${TURN_SPEED}°/s）..."
            RobotApi.getInstance().turnRight(reqId++, TURN_SPEED, TURN_ANGLE, listener)
        }

        // 立即停止底盘运动
        btnStop.setOnClickListener {
            tvStatus.text = "底盘已停止"
            RobotApi.getInstance().stopMove(reqId++, listener)
        }

        // ========================
        // 云台控制（上下俯仰，仅豹二、小宝支持）
        // moveHead(reqId, hmode, vmode, hangle, vangle, listener)
        // 水平方向不动: hmode="relative", hangle=0
        // 垂直方向相对偏移: vmode="relative", vangle=±10
        // ========================

        // 云台上仰（垂直方向相对上移 10°）
        btnHeadUp.setOnClickListener {
            tvStatus.text = "云台上仰 ${HEAD_PITCH_STEP}°..."
            RobotApi.getInstance().moveHead(reqId++, "relative", "relative", 0, HEAD_PITCH_STEP, listener)
        }

        // 云台下俯（垂直方向相对下移 10°）
        btnHeadDown.setOnClickListener {
            tvStatus.text = "云台下俯 ${HEAD_PITCH_STEP}°..."
            RobotApi.getInstance().moveHead(reqId++, "relative", "relative", 0, -HEAD_PITCH_STEP, listener)
        }

        // 云台复位（绝对角度归零）
        btnHeadReset.setOnClickListener {
            tvStatus.text = "云台复位中..."
            RobotApi.getInstance().moveHead(reqId++, "absolute", "absolute", 0, 0, listener)
        }

        // 停止云台运动
        btnHeadStop.setOnClickListener {
            tvStatus.text = "云台已停止"
            RobotApi.getInstance().stopMove(reqId++, listener)
        }
    }
}
