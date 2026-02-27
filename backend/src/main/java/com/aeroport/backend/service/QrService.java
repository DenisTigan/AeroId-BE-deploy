package com.aeroport.backend.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Service
public class QrService {

    public String generateQrCode(String text) throws Exception {
        // 1. Initializam generatorul (250x250 pixeli)
        QRCodeWriter barcodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = barcodeWriter.encode(text, BarcodeFormat.QR_CODE, 250, 250);

        // 2. Desenam imaginea intr-un buffer de memorie
        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        byte[] pngData = pngOutputStream.toByteArray();

        // 3. O transformam in text Base64 (pentru a o putea trimite prin JSON la React)
        // Adaugam prefixul standard ca React sa o poata pune direct intr-un <img src="...">
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(pngData);
    }
}
