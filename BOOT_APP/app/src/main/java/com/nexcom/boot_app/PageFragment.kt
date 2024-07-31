package com.nexcom.boot_app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.widget.GridView

class PageFragment : Fragment() {

    companion object {
        private const val ARG_APP_INFO_LIST = "app_info_list"

        fun newInstance(appInfoList: ArrayList<AppInfo>): PageFragment {
            val fragment = PageFragment()
            val args = Bundle()
            args.putParcelableArrayList(ARG_APP_INFO_LIST, appInfoList)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val gridView: GridView = view.findViewById(R.id.gridView)
        val appInfoList = arguments?.getParcelableArrayList<AppInfo>(ARG_APP_INFO_LIST) ?: return

        val adapter = GridAdapter(requireContext(), appInfoList)
        gridView.adapter = adapter
    }
}
