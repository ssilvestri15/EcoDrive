package com.silvered.ecodrive.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntegerRes
import com.google.android.material.button.MaterialButton
import com.silvered.ecodrive.R
import com.silvered.ecodrive.activity.OnBoardingActivity

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val LAYOUT = "layout"

/**
 * A simple [Fragment] subclass.
 * Use the [OnBoardFirstFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class OnBoardFirstFragment : Fragment() {

    private var layout: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments == null) {
            layout = R.layout.fragment_on_board_first
            return
        }

        layout = arguments!!.getInt(LAYOUT, R.layout.fragment_on_board_first)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        val view = inflater.inflate(layout!!, container, false)

        val button = view.findViewById<MaterialButton>(R.id.materialButton)

        button.setOnClickListener {
            when(layout) {

                R.layout.fragment_on_board_third -> {
                    val activity = activity as OnBoardingActivity
                    activity.endPage()
                }
                else -> {
                    val activity = activity as OnBoardingActivity
                    activity.goNext()
                }

            }
        }

        return view
    }

    companion object {

        @JvmStatic
        fun newInstance(@IntegerRes layout: Int) =
            OnBoardFirstFragment().apply {
                arguments = Bundle().apply {
                    putInt(LAYOUT, layout)
                }
            }
    }
}