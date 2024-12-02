package com.merge.awadh.activity.scan.fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.merge.awadh.R
import com.merge.awadh.databinding.FragmentGame1OpeningPageBinding
class Game1OpeningPageFragment : Fragment() {
    private lateinit var binding: FragmentGame1OpeningPageBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentGame1OpeningPageBinding.inflate(inflater, container, false)

        binding.playButton.setOnClickListener {
            findNavController().navigate(R.id.action_game1OpeningPageFragment_to_deviceSelectionFragment)
        }

        return binding.root
    }
}
