package com.gatil.inventario.ui.cats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.gatil.inventario.data.model.AdoptionStatus
import com.gatil.inventario.data.model.Cat
import com.gatil.inventario.data.model.CatSex
import com.gatil.inventario.util.DateFieldUtil

@Composable
fun CatDetailDialog(
    cat: Cat,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var confirmingDelete by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                CatPhoto(
                    photoBase64 = cat.photoBase64,
                    contentDescription = cat.name,
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(cat.name.ifBlank { "(sem nome)" }, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.width(8.dp))
                        AssistChip(onClick = {}, label = { Text(statusText(cat.adoptionStatus)) })
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(sexText(cat.sex), style = MaterialTheme.typography.bodyMedium)

                    Spacer(Modifier.height(16.dp))
                    SectionTitle("Origem")
                    InfoLine("Data de nascimento", DateFieldUtil.format(cat.birthDate).ifBlank { cat.estimatedAgeNotes.ifBlank { "Desconhecida" } })
                    if (cat.litter.isNotBlank()) InfoLine("Ninhada", cat.litter)
                    if (cat.captureLocation.isNotBlank()) InfoLine("Local de captura", cat.captureLocation)
                    if (cat.color.isNotBlank()) InfoLine("Cor/pelagem", cat.color)
                    InfoLine("Registado em", DateFieldUtil.format(cat.entryDate))

                    Spacer(Modifier.height(16.dp))
                    SectionTitle("Saúde")
                    CheckLine("Esterilizado", cat.sterilized, DateFieldUtil.format(cat.sterilizedDate))
                    InfoLine("Desparasitação externa", DateFieldUtil.format(cat.externalDewormingDate).ifBlank { "Não registada" })
                    InfoLine("Desparasitação interna", DateFieldUtil.format(cat.internalDewormingDate).ifBlank { "Não registada" })
                    CheckLine("Vacinado", cat.vaccinated, cat.vaccinationNotes)
                    CheckLine("Testado FIV/FeLV", cat.fivFelvTested, cat.fivFelvResult)
                    if (cat.microchipNumber.isNotBlank()) InfoLine("Microchip", cat.microchipNumber)
                    if (cat.healthNotes.isNotBlank()) InfoLine("Notas de saúde", cat.healthNotes)

                    if (cat.temperament.isNotBlank() || cat.notes.isNotBlank()) {
                        Spacer(Modifier.height(16.dp))
                        SectionTitle("Comportamento e notas")
                        if (cat.temperament.isNotBlank()) InfoLine("Temperamento", cat.temperament)
                        if (cat.notes.isNotBlank()) InfoLine("Outras notas", cat.notes)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Fechar") }
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Editar")
                    }
                    if (isAdmin) {
                        Spacer(Modifier.width(4.dp))
                        TextButton(
                            onClick = { confirmingDelete = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Eliminar")
                        }
                    }
                }
            }
        }
    }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Eliminar ${cat.name.ifBlank { "este gato" }}?") },
            text = { Text("Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(
                    onClick = { confirmingDelete = false; onDelete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text("$label: ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CheckLine(label: String, checked: Boolean, detail: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
        if (checked && detail.isNotBlank()) {
            Text(" — $detail", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun sexText(sex: CatSex): String = when (sex) {
    CatSex.MACHO -> "Macho"
    CatSex.FEMEA -> "Fêmea"
    CatSex.DESCONHECIDO -> "Sexo desconhecido"
}

private fun statusText(status: AdoptionStatus): String = when (status) {
    AdoptionStatus.DISPONIVEL -> "Disponível"
    AdoptionStatus.RESERVADO -> "Reservado"
    AdoptionStatus.ADOTADO -> "Adotado"
}
