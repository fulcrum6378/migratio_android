package ir.mahdiparastesh.migratio.misc

import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

open class AnyViewHolder<B>(val b: B) : RecyclerView.ViewHolder(b.root) where B : ViewBinding
