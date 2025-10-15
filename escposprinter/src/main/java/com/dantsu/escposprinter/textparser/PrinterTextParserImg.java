package com.dantsu.escposprinter.textparser;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.EscPosPrinterCommands;
import com.dantsu.escposprinter.EscPosPrinterSize;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;
import com.dantsu.escposprinter.exceptions.EscPosEncodingException;


public class PrinterTextParserImg implements IPrinterTextParserElement {
    // Modo de teste pós-imagem para diagnosticar travas de firmware.
    public enum PostImageMode {
        NONE,          // não envia nada além do bloco gráfico
        LF,            // envia apenas LF (0x0A)
        RESET_LF,      // ESC @ seguido de LF
        RESET_LF_FEED, // ESC @ + LF + pequeno feed ESC J
        LF_FEED,       // Apenas LF + pequeno feed ESC J (sem reset)
        FULL_CLEAN,    // Reset completo + LF + feed + reativa autoLfAfterGraphics e desativa minimalRawImageMode
        RAW_ONLY       // Somente imagem crua, restaura flags
    }

    private static PostImageMode configuredPostImageMode = PostImageMode.FULL_CLEAN; // padrão alterado para limpeza completa

    /**
     * Configura globalmente o modo pós-impressão de imagem.
     * @param mode PostImageMode
     */
    public static void setPostImageMode(PostImageMode mode) {
        // Fallback agora sempre para FULL_CLEAN se nulo
        configuredPostImageMode = mode != null ? mode : PostImageMode.FULL_CLEAN;
    }
    
    /**
     * Convert Drawable instance to a hexadecimal string of the image data.
     *
     * @param printerSize A EscPosPrinterSize instance that will print the image.
     * @param drawable Drawable instance to be converted.
     * @return A hexadecimal string of the image data. Empty string if Drawable cannot be cast to BitmapDrawable.
     */
    public static String bitmapToHexadecimalString(EscPosPrinterSize printerSize, Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return PrinterTextParserImg.bitmapToHexadecimalString(printerSize, (BitmapDrawable) drawable);
        }
        return "";
    }
    /**
     * Convert Drawable instance to a hexadecimal string of the image data.
     *
     * @param printerSize A EscPosPrinterSize instance that will print the image.
     * @param drawable Drawable instance to be converted.
     * @param gradient false : Black and white image, true : Grayscale image
     * @return A hexadecimal string of the image data. Empty string if Drawable cannot be cast to BitmapDrawable.
     */
    public static String bitmapToHexadecimalString(EscPosPrinterSize printerSize, Drawable drawable, boolean gradient) {
        if (drawable instanceof BitmapDrawable) {
            return PrinterTextParserImg.bitmapToHexadecimalString(printerSize, (BitmapDrawable) drawable, gradient);
        }
        return "";
    }
    
    /**
     * Convert BitmapDrawable instance to a hexadecimal string of the image data.
     *
     * @param printerSize A EscPosPrinterSize instance that will print the image.
     * @param bitmapDrawable BitmapDrawable instance to be converted.
     * @return A hexadecimal string of the image data.
     */
    public static String bitmapToHexadecimalString(EscPosPrinterSize printerSize, BitmapDrawable bitmapDrawable) {
        return PrinterTextParserImg.bitmapToHexadecimalString(printerSize, bitmapDrawable.getBitmap());
    }

    /**
     * Convert BitmapDrawable instance to a hexadecimal string of the image data.
     *
     * @param printerSize A EscPosPrinterSize instance that will print the image.
     * @param bitmapDrawable BitmapDrawable instance to be converted.
     * @param gradient false : Black and white image, true : Grayscale image
     * @return A hexadecimal string of the image data.
     */
    public static String bitmapToHexadecimalString(EscPosPrinterSize printerSize, BitmapDrawable bitmapDrawable, boolean gradient) {
        return PrinterTextParserImg.bitmapToHexadecimalString(printerSize, bitmapDrawable.getBitmap(), gradient);
    }
    
    /**
     * Convert Bitmap instance to a hexadecimal string of the image data.
     *
     * @param printerSize A EscPosPrinterSize instance that will print the image.
     * @param bitmap Bitmap instance to be converted.
     * @return A hexadecimal string of the image data.
     */
    public static String bitmapToHexadecimalString(EscPosPrinterSize printerSize, Bitmap bitmap) {
        return PrinterTextParserImg.bitmapToHexadecimalString(printerSize, bitmap, true);
    }

    /**
     * Convert Bitmap instance to a hexadecimal string of the image data.
     *
     * @param printerSize A EscPosPrinterSize instance that will print the image.
     * @param bitmap Bitmap instance to be converted.
     * @param gradient false : Black and white image, true : Grayscale image
     * @return A hexadecimal string of the image data.
     */
    public static String bitmapToHexadecimalString(EscPosPrinterSize printerSize, Bitmap bitmap, boolean gradient) {
        return PrinterTextParserImg.bytesToHexadecimalString(printerSize.bitmapToBytes(bitmap, gradient));
    }
    
    /**
     * Convert byte array to a hexadecimal string of the image data.
     *
     * @param bytes Bytes contain the image in ESC/POS command.
     * @return A hexadecimal string of the image data.
     */
    public static String bytesToHexadecimalString(byte[] bytes) {
        StringBuilder imageHexString = new StringBuilder();
        for (byte aByte : bytes) {
            String hexString = Integer.toHexString(aByte & 0xFF);
            if (hexString.length() == 1) {
                imageHexString.append("0");
            }
            imageHexString.append(hexString);
        }
        return imageHexString.toString();
    }
    
    /**
     * Convert hexadecimal string of the image data to bytes ESC/POS command.
     *
     * @param hexString Hexadecimal string of the image data.
     * @return Bytes contain the image in ESC/POS command.
     */
    public static byte[] hexadecimalStringToBytes(String hexString) throws NumberFormatException {
        byte[] bytes = new byte[hexString.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int pos = i * 2;
            bytes[i] = (byte) Integer.parseInt(hexString.substring(pos, pos + 2), 16);
        }
        return bytes;
    }
    
    
    private int length;
    private byte[] image;
    
    /**
     * Create new instance of PrinterTextParserImg.
     *
     * @param printerTextParserColumn Parent PrinterTextParserColumn instance.
     * @param textAlign Set the image alignment. Use PrinterTextParser.TAGS_ALIGN_... constants.
     * @param hexadecimalString Hexadecimal string of the image data.
     */
    public PrinterTextParserImg(PrinterTextParserColumn printerTextParserColumn, String textAlign, String hexadecimalString) {
        this(printerTextParserColumn, textAlign, PrinterTextParserImg.hexadecimalStringToBytes(hexadecimalString));
    }

    /**
     * Create new instance of PrinterTextParserImg.
     *
     * @param printerTextParserColumn Parent PrinterTextParserColumn instance.
     * @param textAlign Set the image alignment. Use PrinterTextParser.TAGS_ALIGN_... constants.
     * @param image Bytes contain the image in ESC/POS command.
     */
    public PrinterTextParserImg(PrinterTextParserColumn printerTextParserColumn, String textAlign, byte[] image) {
        EscPosPrinter printer = printerTextParserColumn.getLine().getTextParser().getPrinter();

        int
                byteWidth = ((int) image[4] & 0xFF) + ((int) image[5] & 0xFF) * 256,
                width = byteWidth * 8,
                height = ((int) image[6] & 0xFF) + ((int) image[7] & 0xFF) * 256,
                nbrByteDiff = (int) Math.floor(((float) (printer.getPrinterWidthPx() - width)) / 8f),
                nbrWhiteByteToInsert = 0;

        int printerPxWidth = printer.getPrinterWidthPx();
        // Se a imagem for mais larga que a cabeça de impressão, faz crop (corta à direita)
        if (width > printerPxWidth && byteWidth > 0) {
            int allowedByteWidth = (int) Math.floor(printerPxWidth / 8f);
            if (allowedByteWidth <= 0) {
                allowedByteWidth = byteWidth; // fallback, evita divisão por zero
            }
            if (allowedByteWidth < byteWidth) {
                byte[] cropped = EscPosPrinterCommands.initGSv0Command(allowedByteWidth, height);
                int lineSrcOffset;
                int lineDstOffset;
                for (int y = 0; y < height; y++) {
                    lineSrcOffset = 8 + y * byteWidth;
                    lineDstOffset = 8 + y * allowedByteWidth;
                    System.arraycopy(image, lineSrcOffset, cropped, lineDstOffset, allowedByteWidth);
                }
                image = cropped;
                byteWidth = allowedByteWidth;
                width = byteWidth * 8;
                nbrByteDiff = (int) Math.floor(((float) (printerPxWidth - width)) / 8f);
            }
        }

        switch (textAlign) {
            case PrinterTextParser.TAGS_ALIGN_CENTER:
                nbrWhiteByteToInsert = Math.round(((float) nbrByteDiff) / 2f);
                break;
            case PrinterTextParser.TAGS_ALIGN_RIGHT:
                nbrWhiteByteToInsert = nbrByteDiff;
                break;
        }

        if (nbrWhiteByteToInsert > 0) {
            int newByteWidth = byteWidth + nbrWhiteByteToInsert; // nova largura após padding
            byte[] newImage = EscPosPrinterCommands.initGSv0Command(newByteWidth, height);
            for (int i = 0; i < height; i++) {
                System.arraycopy(image, (byteWidth * i + 8), newImage, (newByteWidth * i + nbrWhiteByteToInsert + 8), byteWidth);
            }
            image = newImage;
            byteWidth = newByteWidth; // atualiza para cálculo correto de length
        }

        this.length = (int) Math.ceil(((float) byteWidth * 8) / ((float) printer.getPrinterCharSizeWidthPx()));
        this.image = image;
    }

    /**
     * Get the image width in char length.
     *
     * @return int
     */
    @Override
    public int length() throws EscPosEncodingException {
        return this.length;
    }

    /**
     * Print image
     *
     * @param printerSocket Instance of EscPosPrinterCommands
     * @return this Fluent method
     */
    @Override
    public PrinterTextParserImg print(EscPosPrinterCommands printerSocket) throws EscPosConnectionException {
        // Detecta imagem vazia (largura/altura zero) para fallback direto
        boolean isEmpty = (this.image == null
                || this.image.length < 8
                || (((int) this.image[4] & 0xFF) + ((int) this.image[5] & 0xFF) * 256) == 0
                || (((int) this.image[6] & 0xFF) + ((int) this.image[7] & 0xFF) * 256) == 0);

        if (!isEmpty) {
            // Imprime somente o bloco gráfico (modo mínimo) para isolar problema
            printerSocket.setMinimalRawImageMode(true).setAutoLfAfterGraphics(false).printImage(this.image);
        }

        // Se imagem vazia, força FULL_CLEAN independente do modo configurado
        PostImageMode effectiveMode = isEmpty ? PostImageMode.FULL_CLEAN : configuredPostImageMode;

        // Sequência de teste pós-imagem
        switch (effectiveMode) {
            case NONE:
                // Não envia nada adicional
                break;
            case LF:
                printerSocket.newLine();
                break;
            case RESET_LF:
                // Usa API pública reset() em vez de chamadas diretas write/send inexistentes
                printerSocket.reset();
                printerSocket.newLine();
                break;
            case RESET_LF_FEED:
                printerSocket.reset();
                printerSocket.newLine();
                // Feed curto de 16 dots para garantir saída do modo gráfico
                printerSocket.feedPaper(16);
                break;
            case LF_FEED:
                printerSocket.newLine();
                printerSocket.feedPaper(16);
                break;
            case FULL_CLEAN:
                printerSocket.reset();
                printerSocket.newLine();
                printerSocket.feedPaper(16);
                printerSocket.setMinimalRawImageMode(false).setAutoLfAfterGraphics(true);
                break;
            case RAW_ONLY:
                // Apenas restaura flags, sem enviar bytes extras
                printerSocket.setMinimalRawImageMode(false).setAutoLfAfterGraphics(true);
                break;
            default:
                printerSocket.reset();
                printerSocket.newLine();
                printerSocket.feedPaper(16);
                printerSocket.setMinimalRawImageMode(false).setAutoLfAfterGraphics(true);
                break;
        }
        return this;
    }
}
