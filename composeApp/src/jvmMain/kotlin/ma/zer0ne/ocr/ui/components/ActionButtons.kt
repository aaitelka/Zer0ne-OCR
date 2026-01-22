package ma.zer0ne.ocr.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ma.zer0ne.ocr.ui.theme.AppTheme

enum class ActionButtonMode {
    CONVERT_PDF,      // PDFs need to be converted to images first
    PROCESS_IMAGES    // Images ready to be processed to Excel
}

@Composable
fun ActionButtons(
    processing: Boolean,
    hasFiles: Boolean,
    onProcess: () -> Unit,
    onStop: () -> Unit = {},
    mode: ActionButtonMode = ActionButtonMode.PROCESS_IMAGES
) {
    val colors = AppTheme.colors

    val (buttonText, _) = when (mode) {
        ActionButtonMode.CONVERT_PDF -> Pair("Convert PDF to Images", "Converting...")
        ActionButtonMode.PROCESS_IMAGES -> Pair("Extract & Export to Excel", "Extracting...")
    }

    // Use green for PDF conversion, accent (purple) for image processing
    val buttonColor = when (mode) {
        ActionButtonMode.CONVERT_PDF -> colors.success
        ActionButtonMode.PROCESS_IMAGES -> colors.accent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (processing) {
            // Show red Stop button when processing
            Button(
                onClick = onStop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.error
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Stop",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        } else {
            // Show normal action button when not processing
            Button(
                onClick = onProcess,
                enabled = hasFiles,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    disabledContainerColor = colors.bgLight
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = buttonText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}
