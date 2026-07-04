/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.component

import android.widget.Toast
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.rukamori.archivetune.LocalDatabase
import moe.rukamori.archivetune.LocalPlayerConnection
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.extensions.toSavedQueueEntity

/**
 * "Save Queue" dialog: lets the user give the currently playing queue a custom name and stores a
 * permanent, restorable snapshot of it (order, currently playing song, position, shuffle state
 * and repeat mode). See [moe.rukamori.archivetune.db.entities.SavedQueueEntity].
 */
@Composable
fun SaveQueueDialog(
    onDismiss: () -> Unit,
    initialTextFieldValue: String? = null,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val coroutineScope = rememberCoroutineScope()

    TextFieldDialog(
        icon = { Icon(painter = painterResource(R.drawable.bookmark), contentDescription = null) },
        title = { Text(text = stringResource(R.string.save_queue)) },
        placeholder = { Text(text = stringResource(R.string.saved_queue_name)) },
        isInputValid = { it.trim().isNotEmpty() },
        initialTextFieldValue =
            TextFieldValue(initialTextFieldValue ?: context.getString(R.string.queue)),
        onDismiss = onDismiss,
        onDone = { rawName ->
            val name = rawName.trim()
            coroutineScope.launch(Dispatchers.Main) {
                val snapshot = playerConnection.captureQueueSnapshot(name)
                if (snapshot == null) {
                    Toast.makeText(context, R.string.nothing_to_save, Toast.LENGTH_SHORT).show()
                    return@launch
                }
                database.withTransaction {
                    insert(snapshot.toSavedQueueEntity(name))
                }
                Toast
                    .makeText(
                        context,
                        context.getString(R.string.queue_saved, name),
                        Toast.LENGTH_SHORT,
                    ).show()
            }
        },
    )
}
