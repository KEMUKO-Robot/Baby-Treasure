package com.kmq.kmqsamplebody.tts

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ainirobot.agent.AgentCore
import com.ainirobot.agent.OnTranscribeListener
import com.ainirobot.agent.PageAgent
import com.ainirobot.agent.base.Transcription
import com.kmq.kmqsamplebody.MainActivity
import com.kmq.kmqsamplebody.R

/**
 * TTS（文字转语音）功能演示页面
 *
 * 本页面提供以下核心功能：
 * 1. 文字转语音播报 —— 支持显示/隐藏语音条两种模式
 * 2. 语音录入（ASR）—— 支持"已消费"和"未消费"两种模式：
 *    - 已消费模式：语音识别结果由本页面处理，大模型不会自动回复
 *    - 未消费模式：语音识别结果不被消费，大模型会接收并自动回复
 * 3. 停止播报/录入
 * 4. 返回主页
 */
class TtsActivity : AppCompatActivity() {

    companion object {
        /** 日志标签 */
        private const val TAG = "TtsActivity"
    }

    /** PageAgent 用于管理当前页面与机器人 Agent 框架的交互，包括 ASR/TTS 事件监听 */
    private lateinit var pageAgent: PageAgent

    /**
     * 录音模式枚举
     * - NONE：未在录音状态
     * - CONSUMED：已消费模式，onASRResult 返回 true，表示语音结果已被当前页面消费，大模型不会自动处理
     * - UNCONSUMED：未消费模式，onASRResult 返回 false，表示语音结果未被消费，大模型会继续处理并自动回复
     */
    private enum class RecordMode { NONE, CONSUMED, UNCONSUMED }

    /** 当前录音模式，默认为 NONE（未录音） */
    private var recordMode = RecordMode.NONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tts)

        // 初始化 PageAgent，绑定当前 Activity 的生命周期
        pageAgent = PageAgent(this)

        // 获取界面控件引用
        val etInput = findViewById<EditText>(R.id.et_tts_input)             // 文本输入框，用于输入待播报文字 / 展示语音识别结果
        val btnSpeakShow = findViewById<Button>(R.id.btn_tts_speak_show)    // 播报按钮（显示语音条）
        val btnSpeakHide = findViewById<Button>(R.id.btn_tts_speak_hide)    // 播报按钮（隐藏语音条）
        val btnStop = findViewById<Button>(R.id.btn_tts_stop)               // 停止按钮
        val tvStatus = findViewById<TextView>(R.id.tv_tts_status)           // 状态文本，显示当前操作状态
        val btnVoiceConsumed = findViewById<Button>(R.id.btn_voice_consumed)       // 语音录入按钮（已消费模式）
        val btnVoiceUnconsumed = findViewById<Button>(R.id.btn_voice_unconsumed)   // 语音录入按钮（未消费模式）
        val btnBackHome = findViewById<Button>(R.id.btn_back_home)          // 返回主页按钮

        /**
         * 重置所有状态到空闲态
         * - 清除录音模式
         * - 关闭麦克风（静音）
         * - 禁用大模型自动回复（isDisablePlan = true）
         * - 隐藏语音条
         * - 恢复按钮文案和可点击状态
         */
        fun resetToIdle() {
            recordMode = RecordMode.NONE
            AgentCore.isMicrophoneMuted = true       // 静音麦克风，停止接收语音输入
            AgentCore.isDisablePlan = true            // 禁用大模型自动规划/回复
            AgentCore.isEnableVoiceBar = false        // 隐藏语音条 UI
            btnVoiceConsumed.text = "语音录入(已消费)"
            btnVoiceUnconsumed.text = "语音录入(未消费)"
            btnVoiceConsumed.isEnabled = true
            btnVoiceUnconsumed.isEnabled = true
        }

        // 页面初始化时先重置为空闲状态
        resetToIdle()

        /**
         * 设置语音转写监听器，用于接收 ASR（语音识别）和 TTS（文字转语音）的回调
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
                if (recordMode != RecordMode.NONE) {
                    val isConsumed = recordMode == RecordMode.CONSUMED
                    runOnUiThread {
                        // 将识别到的文本实时显示在输入框中
                        etInput.setText(transcription.text)
                        if (transcription.`final`) {
                            // 识别完成（最终结果），重置状态
                            Log.d(TAG, "ASR final: ${transcription.text}, consumed=$isConsumed")
                            resetToIdle()
                            tvStatus.text = "语音录入完成"
                        } else {
                            // 中间结果，持续显示识别状态
                            tvStatus.text = "识别中..."
                        }
                    }
                    // 根据当前模式决定是否消费该识别结果
                    return isConsumed
                }
                // 未处于录音模式时不消费结果
                return false
            }

            /**
             * TTS 播报结果回调
             *
             * @param transcription TTS 播报状态信息
             * @return 是否消费该结果，此处固定返回 false 不做额外处理
             */
            override fun onTTSResult(transcription: Transcription): Boolean {
                return false
            }
        })

        /**
         * 播报按钮（显示语音条）：
         * 将输入框中的文本通过 TTS 播报出来，同时在界面上显示语音条动画
         */
        btnSpeakShow.setOnClickListener {
            val text = etInput.text.toString().trim()
            if (text.isNotEmpty()) {
                resetToIdle()
                AgentCore.isEnableVoiceBar = true    // 启用语音条 UI 展示
                tvStatus.text = "正在播报(显示语音条)..."
                Log.d(TAG, "Start TTS with voice bar: $text")
                AgentCore.tts(text)                  // 调用 TTS 引擎进行语音播报
            }
        }

        /**
         * 播报按钮（隐藏语音条）：
         * 将输入框中的文本通过 TTS 播报出来，但不显示语音条动画
         */
        btnSpeakHide.setOnClickListener {
            val text = etInput.text.toString().trim()
            if (text.isNotEmpty()) {
                resetToIdle()
                AgentCore.isEnableVoiceBar = false   // 不显示语音条 UI
                tvStatus.text = "正在播报(不显示语音条)..."
                Log.d(TAG, "Start TTS without voice bar: $text")
                AgentCore.tts(text)                  // 调用 TTS 引擎进行语音播报
            }
        }

        /**
         * 停止按钮：
         * 停止当前正在进行的 TTS 播报或语音录入，并清除对话上下文
         */
        btnStop.setOnClickListener {
            Log.d(TAG, "Stop button clicked")
            AgentCore.isDisablePlan = true           // 禁用大模型自动规划
            AgentCore.isMicrophoneMuted = true       // 静音麦克风
            AgentCore.stopTTS()                      // 停止 TTS 播报
            AgentCore.clearContext()                  // 清除对话上下文，避免残留状态影响后续交互
            if (recordMode != RecordMode.NONE) {
                resetToIdle()
            }
            tvStatus.text = "已停止"
        }

        /**
         * 语音录入按钮（已消费模式）：
         * - 首次点击：开启麦克风，进入"已消费"录音模式。识别结果由本页面处理，
         *   onASRResult 返回 true，大模型不会收到该语音内容，因此不会自动回复。
         * - 再次点击：停止录入，恢复空闲状态。
         */
        btnVoiceConsumed.setOnClickListener {
            if (recordMode == RecordMode.CONSUMED) {
                // 当前正在"已消费"录音，再次点击则停止
                resetToIdle()
                tvStatus.text = "已停止录入"
            } else {
                resetToIdle()
                recordMode = RecordMode.CONSUMED
                AgentCore.isDisablePlan = true        // 禁用大模型自动规划（已消费模式下无需大模型处理）
                AgentCore.isMicrophoneMuted = false   // 打开麦克风开始接收语音
                btnVoiceConsumed.text = "停止录入(已消费)"
                btnVoiceUnconsumed.isEnabled = false  // 禁用另一个模式按钮，防止同时开启两种模式
                tvStatus.text = "请说话...(已消费，不会自动回复)"
                Log.d(TAG, "Voice CONSUMED mode started")
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
                // 当前正在"未消费"录音，再次点击则停止
                resetToIdle()
                tvStatus.text = "已停止录入"
            } else {
                resetToIdle()
                recordMode = RecordMode.UNCONSUMED
                AgentCore.isDisablePlan = false        // 启用大模型自动规划（未消费模式下大模型需要处理语音内容）
                AgentCore.isMicrophoneMuted = false    // 打开麦克风开始接收语音
                btnVoiceUnconsumed.text = "停止录入(未消费)"
                btnVoiceConsumed.isEnabled = false     // 禁用另一个模式按钮
                tvStatus.text = "请说话...(未消费，大模型会自动回复)"
                Log.d(TAG, "Voice UNCONSUMED mode started")
            }
        }

        /**
         * 返回主页按钮：
         * 使用 FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP 启动 MainActivity，
         * 确保回到已有的 MainActivity 实例而非创建新实例，并关闭当前页面
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
     * 停止 TTS 播报并清除对话上下文，防止资源泄漏和状态残留
     */
    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        AgentCore.stopTTS()          // 停止正在进行的 TTS 播报
        AgentCore.clearContext()     // 清除对话上下文
        super.onDestroy()
    }
}
