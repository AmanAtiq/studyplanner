package com.studyassistant.ui.components

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton

/**
 * Simple ViewPager2 component with pages created programmatically.
 * Each page contains a title TextView and an Android MaterialButton.
 */
@Composable
fun ViewPager2Component(
    pages: List<String>,
    modifier: Modifier = Modifier
) {
    AndroidView(factory = { context ->
        createViewPager(context, pages)
    }, modifier = modifier)
}

private fun createViewPager(context: Context, pages: List<String>): ViewPager2 {
    val viewPager = ViewPager2(context)
    viewPager.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    viewPager.adapter = object : RecyclerView.Adapter<PagerViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagerViewHolder {
            // create a simple vertical LinearLayout with a TextView and a MaterialButton
            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setPadding(24, 24, 24, 24)
                gravity = Gravity.CENTER
            }

            val tv = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.CENTER
                }
                textSize = 18f
            }

            val btn = MaterialButton(context).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = 12
                    gravity = Gravity.CENTER
                }
                text = context.getString(com.studyassistant.R.string.open)
            }

            container.addView(tv)
            container.addView(btn)

            return PagerViewHolder(container, tv, btn)
        }

        override fun getItemCount(): Int = pages.size

        override fun onBindViewHolder(holder: PagerViewHolder, position: Int) {
            holder.title.text = pages[position]
            holder.button.setOnClickListener {
                // show a simple Toast with the page title
                Toast.makeText(context, pages[position], Toast.LENGTH_SHORT).show()
            }
        }
    }
    return viewPager
}

private class PagerViewHolder(itemView: View, val title: TextView, val button: MaterialButton) : RecyclerView.ViewHolder(itemView)
