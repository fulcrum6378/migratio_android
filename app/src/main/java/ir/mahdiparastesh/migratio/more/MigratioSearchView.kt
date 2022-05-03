package ir.mahdiparastesh.migratio.more

import android.content.Context
import android.view.ContextThemeWrapper
import androidx.appcompat.widget.SearchView
import ir.mahdiparastesh.migratio.R

class MigratioSearchView(context: Context) :
    SearchView(ContextThemeWrapper(context, R.style.SearchViewTheme))
