/*
    This file is part of PCAP Remote.

    PCAP Remote is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    PCAP Remote is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with PCAP Remote. If not, see <http://www.gnu.org/licenses/>.

    Copyright 2019 by Andrey Egorov
*/

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
