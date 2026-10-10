package com.gatil.inventario.ui.vet

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.gatil.inventario.data.model.Cat
import com.gatil.inventario.data.model.VetInvoice
import com.gatil.inventario.util.AmountFieldUtil
import com.gatil.inventario.util.CameraCaptureUtil
import com.gatil.inventario.util.DateFieldUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VetInvoicesScreen(
    isAdmin: Boolean,
    onBack: () -> Unit,
    viewModel: VetInvoicesViewModel = viewModel()
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingInvoice by remember { mutableStateOf<VetInvoice?>(null) }
    var viewingPhotoBase64 by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // "Todas" + um separador por clínica com pelo menos uma fatura.
    val tabs = listOf<String?>(null) + viewModel.clinics
    val selectedIndex = tabs.indexOf(viewModel.selectedClinic).coerceAtLeast(0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Faturas de Veterinário") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar fatura")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (viewModel.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (viewModel.invoices.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Sem faturas registadas",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                ScrollableTabRow(selectedTabIndex = selectedIndex) {
                    tabs.forEachIndexed { index, clinic ->
                        Tab(
                            selected = index == selectedIndex,
                            onClick = { viewModel.onSelectClinic(clinic) },
                            text = { Text(clinic ?: "Todas") }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (viewModel.visibleInvoices.size == 1) "1 fatura" else "${viewModel.visibleInvoices.size} faturas",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Total: ${AmountFieldUtil.format(viewModel.visibleTotal)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.visibleInvoices, key = { it.id }) { invoice ->
                        InvoiceRow(
                            invoice = invoice,
                            isAdmin = isAdmin,
                            onClick = { editingInvoice = invoice },
                            onDelete = { viewModel.deleteInvoice(invoice) },
                            onViewPhoto = { viewingPhotoBase64 = invoice.photoBase64 }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        VetInvoiceDialog(
            initial = null,
            cats = viewModel.cats,
            knownClinics = viewModel.clinics,
            preselectedClinic = viewModel.selectedClinic,
            isSaving = viewModel.isSaving,
            errorMessage = viewModel.errorMessage,
            onDismiss = { showAddDialog = false },
            onSave = { invoice, photoUri ->
                viewModel.addInvoice(context, invoice, photoUri) { showAddDialog = false }
            }
        )
    }

    editingInvoice?.let { invoice ->
        VetInvoiceDialog(
            initial = invoice,
            cats = viewModel.cats,
            knownClinics = viewModel.clinics,
            preselectedClinic = null,
            isSaving = viewModel.isSaving,
            errorMessage = viewModel.errorMessage,
            onDismiss = { editingInvoice = null },
            onSave = { updated, photoUri ->
                viewModel.updateInvoice(context, updated, photoUri) { editingInvoice = null }
            }
        )
    }

    viewingPhotoBase64?.let { base64 ->
        val bitmap = remember(base64) { com.gatil.inventario.util.ImageCompressionUtil.decodeBase64ToBitmap(base64) }
        Dialog(onDismissRequest = { viewingPhotoBase64 = null }) {
            if (bitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Foto da fatura",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                )
            }
        }
    }
}

@Composable
private fun InvoiceRow(
    invoice: VetInvoice,
    isAdmin: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onViewPhoto: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (invoice.photoBase64.isNotBlank()) {
                val bitmap = remember(invoice.photoBase64) {
                    com.gatil.inventario.util.ImageCompressionUtil.decodeBase64ToBitmap(invoice.photoBase64)
                }
                if (bitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Foto da fatura",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onViewPhoto)
                    )
                } else {
                    Icon(
                        Icons.Default.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(12.dp))
            } else {
                Icon(
                    Icons.Default.Receipt,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(invoice.clinicName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        AmountFieldUtil.format(invoice.amount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(DateFieldUtil.format(invoice.date), style = MaterialTheme.typography.bodySmall)
                if (invoice.catName.isNotBlank()) {
                    Text("Gato: ${invoice.catName}", style = MaterialTheme.typography.bodySmall)
                }
                if (invoice.description.isNotBlank()) {
                    Text(invoice.description, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (isAdmin) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                }
            }
        }
    }
}

/**
 * Formulário único para adicionar OU editar uma fatura, incluindo foto/scan
 * opcional (câmara ou galeria) e sugestões de clínicas já usadas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VetInvoiceDialog(
    initial: VetInvoice?,
    cats: List<Cat>,
    knownClinics: List<String>,
    preselectedClinic: String?,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (VetInvoice, Uri?) -> Unit
) {
    var clinicName by remember { mutableStateOf(initial?.clinicName ?: preselectedClinic ?: "") }
    var catExpanded by remember { mutableStateOf(false) }
    var selectedCat by remember { mutableStateOf(cats.firstOrNull { it.id == initial?.catId }) }
    var catNameFallback by remember { mutableStateOf(initial?.catName ?: "") }

    var dateText by remember { mutableStateOf(DateFieldUtil.format(initial?.date)) }
    var amountText by remember {
        mutableStateOf(initial?.amount?.takeIf { it > 0 }?.toString() ?: "")
    }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var newPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) newPhotoUri = uri }

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

    val dateValid = dateText.isNotBlank() && DateFieldUtil.isValidOrEmpty(dateText)
    val amountValid = amountText.isNotBlank() && AmountFieldUtil.isValid(amountText)
    val formValid = clinicName.isNotBlank() && dateValid && amountValid

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    if (initial == null) "Nova fatura" else "Editar fatura",
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
                    // Foto/scan da fatura
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val displayUri = newPhotoUri
                        val existingBitmap = remember(initial?.photoBase64) {
                            initial?.photoBase64?.takeIf { it.isNotBlank() }
                                ?.let { com.gatil.inventario.util.ImageCompressionUtil.decodeBase64ToBitmap(it) }
                        }
                        when {
                            displayUri != null -> AsyncImage(
                                model = displayUri,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                            existingBitmap != null -> androidx.compose.foundation.Image(
                                bitmap = existingBitmap.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                            else -> Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(40.dp))
                        }
                        Row(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalIconButton(onClick = launchCamera) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = "Tirar foto")
                            }
                            FilledTonalIconButton(
                                onClick = {
                                    photoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = "Escolher da galeria")
                            }
                        }
                    }

                    OutlinedTextField(
                        value = clinicName,
                        onValueChange = { clinicName = it },
                        label = { Text("Clínica") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (knownClinics.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            knownClinics.forEach { clinic ->
                                AssistChip(onClick = { clinicName = clinic }, label = { Text(clinic) })
                            }
                        }
                    }

                    if (cats.isEmpty()) {
                        OutlinedTextField(
                            value = catNameFallback,
                            onValueChange = { catNameFallback = it },
                            label = { Text("Gato (opcional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = catExpanded,
                            onExpandedChange = { catExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedCat?.name ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Gato (opcional)") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = catExpanded,
                                onDismissRequest = { catExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Nenhum") },
                                    onClick = {
                                        selectedCat = null
                                        catExpanded = false
                                    }
                                )
                                cats.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.name) },
                                        onClick = {
                                            selectedCat = cat
                                            catExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { dateText = it },
                        label = { Text("Data (dd/mm/aaaa)") },
                        isError = dateText.isNotBlank() && !DateFieldUtil.isValidOrEmpty(dateText),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Valor (€)") },
                        isError = amountText.isNotBlank() && !AmountFieldUtil.isValid(amountText),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descrição (ex: consulta + vacina)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notas (opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancelar") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val parsedDate = DateFieldUtil.parse(dateText) ?: return@Button
                            val parsedAmount = AmountFieldUtil.parse(amountText) ?: return@Button
                            val invoice = (initial ?: VetInvoice()).copy(
                                clinicName = clinicName.trim(),
                                catId = if (cats.isEmpty()) "" else (selectedCat?.id ?: ""),
                                catName = if (cats.isEmpty()) catNameFallback else (selectedCat?.name ?: ""),
                                date = parsedDate,
                                amount = parsedAmount,
                                description = description,
                                notes = notes
                            )
                            onSave(invoice, newPhotoUri)
                        },
                        enabled = formValid && !isSaving
                    ) {
                        if (isSaving) {
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
