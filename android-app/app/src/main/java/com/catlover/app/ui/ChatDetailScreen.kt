gestures.detectTapGestures
import androidx.compose.foundation.ms
import androidx.copose.foundation.hape.CircleShapecons
import androidx.compose.material.icons.filled.AttachFile
import androidx.mpose.material.ico.filled.Micl.icons.filed.Timer
import androidx.compose.materialColoinput.pointe.pointerInput
import ndrodx.ompoe.ui.textfnt.FntWeightnit,    onSendVoice: (java.io.File) -> Unit,
    onAttachFile: () -> Unit
 vartexttate by remember { mutableStateOf("") }
    var isReording by remember { mutableStateOf(false) }

    ScopBar = {
            TApp(
               title                     Column {                       style = MaterialTheme.typography.titleMedium,
                     ,
                            letterSpacing = 2.sp
                        )
                        Text(
                            "SECURE NEURAL LINK"        color = NeonCyan.copy(alpha = 0.5f),              style=MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.sp
                        )
                    }
                 ToptopAppBarColors(conainerColor = CyberBlack)        )
         CybbttomBar = {
            ChatIput(
                value = textState,
                onange = { textStte = it },
                isRecording = isRecordi,              onRecordToggle = { isRecording = !isRecrdig },
                on
                    if (textState.isNotBlank()) {
                        onSendMessage(textState)
                    textState = ""
                    ,
                onAttach = onAttachFile
                .padding(horizontal = 16.     rseLayot = true){
            MessageBubble(msg)                }
        ) {
            if (message.expiresAt != null) {
        Row(verticalAlignment = Alignment.CenterVertically        Icon(Icons.Default.imer, contentDescription = null, tint = NonCyan, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Te"SELF-DESTRUCT ACTIVE", color = NeonCyan, fonSiz = 8.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
            Te(textRow(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                            )
                if (essage.isMe) {
                    Spacer(mwidth(4.dp))
                    Text(
                        text = if (messge.isRead) "✓✓" ese "✓",
                        color = f (messae.isRead) NeoCyan else Color.White.copyapha = 0.3f),
                        fontSze = 12.sp,
                        fotWight = FoWeightBol                                    }}

isRecrdig: Boolean,
    onRecordToggle: () -> Unit,
    on,    onAttach: (-> Unit
) 
  Surfae(
        crmodifier=Modifier
          2
            IconButton(onClick = onAttach) {
                Icon(Icons.Default.AttachFile, contentDescription = null, contentColor = NeonCyan)
            }

           Nualsignl,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))          colors=TextFieldDefaults.textFieldColors(Wie
))

            if (value.isBlank( {         { /* Handlelg press for rcordig */ }            (if isRecording) Color.Red else rple)
                        .pointeInput(Unit) {
                            detectTaGestures(
                                onLongPress = { onRecordTogg() }
                            
                        }        Michite)
                }
            } else {
                IconButton(
                    onClick = onSend,
                    modifier = Modifier
                        .clip(CircleSape)
                        .background(NeonCyan)
                ) {
                    Icon(Icons.Default.Send, contentDescripton = null, contenColor = CybrBlack
                }
