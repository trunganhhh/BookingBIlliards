package com.trunganh.bookingbilliards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.trunganh.bookingbilliards.databinding.FragmentHomeBinding

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBookNowButton()
        setupGoogleMaps()
    }

    private fun setupBookNowButton() {
        binding.btnBookNow.setOnClickListener {
            // Chuyển đến trang danh sách bàn
            findNavController().navigate(R.id.navigation_tables)
        }
    }

    private fun setupGoogleMaps() {
        binding.webViewMap.apply {
            settings.javaScriptEnabled = true
            webViewClient = WebViewClient()
            
            // Thay thế YOUR_LATITUDE và YOUR_LONGITUDE bằng tọa độ thực tế của quán
            val mapUrl = """
                <html>
                    <body style="margin:0;padding:0;">
                        <iframe 
                            width="100%" 
                            height="100%" 
                            frameborder="0" 
                            style="border:0" 
                            src="https://www.google.com/maps/embed/v1/place?key=YOUR_API_KEY&q=YOUR_LATITUDE,YOUR_LONGITUDE&zoom=15" 
                            allowfullscreen>
                        </iframe>
                    </body>
                </html>
            """.trimIndent()
            
            loadData(mapUrl, "text/html", "utf-8")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
