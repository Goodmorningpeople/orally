package com.example.orally

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.collections.forEach


@Composable
fun NotesContent(modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf<List<DocumentSnapshot>>(emptyList()) }
    var deletedNotes by remember { mutableStateOf<List<DocumentSnapshot>>(emptyList()) }
    var selectedNote by remember { mutableStateOf<DocumentSnapshot?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var showDeleted by remember { mutableStateOf(false) }

    val db = FirebaseFirestore.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid

    LaunchedEffect(Unit) {
        visible = true
        if (uid != null) {
            val userDoc = db.collection("users").document(uid)

            userDoc.collection("notes")
                .addSnapshotListener { value, _ ->
                    if (value != null) notes = value.documents
                }

            userDoc.collection("recently_deleted")
                .addSnapshotListener { value, _ ->
                    if (value != null) deletedNotes = value.documents
                }
        }
    }

    AnimatedContent(
        targetState = showEditor,
        transitionSpec = {
            slideInHorizontally(
                initialOffsetX = { if (targetState) it else -it },
                animationSpec = tween(500, easing = EaseInOutCubic)
            ) + fadeIn(animationSpec = tween(500)) togetherWith
                    slideOutHorizontally(
                        targetOffsetX = { if (targetState) -it else it },
                        animationSpec = tween(500, easing = EaseInOutCubic)
                    ) + fadeOut(animationSpec = tween(300))
        },
        label = "notes_transition"
    ) { isEditing ->
        if (isEditing) {
            NoteEditor(
                note = selectedNote,
                onSave = { title, content ->
                    if (uid != null) {
                        val collection = db.collection("users").document(uid).collection("notes")
                        if (selectedNote == null) {
                            collection.add(mapOf("title" to title, "content" to content))
                        } else {
                            collection.document(selectedNote!!.id).update(
                                mapOf("title" to title, "content" to content)
                            )
                        }
                    }
                    selectedNote = null
                    showEditor = false
                },
                onCancel = {
                    selectedNote = null
                    showEditor = false
                }
            )
        } else {
            NotesListView(
                notes = notes,
                deletedNotes = deletedNotes,
                visible = visible,
                showDeleted = showDeleted,
                onToggleDeleted = { showDeleted = !showDeleted },
                onAddNote = {
                    selectedNote = null
                    showEditor = true
                },
                onEditNote = {
                    selectedNote = it
                    showEditor = true
                },
                onDeleteNote = { note ->
                    uid?.let {
                        val userDoc = db.collection("users").document(it)
                        val noteData = note.data ?: return@let
                        userDoc.collection("recently_deleted").document(note.id).set(noteData)
                        userDoc.collection("notes").document(note.id).delete()
                    }
                },
                onRestoreNote = { note ->
                    uid?.let {
                        val userDoc = db.collection("users").document(it)
                        val noteData = note.data ?: return@let
                        userDoc.collection("notes").document(note.id).set(noteData)
                        userDoc.collection("recently_deleted").document(note.id).delete()
                    }
                },
                modifier = modifier
            )
        }
    }
}

@Composable
fun NoteEditor(
    note: DocumentSnapshot?,
    onSave: (String, String) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf(note?.getString("title") ?: "") }
    var content by remember { mutableStateOf(note?.getString("content") ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (note == null) "New Note 📝" else "Edit Note 🖊️",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("Content") },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            maxLines = 10,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(onClick = onCancel) {
                Text("Cancel")
            }
            Button(onClick = {
                if (title.isNotBlank() || content.isNotBlank()) {
                    onSave(title.trim(), content.trim())
                }
            }) {
                Text("Save")
            }
        }
    }
}

@Composable
fun NotesListView(
    notes: List<DocumentSnapshot>,
    deletedNotes: List<DocumentSnapshot>,
    visible: Boolean,
    showDeleted: Boolean,
    onToggleDeleted: () -> Unit,
    onAddNote: () -> Unit,
    onEditNote: (DocumentSnapshot) -> Unit,
    onDeleteNote: (DocumentSnapshot) -> Unit,
    onRestoreNote: (DocumentSnapshot) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(visible) {
            Text("📝", style = MaterialTheme.typography.displayLarge)
        }

        Spacer(Modifier.height(16.dp))

        AnimatedVisibility(visible) {
            Text("Your Notes", style = MaterialTheme.typography.headlineMedium)
        }

        Spacer(Modifier.height(24.dp))

        notes.forEach { note ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Text(text = note.getString("title") ?: "Untitled", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(text = note.getString("content") ?: "", style = MaterialTheme.typography.bodyMedium)

                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { onEditNote(note) }) {
                            Text("Edit")
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { onDeleteNote(note) }) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        AnimatedVisibility(visible) {
            Button(
                onClick = onAddNote,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("➕ New Note")
            }
        }

        Spacer(Modifier.height(24.dp))

        AnimatedVisibility(visible) {
            OutlinedButton(
                onClick = onToggleDeleted,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (showDeleted) "Hide Recently Deleted" else "Show Recently Deleted")
            }
        }

        Spacer(Modifier.height(16.dp))

        AnimatedVisibility(visible && showDeleted && deletedNotes.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Recently Deleted", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                deletedNotes.forEach { note ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(note.getString("title") ?: "Untitled", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(note.getString("content") ?: "", style = MaterialTheme.typography.bodyMedium)

                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { onRestoreNote(note) }) {
                                    Text("Restore")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}