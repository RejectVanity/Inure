package app.simple.inure.ui.viewers

import android.content.pm.PackageInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import app.simple.inure.R
import app.simple.inure.adapters.details.AdapterProviders
import app.simple.inure.decorations.popup.PopupMenuCallback
import app.simple.inure.decorations.views.CustomVerticalRecyclerView
import app.simple.inure.dialogs.miscellaneous.ErrorPopup
import app.simple.inure.dialogs.miscellaneous.ShellExecutorDialog
import app.simple.inure.extension.fragments.ScopedFragment
import app.simple.inure.model.ProviderInfoModel
import app.simple.inure.popups.viewers.PopupProvidersMenu
import app.simple.inure.ui.subviewers.ProviderInfo
import app.simple.inure.util.FragmentHelper
import app.simple.inure.viewmodels.factory.PackageInfoFactory
import app.simple.inure.viewmodels.viewers.ApkDataViewModel

class Providers : ScopedFragment() {

    private lateinit var recyclerView: CustomVerticalRecyclerView
    private lateinit var adapterProviders: AdapterProviders
    private lateinit var componentsViewModel: ApkDataViewModel
    private lateinit var packageInfoFactory: PackageInfoFactory

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_provider, container, false)

        recyclerView = view.findViewById(R.id.providers_recycler_view)
        packageInfo = requireArguments().getParcelable("application_info")!!
        packageInfoFactory = PackageInfoFactory(requireActivity().application, packageInfo)
        componentsViewModel = ViewModelProvider(this, packageInfoFactory).get(ApkDataViewModel::class.java)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        startPostponedEnterTransition()

        componentsViewModel.getProviders().observe(viewLifecycleOwner, {
            adapterProviders = AdapterProviders(it, packageInfo)
            recyclerView.adapter = adapterProviders

            adapterProviders.setOnProvidersCallbackListener(object : AdapterProviders.Companion.ProvidersCallbacks {
                override fun onProvidersClicked(providerInfoModel: ProviderInfoModel) {
                    clearExitTransition()
                    FragmentHelper.openFragment(requireActivity().supportFragmentManager,
                                                ProviderInfo.newInstance(providerInfoModel, packageInfo),
                                                "provider_info")
                }

                override fun onProvidersLongPressed(packageId: String, packageInfo: PackageInfo, icon: View, isComponentEnabled: Boolean, position: Int) {
                    val v = PopupProvidersMenu(icon, isComponentEnabled)

                    v.setOnMenuClickListener(object : PopupMenuCallback {
                        override fun onMenuItemClicked(source: String) {
                            when (source) {
                                getString(R.string.enable) -> {
                                    val shell = ShellExecutorDialog.newInstance("pm enable ${packageInfo.packageName}/$packageId")

                                    shell.setOnCommandResultListener(object : ShellExecutorDialog.Companion.CommandResultCallbacks {
                                        override fun onCommandExecuted(result: String) {
                                            if (result.contains("Done!")) {
                                                adapterProviders.notifyItemChanged(position)
                                            }
                                        }
                                    })

                                    shell.show(childFragmentManager, "shell_executor")
                                }
                                getString(R.string.disable) -> {
                                    val shell = ShellExecutorDialog.newInstance("pm disable ${packageInfo.packageName}/$packageId")

                                    shell.setOnCommandResultListener(object : ShellExecutorDialog.Companion.CommandResultCallbacks {
                                        override fun onCommandExecuted(result: String) {
                                            if (result.contains("Done!")) {
                                                adapterProviders.notifyItemChanged(position)
                                            }
                                        }
                                    })

                                    shell.show(childFragmentManager, "shell_executor")
                                }
                            }
                        }
                    })
                }
            })
        })

        componentsViewModel.getError().observe(viewLifecycleOwner, {
            val e = ErrorPopup.newInstance(it)
            e.show(childFragmentManager, "error_dialog")
            e.setOnErrorDialogCallbackListener(object : ErrorPopup.Companion.ErrorDialogCallbacks {
                override fun onDismiss() {
                    requireActivity().onBackPressed()
                }
            })
        })
    }

    companion object {
        fun newInstance(applicationInfo: PackageInfo): Providers {
            val args = Bundle()
            args.putParcelable("application_info", applicationInfo)
            val fragment = Providers()
            fragment.arguments = args
            return fragment
        }
    }
}
