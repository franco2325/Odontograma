package com.odontograma.v1

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.foundation.layout.FlowRow
import android.content.Context
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { DentalApp() } }
}

data class Tooth(val number: Int, val finding: String = "")

@Composable fun DentalApp() {
    var patient by rememberSaveable { mutableStateOf("") }
    var transcript by remember { mutableStateOf("") }
    val findings = remember { mutableStateMapOf<Int,String>() }
    var selected by remember { mutableStateOf<Int?>(null) }
    var manual by remember { mutableStateOf("") }

    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
        if (text.isNotBlank()) { transcript = text; parseDictation(text, findings) }
    }
    val mic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchSpeech(speech)
    }

    MaterialTheme {
        Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Odontograma FDI", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(patient, { patient = it }, label={Text("Paciente")}, modifier=Modifier.fillMaxWidth(), singleLine=true)
            Spacer(Modifier.height(10.dp))
            Button(onClick={
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) launchSpeech(speech)
                else mic.launch(Manifest.permission.RECORD_AUDIO)
            }, modifier=Modifier.fillMaxWidth()) { Text("🎙️  Dictar odontograma") }
            if (transcript.isNotBlank()) {
                Spacer(Modifier.height(10.dp)); Text("Texto reconocido", fontWeight=FontWeight.Bold)
                Text(transcript, modifier=Modifier.fillMaxWidth().background(Color(0xFFF3F4F6), RoundedCornerShape(10.dp)).padding(12.dp))
            }
            Spacer(Modifier.height(14.dp)); Text("Odontograma", fontWeight=FontWeight.Bold)
            ToothGrid(11..18, findings, selected){ selected=it }
            ToothGrid(21..28, findings, selected){ selected=it }
            ToothGrid(31..38, findings, selected){ selected=it }
            ToothGrid(41..48, findings, selected){ selected=it }

            selected?.let { n ->
                Spacer(Modifier.height(14.dp)); Text("Editar pieza $n", fontWeight=FontWeight.Bold)
                OutlinedTextField(manual, { manual=it }, label={Text("Hallazgo")}, modifier=Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("Atajos", style=MaterialTheme.typography.labelMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    listOf("Sano", "Caries", "Ausente", "Corona", "Implante", "Endodoncia").forEach { label ->
                        AssistChip(onClick={ manual=label }, label={ Text(label) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Button(onClick={ findings[n]=manual; manual=""; selected=null }, modifier=Modifier.weight(1f)) { Text("Guardar") }
                    OutlinedButton(onClick={ findings.remove(n); manual=""; selected=null }, modifier=Modifier.weight(1f)) { Text("Limpiar") }
                }
            }
            Spacer(Modifier.height(18.dp)); Text("Ejemplo de dictado: “11 caries mesial, 16 amalgama oclusal, 18 ausente, 24 corona metalocerámica”.", style=MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable fun ToothGrid(range: IntRange, findings: Map<Int,String>, selected: Int?, onSelect:(Int)->Unit) {
    LazyVerticalGrid(columns=GridCells.Fixed(4), modifier=Modifier.height(170.dp), userScrollEnabled=false, verticalArrangement=Arrangement.spacedBy(6.dp), horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        items(range.toList()) { n ->
            val f=findings[n].orEmpty()
            Column(Modifier.border(2.dp, if(selected==n) MaterialTheme.colorScheme.primary else Color.LightGray, RoundedCornerShape(10.dp)).clickable{onSelect(n)}.padding(7.dp), horizontalAlignment=Alignment.CenterHorizontally) {
                Text(n.toString(), fontWeight=FontWeight.Bold)
                Text(if(f.isBlank()) "—" else f, style=MaterialTheme.typography.labelSmall, maxLines=3)
            }
        }
    }
    Spacer(Modifier.height(6.dp))
}

fun launchSpeech(launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
    val i=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-AR"); putExtra(RecognizerIntent.EXTRA_PROMPT, "Dictá el odontograma") }
    launcher.launch(i)
}

fun parseDictation(text:String, findings:MutableMap<Int,String>) {
    var t = text.lowercase(Locale("es", "AR"))
        .replace("dieciséis", "16").replace("dieciseis", "16")
        .replace("diecisiete", "17").replace("dieciocho", "18")
        .replace("once", "11").replace("doce", "12").replace("trece", "13")
        .replace("catorce", "14").replace("quince", "15")
        .replace("veintiuno", "21").replace("veintidós", "22").replace("veintidos", "22")
        .replace("veintitrés", "23").replace("veintitres", "23").replace("veinticuatro", "24")
        .replace("veinticinco", "25").replace("veintiséis", "26").replace("veintiseis", "26")
        .replace("veintisiete", "27").replace("veintiocho", "28")
        .replace("treinta y uno", "31").replace("treinta y dos", "32").replace("treinta y tres", "33")
        .replace("treinta y cuatro", "34").replace("treinta y cinco", "35").replace("treinta y seis", "36")
        .replace("treinta y siete", "37").replace("treinta y ocho", "38")
        .replace("cuarenta y uno", "41").replace("cuarenta y dos", "42").replace("cuarenta y tres", "43")
        .replace("cuarenta y cuatro", "44").replace("cuarenta y cinco", "45").replace("cuarenta y seis", "46")
        .replace("cuarenta y siete", "47").replace("cuarenta y ocho", "48")

    // Normaliza expresiones odontológicas frecuentes para que el resultado sea consistente.
    val aliases = listOf(
        "ocluso distal" to "OD", "ocluso-distal" to "OD", "oclusal distal" to "OD",
        "mesio oclusal distal" to "MOD", "mesio-ocluso-distal" to "MOD", "mesio ocluso distal" to "MOD",
        "mesio oclusal" to "MO", "mesio-oclusal" to "MO", "distal oclusal" to "DO",
        "composite" to "Composite", "resina" to "Composite", "amalgama" to "Amalgama",
        "corona metal cerámica" to "Corona metalocerámica", "corona metalceramica" to "Corona metalocerámica",
        "metal cerámica" to "metalocerámica", "metalceramica" to "metalocerámica",
        "tratamiento de conducto" to "Endodoncia", "tratamiento conducto" to "Endodoncia",
        "implante dental" to "Implante", "sin pieza" to "Ausente", "pieza ausente" to "Ausente"
    )
    aliases.forEach { (a,b) -> t=t.replace(a,b.lowercase()) }

    val re=Regex("\\b(1[1-8]|2[1-8]|3[1-8]|4[1-8])\\b")
    val matches=re.findAll(t).toList()
    for(i in matches.indices){
        val n=matches[i].value.toInt()
        val start=matches[i].range.last+1
        val end=if(i+1<matches.size) matches[i+1].range.first else t.length
        var f=t.substring(start,end).trim().trim(',', '.', ';', ':')
        f=Regex("\\s+").replace(f," ")
        if(f.isBlank()) f="Hallazgo"
        f=f.replaceFirstChar{it.uppercase()}
        findings[n]=f
    }
}
