package ir.mahdiparastesh.migratio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ir.mahdiparastesh.migratio.data.Country
import ir.mahdiparastesh.migratio.data.Criterion
import ir.mahdiparastesh.migratio.data.MyCriterion

class Model : ViewModel() {
    var gotCountries: List<Country>? = null
    var gotCriteria: List<Criterion>? = null
    var myCountries: MutableSet<String>? = null
    var myCriteria: ArrayList<MyCriterion>? = null
    var loaded = false
    var showingHelp = false
    var showingAbout = false

    @Suppress("UNCHECKED_CAST")
    class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(Model::class.java)) {
                val key = "Model"
                return if (hashMapViewModel.containsKey(key)) getViewModel(key) as T
                else {
                    addViewModel(key, Model())
                    getViewModel(key) as T
                }
            }
            throw IllegalArgumentException("Unknown Model class")
        }

        companion object {
            val hashMapViewModel = HashMap<String, ViewModel>()

            fun addViewModel(key: String, viewModel: ViewModel) =
                hashMapViewModel.put(key, viewModel)

            fun getViewModel(key: String): ViewModel? = hashMapViewModel[key]
        }
    }
}
