package com.andruid.magic.helpfulsense.model

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.andruid.magic.helpfulsense.R
import com.andruid.magic.helpfulsense.database.entity.Contact
import com.andruid.magic.helpfulsense.ui.viewholder.ContactViewHolder
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible

data class ContactHolder(
        val contact: Contact
) : AbstractFlexibleItem<ContactViewHolder>() {
    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?,
                                holder: ContactViewHolder?, position: Int, payloads: MutableList<Any>?) {
        holder?.bind((adapter?.getItem(position) as ContactHolder).contact)
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?) =
            ContactViewHolder(view, adapter)

    override fun getLayoutRes() = R.layout.layout_contact

    override fun isDraggable() = true

    override fun isSwipeable() = true
}

fun Contact.toContactHolder() = ContactHolder(this)