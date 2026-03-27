package com.kmq.kmqsamplebody.tts

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ainirobot.agent.AgentCore
import com.ainirobot.agent.PageAgent
import com.ainirobot.agent.TTSCallback
import com.kmq.kmqsamplebody.MainActivity
import com.kmq.kmqsamplebody.R

/**
 * TTS 流式播报演示页面
 *
 * 本页面模拟流式 TTS 输出效果：点击播报时，文本区域从空白开始逐字显示，
 * 同时调用 TTS 引擎进行语音播报，视觉上模拟大模型流式生成文本的效果。
 */
class TtsStreamActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "TtsStreamActivity"

        /** 每次追加的字符数 */
        private const val CHUNK_SIZE = 3

        /** 每次追加的时间间隔（毫秒），值越小文字出现越快 */
        private const val STREAM_INTERVAL_MS = 60L

        /** 待播报的公司介绍文本 */
        private const val COMPANY_TEXT = """发展历程

        公司2015年成立，初期获得浙江省科技创投 800万人民币风险投资，公司投后估值8000万人民币。之后陆续获得两家上市公司（浙江永强和银江资本），一家基金公司（芳晟基金）共计数千万人民币投资，投后估值2亿人民币。2018年5月公司和南京政府合资成立南京研发中心，其中南京政府产业引导基金入资1500万人民币，南京研发中心投后估值1.5亿人民币。2018年10月成立北京销售公司。2020年9月银川市政府招商引资，设立西北人工智能产业基地，为行业提供一站式机器人产品解决方案，打造机器人行业解决方案全新品牌"科梦奇"。2022年3月联合复旦大学，成立科梦奇机器人（上海）研发中心。

市场拓展

        2016年机器人第一代原型机亮相美国CES展会，后续陆续亮相于日本机器人展会、法国NOROBOT展会、德国工业展、柏林消费电子展IFA等海外知名展会。助力公司成为了国内最早出口全球的机器人公司之一，公司产品远销欧洲、日本、美国等13个国家，建立起全球产品销售网络。

公司优势

        技术优势：同行中核心技术最全，唯一同时拥有麦克风阵列、多传感器交互、高精度底盘、四足机器人、低速无人驾驶等自主研发技术。

        应用方案与合作伙伴优势：依托于浙大、交大、华科、复旦等各大高校和部分机器人研发中心资源。具备快速针对市场需求变化提供解决方案的能力。

        团队优势：拥有美国麻省理工机器视觉专家、德国航天所机器人工程师、中科院、军队等一批海外背景+军方背景的合伙人。"""
    }

    /** PageAgent 用于管理当前页面与机器人 Agent 框架的交互 */
    private lateinit var pageAgent: PageAgent

    /** 主线程 Handler，用于定时追加文字实现流式效果 */
    private val handler = Handler(Looper.getMainLooper())

    /** 流式文字输出的定时任务引用，用于停止时取消 */
    private var streamRunnable: Runnable? = null

    /** 当前已显示的字符索引，标记流式输出进度 */
    private var currentIndex = 0

    /** 是否正在进行流式输出 */
    private var isStreaming = false

    /** 处理后的完整文本 */
    private val fullText by lazy { COMPANY_TEXT.trimIndent() }

    /** 文本展示区域 */
    private lateinit var tvContent: TextView

    /** 文本滚动容器 */
    private lateinit var scrollView: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tts_stream)

        // 初始化 PageAgent，绑定当前 Activity 的生命周期
        pageAgent = PageAgent(this)

        // 获取界面控件引用
        scrollView = findViewById(R.id.sv_stream_content)
        tvContent = findViewById(R.id.tv_stream_content)
        val btnSpeakShow = findViewById<Button>(R.id.btn_stream_speak_show)
        val btnSpeakHide = findViewById<Button>(R.id.btn_stream_speak_hide)
        val btnStop = findViewById<Button>(R.id.btn_stream_stop)
        val tvStatus = findViewById<TextView>(R.id.tv_stream_status)
        val btnBackHome = findViewById<Button>(R.id.btn_back_home)

        // 初始显示完整文本
        tvContent.text = fullText

        /**
         * 播报按钮（显示语音条）：
         * 启用语音条 UI，同时启动流式文字动画和 TTS 播报
         */
        btnSpeakShow.setOnClickListener {
            AgentCore.isEnableVoiceBar = true
            tvStatus.text = "流式播报中(显示语音条)..."
            Log.d(TAG, "Start stream TTS with voice bar")
            startStreamText()
            AgentCore.tts(fullText, 180000, object : TTSCallback {
                override fun onTaskEnd(status: Int, result: String?) {
                    runOnUiThread {
                        tvStatus.text = if (status == 1) "播报完成" else "播报失败: $result"
                    }
                }
            })
        }

        /**
         * 播报按钮（不显示语音条）：
         * 隐藏语音条 UI，同时启动流式文字动画和 TTS 播报
         */
        btnSpeakHide.setOnClickListener {
            AgentCore.isEnableVoiceBar = false
            tvStatus.text = "流式播报中(不显示语音条)..."
            Log.d(TAG, "Start stream TTS without voice bar")
            startStreamText()
            AgentCore.tts(fullText, 180000, object : TTSCallback {
                override fun onTaskEnd(status: Int, result: String?) {
                    runOnUiThread {
                        tvStatus.text = if (status == 1) "播报完成" else "播报失败: $result"
                    }
                }
            })
        }

        /**
         * 停止按钮：停止流式文字动画和 TTS 播报，恢复显示完整文本
         */
        btnStop.setOnClickListener {
            Log.d(TAG, "Stop button clicked")
            stopStreamText()
            tvContent.text = fullText
            AgentCore.stopTTS()
            AgentCore.clearContext()
            tvStatus.text = "已停止"
        }

        /**
         * 返回首页按钮
         */
        btnBackHome.setOnClickListener {
            stopStreamText()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }

    /**
     * 启动流式文字输出动画
     * 清空文本区域，然后每隔 [STREAM_INTERVAL_MS] 毫秒追加 [CHUNK_SIZE] 个字符，
     * 同时自动滚动到底部，模拟大模型流式生成文本的视觉效果。
     */
    private fun startStreamText() {
        stopStreamText()
        currentIndex = 0
        isStreaming = true
        tvContent.text = ""

        val runnable = object : Runnable {
            override fun run() {
                if (!isStreaming) return
                val end = (currentIndex + CHUNK_SIZE).coerceAtMost(fullText.length)
                tvContent.text = fullText.substring(0, end)
                // 自动滚动到底部，跟随最新文字
                scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
                currentIndex = end
                if (currentIndex < fullText.length) {
                    handler.postDelayed(this, STREAM_INTERVAL_MS)
                } else {
                    isStreaming = false
                }
            }
        }
        streamRunnable = runnable
        handler.post(runnable)
    }

    /**
     * 停止流式文字输出动画
     */
    private fun stopStreamText() {
        isStreaming = false
        streamRunnable?.let { handler.removeCallbacks(it) }
        streamRunnable = null
    }

    /**
     * Activity 销毁时的清理工作：
     * 取消流式动画、停止 TTS 播报并清除对话上下文
     */
    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        stopStreamText()
        AgentCore.stopTTS()
        AgentCore.clearContext()
        super.onDestroy()
    }
}
