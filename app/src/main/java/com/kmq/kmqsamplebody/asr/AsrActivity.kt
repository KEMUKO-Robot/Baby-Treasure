package com.kmq.kmqsamplebody.asr

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ainirobot.agent.AgentCore
import com.ainirobot.agent.OnTranscribeListener
import com.ainirobot.agent.PageAgent
import com.ainirobot.agent.base.Transcription
import com.kmq.kmqsamplebody.MainActivity
import com.kmq.kmqsamplebody.R

/**
 * ASR（语音识别）功能演示页面
 *
 * 本页面提供以下核心功能：
 * 1. 语音录入（已消费模式）—— 识别结果由本页面处理，onASRResult 返回 true，
 *    大模型不会收到语音内容，因此不会自动回复。
 * 2. 语音录入（未消费模式）—— 识别结果不被消费，onASRResult 返回 false，
 *    大模型会接收语音内容并自动生成回复。
 * 3. 停止录入
 * 4. 返回首页
 */
class AsrActivity : AppCompatActivity() {

    companion object {
        /** 日志标签 */
        private const val TAG = "AsrActivity"
    }

    /** PageAgent 用于管理当前页面与机器人 Agent 框架的交互，包括 ASR 事件监听 */
    private lateinit var pageAgent: PageAgent

    /**
     * 录音模式枚举
     * - NONE：未在录音状态
     * - CONSUMED：已消费模式，onASRResult 返回 true，语音结果已被当前页面消费，大模型不会自动处理
     * - UNCONSUMED：未消费模式，onASRResult 返回 false，语音结果未被消费，大模型会继续处理并自动回复
     */
    private enum class RecordMode { NONE, CONSUMED, UNCONSUMED }

    /** 当前录音模式，默认为 NONE（未录音） */
    private var recordMode = RecordMode.NONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asr)

        // 初始化 PageAgent，绑定当前 Activity 的生命周期
        pageAgent = PageAgent(this)

        // 获取界面控件引用
        val tvResult = findViewById<TextView>(R.id.tv_asr_result)                      // 识别结果展示区域
        val tvStatus = findViewById<TextView>(R.id.tv_asr_status)                      // 状态文本
        val btnVoiceConsumed = findViewById<Button>(R.id.btn_voice_consumed)            // 语音录入按钮（已消费模式）
        val btnVoiceUnconsumed = findViewById<Button>(R.id.btn_voice_unconsumed)        // 语音录入按钮（未消费模式）
        val btnStop = findViewById<Button>(R.id.btn_asr_stop)                          // 停止按钮
        val btnBackHome = findViewById<Button>(R.id.btn_back_home)                     // 返回首页按钮

        /**
         * 重置所有状态到空闲态
         * - 清除录音模式
         * - 关闭麦克风（静音）
         * - 禁用大模型自动回复
         * - 隐藏语音条
         * - 恢复按钮文案和可点击状态
         */
        fun resetToIdle() {
            recordMode = RecordMode.NONE
            AgentCore.isMicrophoneMuted = true
            AgentCore.isDisablePlan = true
            AgentCore.isEnableVoiceBar = false
            btnVoiceConsumed.text = "语音录入(已消费)"
            btnVoiceUnconsumed.text = "语音录入(未消费)"
            btnVoiceConsumed.isEnabled = true
            btnVoiceUnconsumed.isEnabled = true
        }

        // 页面初始化时先重置为空闲状态
        resetToIdle()

        /**
         * 设置语音转写监听器，用于接收 ASR 语音识别的回调
         */
        pageAgent.setOnTranscribeListener(object : OnTranscribeListener {

            /**
             * ASR 语音识别结果回调
             *
             * @param transcription 识别结果对象，包含：
             *   - text：当前识别到的文本内容
             *   - final：是否为最终结果（true 表示一句话识别完毕）
             * @return true 表示"已消费"该结果（大模型不会再处理），false 表示"未消费"（大模型会继续处理）
             */
            override fun onASRResult(transcription: Transcription): Boolean {
                val currentMode = recordMode
                val isConsumed = currentMode == RecordMode.CONSUMED
                runOnUiThread {
                    // 将识别到的文本实时显示在结果区域
                    tvResult.text = transcription.text
                    if (transcription.`final`) {
                        Log.d(TAG, "ASR final: ${transcription.text}, consumed=$isConsumed")
                        resetToIdle()
                        tvStatus.text = "识别完成"
                    } else {
                        tvStatus.text = "识别中..."
                    }
                }
                return isConsumed
            }

            /**
             * TTS 播报结果回调（ASR 页面不处理 TTS 事件）
             */
            override fun onTTSResult(transcription: Transcription): Boolean {
                return false
            }
        })

        /**
         * 语音录入按钮（已消费模式）：
         * - 首次点击：开启麦克风，进入"已消费"录音模式。识别结果由本页面处理，
         *   onASRResult 返回 true，大模型不会收到该语音内容，因此不会自动回复。
         * - 再次点击：停止录入，恢复空闲状态。
         */
        btnVoiceConsumed.setOnClickListener {
            if (recordMode == RecordMode.CONSUMED) {
                resetToIdle()
                tvStatus.text = "已停止录入"
            } else {
                // 先停止之前可能的录入状态
                if (recordMode != RecordMode.NONE) {
                    resetToIdle()
                }
                tvResult.text = ""
                recordMode = RecordMode.CONSUMED
                AgentCore.isDisablePlan = true
                AgentCore.isEnableVoiceBar = false
                btnVoiceConsumed.text = "停止录入(已消费)"
                btnVoiceUnconsumed.isEnabled = false
                tvStatus.text = "请说话...(已消费，不会自动回复)"
                Log.d(TAG, "Voice CONSUMED mode started")
                // 最后打开麦克风，确保其他状态已就绪
                AgentCore.isMicrophoneMuted = false
            }
        }

        /**
         * 语音录入按钮（未消费模式）：
         * - 首次点击：开启麦克风，进入"未消费"录音模式。识别结果不被消费，
         *   onASRResult 返回 false，大模型会接收语音内容并自动生成回复。
         * - 再次点击：停止录入，恢复空闲状态。
         */
        btnVoiceUnconsumed.setOnClickListener {
            if (recordMode == RecordMode.UNCONSUMED) {
                resetToIdle()
                tvStatus.text = "已停止录入"
            } else {
                // 先停止之前可能的录入状态
                if (recordMode != RecordMode.NONE) {
                    resetToIdle()
                }
                tvResult.text = ""
                recordMode = RecordMode.UNCONSUMED
                AgentCore.isDisablePlan = false
                AgentCore.isEnableVoiceBar = false
                btnVoiceUnconsumed.text = "停止录入(未消费)"
                btnVoiceConsumed.isEnabled = false
                tvStatus.text = "请说话...(未消费，大模型会自动回复)"
                Log.d(TAG, "Voice UNCONSUMED mode started")
                // 最后打开麦克风，确保其他状态已就绪
                AgentCore.isMicrophoneMuted = false
            }
        }

        /**
         * 停止按钮：
         * 停止当前语音录入，静音麦克风并清除对话上下文
         */
        btnStop.setOnClickListener {
            Log.d(TAG, "Stop button clicked")
            AgentCore.isDisablePlan = true
            AgentCore.isMicrophoneMuted = true
            AgentCore.clearContext()
            if (recordMode != RecordMode.NONE) {
                resetToIdle()
            }
            tvStatus.text = "已停止"
        }

        /**
         * 返回首页按钮：
         * 使用 FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP 回到 MainActivity，
         * 并关闭当前页面
         */
        btnBackHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }

    /**
     * Activity 销毁时的清理工作：
     * 静音麦克风并清除对话上下文，防止资源泄漏和状态残留
     */
    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        AgentCore.isMicrophoneMuted = true
        AgentCore.clearContext()
        super.onDestroy()
    }
}
