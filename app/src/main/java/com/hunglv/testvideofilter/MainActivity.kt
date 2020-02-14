package com.hunglv.testvideofilter

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import cn.ezandroid.ezfilter.EZFilter
import cn.ezandroid.ezfilter.core.FilterRender
import cn.ezandroid.ezfilter.core.RenderPipeline
import cn.ezandroid.ezfilter.extra.IAdjustable
import cn.ezandroid.ezfilter.video.VideoBuilder
import cn.ezandroid.ezfilter.video.VideoInput
import cn.ezandroid.ezfilter.video.player.IMediaPlayer
import com.hunglv.testvideofilter.render.BlendOverlayRender
import com.hunglv.testvideofilter.render.LookupRender
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        private const val PATH_VIDEO = "/storage/emulated/0/Vintage Camera/cut.mp4"
    }
    private var currentFilter = FilterRender()
    private var mRenderPipeline: RenderPipeline? = null
    private var positionPlaying = 0
    //
    private var mUri: Uri? = null
    // quản lý audio
    private var mAudioManager: AudioManager? = null
    private lateinit var request: AudioFocusRequest
    //
    private var mMediaPlayer: IMediaPlayer? = null
    //
    private var handlerIsRunning = false
    private val mHandler = Handler()
    private val mRunnable = object : Runnable {
        override fun run() {
            if (mMediaPlayer != null) {
                val lastPos = positionPlaying
                positionPlaying = try {
                    mMediaPlayer!!.currentPosition
                } catch (e: IllegalStateException) {
                    lastPos
                }
                tv_current_position.text = getTimeFormatted(positionPlaying.toLong())
                seekbar_media.progress = positionPlaying
            }
            mHandler.postDelayed(this, 100)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            1
        )
        if (mAudioManager == null) {
            mAudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        }
        MediaScannerConnection.scanFile(
            this,
            arrayOf(File(PATH_VIDEO).absolutePath),
            null
        ) { path, uri ->
            Log.i("onScanCompleted", path)
            mUri = uri
            mRenderPipeline = EZFilter.input(mUri)
                .setLoop(true)
                .setPreparedListener {
                    mMediaPlayer = it
                    tv_duration.text = getTimeFormatted(it.duration.toLong())
                    tv_current_position.text = getString(R.string.default_position)
                    seekbar_media.max = it.duration
                    seekbar_media.progress = 0
                    ivPlayPause.setImageResource(R.drawable.ic_pause)
                    registerCurrentPositionMediaPlayer()
                }.into(mRenderView)
        }
        addEvents()
    }

    private fun registerCurrentPositionMediaPlayer() {
        if (!handlerIsRunning) {
            handlerIsRunning = true
            mHandler.post(mRunnable)
        }
    }

    private fun getTimeFormatted(time: Long): String {
        val hour = TimeUnit.MILLISECONDS.toHours(time)
        val mm = TimeUnit.MILLISECONDS.toMinutes(time) % 60
        val ss = TimeUnit.MILLISECONDS.toSeconds(time) % 60
        return if (hour == 0L) {
            String.format(Locale.US, "%02d:%02d", mm, ss)
        } else {
            String.format(Locale.US, "%02d:%02d:%02d", hour, mm, ss)
        }

    }

    private fun addEvents() {
        seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                tv_progress.text = p1.toString()
                changeValueFilter(p1)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }

        })
        btn_normal.setOnClickListener(this)
        btn_vintage.setOnClickListener(this)
        btn_portrait.setOnClickListener(this)
        btn_dust.setOnClickListener(this)
        btn_vignette.setOnClickListener(this)
        btn_mask.setOnClickListener(this)
        btn_light_leak.setOnClickListener(this)
        btn_border.setOnClickListener(this)
        btn_brightness.setOnClickListener(this)
        btn_contrast.setOnClickListener(this)
        btn_saturation.setOnClickListener(this)
        btn_high_light.setOnClickListener(this)
        btn_shadow.setOnClickListener(this)
        btn_date.setOnClickListener(this)

        layout_video.setOnClickListener {
            showHideMediaButton()
        }

        ivPlayPause.setOnClickListener {
            if (mMediaPlayer != null) {
                if (mMediaPlayer!!.isPlaying) {
                    ivPlayPause.setImageResource(R.drawable.ic_play)
                    mMediaPlayer!!.pause()
                } else {
                    ivPlayPause.setImageResource(R.drawable.ic_pause)
                    mMediaPlayer!!.start()
                }
            }
        }
        seekbar_media.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if (p2) {
                    mMediaPlayer?.seekTo(p1)
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }
        })
    }

    private fun showHideMediaButton() {
        if (ivPlayPause.visibility == View.VISIBLE) {
            ivPlayPause.visibility = View.INVISIBLE
            tv_current_position.visibility = View.INVISIBLE
            tv_duration.visibility = View.INVISIBLE
            seekbar_media.visibility = View.INVISIBLE
        } else {
            ivPlayPause.visibility = View.VISIBLE
            tv_current_position.visibility = View.VISIBLE
            tv_duration.visibility = View.VISIBLE
            seekbar_media.visibility = View.VISIBLE
        }
    }

    private fun changeValueFilter(p1: Int) {
        if (currentFilter is LookupRender){
            (currentFilter as LookupRender).adjust(p1/100f)
        }
        if (currentFilter is BlendOverlayRender){
            (currentFilter as BlendOverlayRender).adjust(p1/100f)
        }
    }

    override fun onClick(p0: View?) {
        currentFilter = when (p0?.id) {
            R.id.btn_vintage -> {
                LookupRender(this, R.drawable.v1)
            }
            R.id.btn_portrait -> {
                LookupRender(this, R.drawable.p3)
            }
            R.id.btn_dust -> {
                BlendOverlayRender(this, R.drawable.dust)
            }
            R.id.btn_vignette -> {
                FilterRender()
            }
            R.id.btn_mask -> {
                BlendOverlayRender(this, R.drawable.mask)
            }
            R.id.btn_light_leak -> {
                BlendOverlayRender(this, R.drawable.lightleak)
            }
            R.id.btn_border -> {
                FilterRender()
            }
            R.id.btn_brightness -> {
                FilterRender()
            }
            R.id.btn_contrast -> {
                FilterRender()
            }
            R.id.btn_shadow -> {
                FilterRender()
            }
            R.id.btn_date -> {
                FilterRender()
            }
            else -> {
                FilterRender()
            }
        }
        mRenderPipeline = EZFilter.input(mUri)
            .addFilter(currentFilter)
            .setLoop(true)
            .setPreparedListener {
                mMediaPlayer = it
                tv_duration.text = getTimeFormatted(it.duration.toLong())
                tv_current_position.text = getString(R.string.default_position)
                seekbar_media.max = it.duration
                seekbar_media.progress = 0
                ivPlayPause.setImageResource(R.drawable.ic_pause)
                registerCurrentPositionMediaPlayer()
            }.into(mRenderView)
    }


    override fun onStop() {
        super.onStop()
        abandonMediaFocus()
        mMediaPlayer?.release()
    }

    override fun onStart() {
        super.onStart()
        requestAudioFocus()
    }

    override fun onResume() {
        super.onResume()
        playMedia()

    }

    private fun playMedia() {
        mMediaPlayer?.seekTo(positionPlaying)
        if (mRenderPipeline != null) {
            (mRenderPipeline!!.startPointRender as VideoInput).start()
        }
        registerCurrentPositionMediaPlayer()
        requestAudioFocus()
    }

    override fun onPause() {
        super.onPause()
        pauseMedia()

    }

    override fun onDestroy() {

        super.onDestroy()
    }

    private fun pauseMedia() {
        if (mRenderPipeline != null) {
            positionPlaying =
                (mRenderPipeline!!.startPointRender as VideoInput).mediaPlayer.currentPosition
            (mRenderPipeline!!.startPointRender as VideoInput).pause()
        }
        abandonMediaFocus()
        if (handlerIsRunning) {
            handlerIsRunning = false
            mHandler.removeCallbacks(mRunnable)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (grantResults.isEmpty()) { // Empty results are triggered if a permission is requested while another request was already
// pending and can be safely ignored in this case.
            return
        }
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d("MainActivity", "onRequestPermissionsResult: OK")
        }
    }

    private fun requestAudioFocus(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //
            request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(afListener)
                .build()
            //
            mAudioManager!!.requestAudioFocus(
                request
            )
        } else {
            @Suppress("DEPRECATION")
            mAudioManager!!.requestAudioFocus(
                afListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    private fun abandonMediaFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mAudioManager!!.abandonAudioFocusRequest(request)
        } else {
            @Suppress("DEPRECATION")
            mAudioManager!!.abandonAudioFocus(afListener)
        }
    }

    private val afListener = AudioManager.OnAudioFocusChangeListener { i: Int ->
        when (i) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                //Âm thanh đã được khôi phục trở lại
                // -> có thể play media


            }
            AudioManager.AUDIOFOCUS_LOSS -> {

                //abandoneMediaFocus()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                //Tạm thời mất âm thanh, tạm ngưng chờ lấy lại được âm thanh

            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                //Tạm thời mất âm thanh, vẫn có thể giảm âm lượng và play hoặc tạm ngưng âm thanh
                //lowerVolume() ||  pause()

            }
        }
    }

}
