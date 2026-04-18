package com.studyassistant.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.studyassistant.domain.model.Quiz
import java.text.SimpleDateFormat
import java.util.*

/**
 * RecyclerView Adapter for displaying Quiz items in a list
 * Uses ListAdapter with DiffUtil for efficient list updates
 */
class QuizAdapter(
    private val onOpenAttempt: (Quiz) -> Unit,
    private val onTakeQuiz: (Quiz) -> Unit
) : ListAdapter<Quiz, QuizAdapter.QuizViewHolder>(QuizDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuizViewHolder {
        // Inflate the quiz card layout
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return QuizViewHolder(view, onOpenAttempt, onTakeQuiz)
    }

    override fun onBindViewHolder(holder: QuizViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder for Quiz items
     */
    class QuizViewHolder(
        private val itemView: android.view.View,
        private val onOpenAttempt: (Quiz) -> Unit,
        private val onTakeQuiz: (Quiz) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val titleView: TextView? = itemView.findViewById(android.R.id.text1)

        fun bind(quiz: Quiz) {
            // Format the date
            val dateFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
            val formattedDate = dateFormat.format(quiz.createdAt)

            // Set quiz information using friendly title if available
            val title = if (quiz.title.isNotBlank()) quiz.title else "Quiz - ${formattedDate}"
            titleView?.text = "$title\n" +
                    "Score: ${quiz.score}/${quiz.questions.size}\n" +
                    "${quiz.questions.size} Questions\n" +
                    (if (quiz.completed) "✓ Completed" else "⏱ In Progress")

            // Set click listener for the entire item
            itemView.setOnClickListener {
                if (quiz.completed) {
                    onOpenAttempt(quiz)
                } else {
                    onTakeQuiz(quiz)
                }
            }
        }
    }

    /**
     * DiffUtil callback for efficient list updates
     */
    class QuizDiffCallback : DiffUtil.ItemCallback<Quiz>() {
        override fun areItemsTheSame(oldItem: Quiz, newItem: Quiz): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Quiz, newItem: Quiz): Boolean {
            return oldItem == newItem
        }
    }
}
