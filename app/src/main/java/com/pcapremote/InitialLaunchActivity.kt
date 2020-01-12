package com.pcapremote

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_initial_launch.*


class InitialLaunchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_initial_launch)
        supportActionBar?.hide()

        tvOpenTutorial.setOnClickListener {
            MiscUtils.startHelpActivity(this)
            finish()
        }

        tvSkip.setOnClickListener {
            finish()
        }

//        tvNext.setOnClickListener {
//            setContentView(R.layout.activity_initial_launch_dark_theme)
//
//            tvOpenTutorial.setOnClickListener {
//                MiscUtils.startHelpActivity(this)
//                finish()
//            }
//
//            tvSkip.setOnClickListener {
//                finish()
//            }
//        }
    }
}
