package com.gatil.inventario.ui.cats

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.gatil.inventario.data.model.AdoptionStatus
import com.gatil.inventario.data.model.Cat
import com.gatil.inventario.data.model.CatSex
import com.gatil.inventario.util.CameraCaptureUtil
import com.gatil.inventario.util.DateFieldUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatFormDialog(
    initial: Cat?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    viewModel: CatFormViewModel = viewModel()
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var sex by remember { mutableStateOf(initial?.sex ?: CatSex.DESCONHECIDO) }
    var newPhotoUri by remember { mutableStateOf<Uri?>(null) }

    var birthDateText by remember { mutableStateOf(DateFieldUtil.format(initial?.birthDate)) }
    var estimatedAgeNotes by remember { mutableStateOf(initial?.estimatedAgeNotes ?: "") }
    var litter by remember { mutableStateOf(initial?.litter ?: "") }
    var captureLocation by remember { mutableStateOf(initial?.captureLocation ?: "") }
    var color by remember { mutableStateOf(initial?.color ?: "") }

    var sterilized by remember { mutableStateOf(initial?.sterilized ?: false) }
    var sterilizedDateText by remember { mutableStateOf(DateFieldUtil.format(initial?.sterilizedDate)) }
    var externalDewormingText by remember { mutableStateOf(DateFieldUtil.format(initial?.externalDewormingDate)) }
    var internalDewormingText by remember { mutableStateOf(DateFieldUtil.format(initial?.internalDewormingDate)) }
    var vaccinated by remember { mutableStateOf(initial?.vaccinated ?: false) }
    var vaccinationNotes by remember { mutableStateOf(initial?.vaccinationNotes ?: "") }
    var fivFelvTested by remember { mutableStateOf(initial?.fivFelvTested ?: false) }
    var fivFelvResult by remember { mutableStateOf(initial?.fivFelvResult ?: "") }
    var microchipNumber by remember { mutableStateOf(initial?.microchipNumber ?: "") }
    var healthNotes by remember { mutableStateOf(initial?.healthNotes ?: "") }

    var temperament by remember { mutableStateOf(initial?.temperament ?: "") }
    var adoptionStatus by remember { mutableStateOf(initial?.adoptionStatus ?: AdoptionStatus.DISPONIVEL) }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) newPhotoUri = uri }

    val context = LocalContext.current
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success -> if (success) pendingCameraUri?.let { newPhotoUri = it } }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = CameraCaptureUtil.createTempImageUri(context)
            pendingCameraUri = uri
            cameraCaptureLauncher.launch(uri)
        }
    }
    val launchCamera: () -> Unit = {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            val uri = CameraCaptureUtil.createTempImageUri(context)
            pendingCameraUri = uri
            cameraCaptureLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val datesValid = DateFieldUtil.isValidOrEmpty(birthDateText) &&
        DateFieldUtil.isValidOrEmpty(sterilizedDateText) &&
        DateFieldUtil.isValidOrEmpty(externalDewormingText) &&
        DateFieldUtil.isValidOrEmpty(internalDewormingText)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.92f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    if (initial == null) "Novo gato" else "Editar gato",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Foto
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val displayUri = newPhotoUri
                        when {
                            displayUri != null -> AsyncImage(
                                model = displayUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            else -> CatPhoto(
                                photoBase64 = initial?.photoBase64 ?: "",
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        FilledTonalIconButton(
                            onClick = launchCamera,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp, 8.dp, 56.dp, 8.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Tirar foto")
                        }
                        FilledTonalIconButton(
                            onClick = {
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = "Escolher da galeria")
                        }
                    }

                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())

                    Text("Sexo", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SexChip("Macho", sex == CatSex.MACHO) { sex = CatSex.MACHO }
                        SexChip("Fêmea", sex == CatSex.FEMEA) { sex = CatSex.FEMEA }
                        SexChip("Desconhecido", sex == CatSex.DESCONHECIDO) { sex = CatSex.DESCONHECIDO }
                    }

                    SectionDivider("Origem")
                    OutlinedTextField(
                        value = birthDateText,
                        onValueChange = { birthDateText = it },
                        label = { Text("Data de nascimento (dd/mm/aaaa, se conhecida)") },
                        isError = !DateFieldUtil.isValidOrEmpty(birthDateText),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = estimatedAgeNotes,
                        onValueChange = { estimatedAgeNotes = it },
                        label = { Text("Idade aproximada (se a data não for conhecida)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(value = litter, onValueChange = { litter = it }, label = { Text("Ninhada") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = captureLocation, onValueChange = { captureLocation = it }, label = { Text("Local de captura") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = color, onValueChange = { color = it }, label = { Text("Cor/pelagem") }, modifier = Modifier.fillMaxWidth())

                    SectionDivider("Saúde")
                    CheckboxRow("Esterilizado", sterilized) { sterilized = it }
                    if (sterilized) {
                        OutlinedTextField(
                            value = sterilizedDateText,
                            onValueChange = { sterilizedDateText = it },
                            label = { Text("Data de esterilização (dd/mm/aaaa)") },
                            isError = !DateFieldUtil.isValidOrEmpty(sterilizedDateText),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = externalDewormingText,
                        onValueChange = { externalDewormingText = it },
                        label = { Text("Desparasitação externa (dd/mm/aaaa)") },
                        isError = !DateFieldUtil.isValidOrEmpty(externalDewormingText),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = internalDewormingText,
                        onValueChange = { internalDewormingText = it },
                        label = { Text("Desparasitação interna (dd/mm/aaaa)") },
                        isError = !DateFieldUtil.isValidOrEmpty(internalDewormingText),
                        modifier = Modifier.fillMaxWidth()
                    )
                    CheckboxRow("Vacinado", vaccinated) { vaccinated = it }
                    if (vaccinated) {
                        OutlinedTextField(
                            value = vaccinationNotes,
                            onValueChange = { vaccinationNotes = it },
                            label = { Text("Vacinas/datas") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    CheckboxRow("Testado FIV/FeLV", fivFelvTested) { fivFelvTested = it }
                    if (fivFelvTested) {
                        OutlinedTextField(
                            value = fivFelvResult,
                            onValueChange = { fivFelvResult = it },
                            label = { Text("Resultado (ex: Negativo)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(value = microchipNumber, onValueChange = { microchipNumber = it }, label = { Text("Nº de microchip (opcional)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = healthNotes, onValueChange = { healthNotes = it }, label = { Text("Notas de saúde") }, modifier = Modifier.fillMaxWidth())

                    SectionDivider("Comportamento e adoção")
                    OutlinedTextField(value = temperament, onValueChange = { temperament = it }, label = { Text("Temperamento") }, modifier = Modifier.fillMaxWidth())

                    Text("Estado de adoção", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusChip("Disponível", adoptionStatus == AdoptionStatus.DISPONIVEL) { adoptionStatus = AdoptionStatus.DISPONIVEL }
                        StatusChip("Reservado", adoptionStatus == AdoptionStatus.RESERVADO) { adoptionStatus = AdoptionStatus.RESERVADO }
                        StatusChip("Adotado", adoptionStatus == AdoptionStatus.ADOTADO) { adoptionStatus = AdoptionStatus.ADOTADO }
                    }
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Outras notas") }, modifier = Modifier.fillMaxWidth())

                    viewModel.errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, enabled = !viewModel.isSaving) { Text("Cancelar") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val cat = (initial ?: Cat()).copy(
                                name = name,
                                sex = sex,
                                birthDate = DateFieldUtil.parse(birthDateText),
                                estimatedAgeNotes = estimatedAgeNotes,
                                litter = litter,
                                captureLocation = captureLocation,
                                color = color,
                                sterilized = sterilized,
                                sterilizedDate = DateFieldUtil.parse(sterilizedDateText),
                                externalDewormingDate = DateFieldUtil.parse(externalDewormingText),
                                internalDewormingDate = DateFieldUtil.parse(internalDewormingText),
                                vaccinated = vaccinated,
                                vaccinationNotes = vaccinationNotes,
                                fivFelvTested = fivFelvTested,
                                fivFelvResult = fivFelvResult,
                                microchipNumber = microchipNumber,
                                healthNotes = healthNotes,
                                temperament = temperament,
                                adoptionStatus = adoptionStatus,
                                notes = notes
                            )
                            viewModel.save(context, cat, newPhotoUri, onSaved)
                        },
                        enabled = name.isNotBlank() && datesValid && !viewModel.isSaving
                    ) {
                        if (viewModel.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Guardar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionDivider(title: String) {
    Spacer(Modifier.height(4.dp))
    HorizontalDivider()
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun CheckboxRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label)
    }
}

@Composable
private fun SexChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun StatusChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}
