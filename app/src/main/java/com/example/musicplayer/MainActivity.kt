package com.example.musicplayer

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.musicplayer.databinding.ActivityMainBinding
import java.io.File
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var mp: MediaPlayer
    private var playList: ArrayList<HashMap<String, String>>? = null
    private var songsFound = 0
    private var currentSongIndex = 0

    private lateinit var runnable: Runnable
    private var handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mp = MediaPlayer()

        scanSongs()
        scanButton()

        seekBarConfig()
        musicPlayerControls()
    }

    private fun scanSongs() {
        binding.reloadBTN.isEnabled = false
        binding.reloadBTN.isClickable = false

        val rootPath = Environment.getExternalStorageDirectory().absolutePath
        playList = getPlayList(rootPath)
        songsFound = playList?.size ?: 0

        var msg = "Found $songsFound MP3 files!"
        if (songsFound == 0)
            msg += " (Please check storage permissions!)"

        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

        binding.playBTN.setImageResource(R.drawable.ic_baseline_play_arrow)
        currentSongIndex = 0
        loadSong(currentSongIndex)
        updateSongsCounter()

        binding.reloadBTN.isEnabled = true
        binding.reloadBTN.isClickable = true
    }

    private fun getPlayList(rootPath: String): ArrayList<HashMap<String, String>>? {
        var fileList = ArrayList<HashMap<String, String>>()
        try {
            val rootFolder = File(rootPath)
            val files = rootFolder.listFiles() ?: null
            if (files != null){
                for (file in files){
                    if (file.isDirectory){
                        getPlayList(file.absolutePath)?.let { fileList.addAll(it) }
                    } else if(file.name.endsWith(".mp3")){
                        val song = HashMap<String, String>()
                        song["file_path"] = file.absolutePath
                        song["file_name"] = file.name
                        fileList.add(song)
                    }
                }
            }
            return fileList

        } catch (e: Exception) {
            return null
        }
    }

    private fun updateSongsCounter() {
        val songId = if (currentSongIndex in 0 until songsFound) currentSongIndex + 1 else 0

        binding.songsCounterTV.text = getString(R.string.song_counter, songId, songsFound)
    }

    private fun scanButton() {
        binding.reloadBTN.setOnClickListener {
            scanSongs()
        }
    }

    private fun loadSong(songId: Int){
        if (songId in 0 until songsFound){
            mp.stop()
            mp.reset()
            mp.setDataSource(playList?.get(currentSongIndex)?.get("file_path"))
            mp.prepare()

            mp.seekTo(0)
            updateSongInfo()
            updateSongsCounter()
        }
    }

    private fun updateSongInfo() {
        if (currentSongIndex in 0 until songsFound){
            binding.seekBarSB.progress = mp.currentPosition
            binding.seekBarSB.max = mp.duration
            binding.filenameTV.text = playList?.get(currentSongIndex)?.get("file_name") ?: ""
            binding.timePassedTV.text = durationToString(mp.currentPosition)
            binding.timeMaxTV.text = durationToString(mp.duration)
        } else {
            binding.filenameTV.text = ""
            binding.timePassedTV.text = durationToString(0)
            binding.timeMaxTV.text = durationToString(0)
        }
    }

    private fun durationToString(duration: Int): String {
        return String.format(
            "%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(duration.toLong()),
            TimeUnit.MILLISECONDS.toSeconds(duration.toLong()) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration.toLong()))
        )
    }

    private fun seekBarConfig() {
        binding.seekBarSB.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, pos: Int, changed: Boolean) {
                if (changed){
                    mp.seekTo(pos)
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }

        })

        runnable = Runnable {
            if (currentSongIndex in 0 until songsFound){
                updateSongTime()
                if (mp.currentPosition >= mp.duration)
                    nextSong()
            }
            handler.postDelayed(runnable, 50)
        }
        handler.postDelayed(runnable, 50)
    }

    private fun updateSongTime() {
        binding.seekBarSB.progress = mp.currentPosition
        binding.timePassedTV.text = durationToString(mp.currentPosition)
    }

    private fun musicPlayerControls() {
        // PLAY / PAUSE
        binding.playBTN.setOnClickListener {
            if (currentSongIndex in 0 until songsFound){
                if (!mp.isPlaying){
                    mp.start()
                    binding.playBTN.setImageResource(R.drawable.ic_baseline_pause)
                } else {
                    mp.pause()
                    binding.playBTN.setImageResource(R.drawable.ic_baseline_play_arrow)
                }

            }
        }

        // STOP
        binding.stopBTN.setOnClickListener {
            if (currentSongIndex in 0 until songsFound){
                mp.stop()
                mp.prepare()
                mp.seekTo(0)
                binding.playBTN.setImageResource(R.drawable.ic_baseline_play_arrow)
                updateSongTime()
            }
        }

        // BACKWARD 10 SEC
        binding.backwardBTN.setOnClickListener {
            if (currentSongIndex in 0 until songsFound){
                val msToSkip = 10 * 1000

                mp.seekTo(mp.currentPosition - msToSkip)
                updateSongTime()
            }
        }

        // FORWARD 10 SEC
        binding.forwardBTN.setOnClickListener {
            if (currentSongIndex in 0 until songsFound){
                val msToSkip = 10 * 1000

                mp.seekTo(mp.currentPosition + msToSkip)
                updateSongTime()
            }
        }

        // PREVIOUS SONG
        binding.previousBTN.setOnClickListener {
            if (currentSongIndex in 0 until songsFound){
                previousSong()
            }
        }

        // NEXT SONG
        binding.nextBTN.setOnClickListener {
            if (currentSongIndex in 0 until songsFound){
                nextSong()
            }
        }
    }

    private fun previousSong() {
        currentSongIndex = if (currentSongIndex > 0) currentSongIndex - 1 else songsFound - 1
        loadCurrentIndexSong()
    }

    private fun nextSong() {
        currentSongIndex = if (currentSongIndex >= songsFound - 1) 0 else currentSongIndex + 1
        loadCurrentIndexSong()
    }

    private fun loadCurrentIndexSong(){
        val wasPlaying = mp.isPlaying

        loadSong(currentSongIndex)

        if (wasPlaying)
            mp.start()
    }

}