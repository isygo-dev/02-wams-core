package eu.isygoit.ui.auth;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public final class QrCodeRenderer {

    private static final int SIZE = 49;

    private QrCodeRenderer() {
    }

    public static String toSvgDataUri(String content) throws WriterException {
        BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE,
                SIZE, SIZE, Map.of(EncodeHintType.MARGIN, 4));
        StringBuilder path = new StringBuilder();
        for (int y = 0; y < matrix.getHeight(); y++) {
            for (int x = 0; x < matrix.getWidth(); x++) {
                if (matrix.get(x, y)) {
                    path.append('M').append(x).append(' ').append(y)
                            .append("h1v1h-1z");
                }
            }
        }

        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 "
                + matrix.getWidth() + " " + matrix.getHeight()
                + "\" shape-rendering=\"crispEdges\"><rect width=\"100%\" height=\"100%\""
                + " fill=\"#fff\"/><path d=\"" + path + "\" fill=\"#111\"/></svg>";
        String encoded = Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
        return "data:image/svg+xml;base64," + encoded;
    }
}
