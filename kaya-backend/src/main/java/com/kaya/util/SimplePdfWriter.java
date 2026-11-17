package com.kaya.util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Minimal single-page PDF receipt generator.
 *
 * <p>Chinese text uses the built-in STSong-Light CID font (Adobe-GB1) with
 * UniGB-UCS2-H encoding; ASCII runs use Helvetica so numbers and Latin letters
 * stay proportional instead of full-width. A ToUnicode map keeps the text
 * copyable and searchable.</p>
 */
public final class SimplePdfWriter {

    private static final float PAGE_WIDTH = 595;
    private static final float PAGE_HEIGHT = 842;
    private static final float MARGIN_LEFT = 72;
    private static final float MARGIN_RIGHT = 72;
    private static final float LABEL_X = 72;
    private static final float VALUE_X = 160;
    private static final float BODY_SIZE = 12;
    private static final float LINE_HEIGHT = 28;

    private SimplePdfWriter() {
    }

    public static byte[] singlePage(String title, List<String> lines) {
        String contentStream = buildContent(title, lines);
        byte[] streamBytes = contentStream.getBytes(StandardCharsets.US_ASCII);
        String toUnicode = buildToUnicode(title, lines);
        byte[] toUnicodeBytes = toUnicode.getBytes(StandardCharsets.US_ASCII);

        List<String> objects = new ArrayList<>();
        objects.add("<< /Type /Catalog /Pages 2 0 R >>");
        objects.add("<< /Type /Pages /Kids [3 0 R] /Count 1 >>");
        objects.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + (int) PAGE_WIDTH + " "
                + (int) PAGE_HEIGHT + "] "
                + "/Resources << /Font << /F1 4 0 R /F2 9 0 R /F3 10 0 R >> >> /Contents 5 0 R >>");
        objects.add("<< /Type /Font /Subtype /Type0 /BaseFont /STSong-Light "
                + "/Encoding /UniGB-UCS2-H /DescendantFonts [6 0 R] /ToUnicode 8 0 R >>");
        objects.add("<< /Length " + streamBytes.length + " >>\nstream\n"
                + contentStream + "endstream");
        objects.add("<< /Type /Font /Subtype /CIDFontType0 /BaseFont /STSong-Light "
                + "/CIDSystemInfo << /Registry (Adobe) /Ordering (GB1) /Supplement 2 >> "
                + "/FontDescriptor 7 0 R >>");
        objects.add("<< /Type /FontDescriptor /FontName /STSong-Light /Flags 6 "
                + "/FontBBox [-25 -254 1000 880] /ItalicAngle 0 /Ascent 880 "
                + "/Descent -120 /CapHeight 880 /StemV 93 >>");
        objects.add("<< /Length " + toUnicodeBytes.length + " >>\nstream\n"
                + toUnicode + "endstream");
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>");

        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(pdf.length());
            pdf.append(i + 1).append(" 0 obj\n")
                    .append(objects.get(i))
                    .append("\nendobj\n");
        }
        int xrefOffset = pdf.length();
        pdf.append("xref\n0 ").append(objects.size() + 1).append("\n");
        pdf.append("0000000000 65535 f \n");
        for (Integer offset : offsets) {
            pdf.append(String.format("%010d 00000 n \n", offset));
        }
        pdf.append("trailer\n<< /Size ").append(objects.size() + 1)
                .append(" /Root 1 0 R >>\nstartxref\n")
                .append(xrefOffset)
                .append("\n%%EOF\n");

        return pdf.toString().getBytes(StandardCharsets.US_ASCII);
    }

    private static String buildContent(String title, List<String> lines) {
        StringBuilder content = new StringBuilder();

        // 标题
        textAt(content, MARGIN_LEFT, 770, title, 20, 0.15f);

        // 分隔线
        content.append("0.8 w 0.85 g ")
                .append(MARGIN_LEFT).append(" 750 m ")
                .append((int) (PAGE_WIDTH - MARGIN_RIGHT)).append(" 750 l S ")
                .append("0 g\n");

        // 明细行：标签在左，值在右
        float y = 712;
        for (String line : lines) {
            String label = "";
            String value = line == null ? "" : line;
            int idx = value.indexOf('：');
            if (idx > 0) {
                label = value.substring(0, idx + 1);
                value = value.substring(idx + 1);
            }
            if (!label.isEmpty()) {
                textAt(content, LABEL_X, y, label, BODY_SIZE, 0.42f);
            }
            List<String> wrapped = wrap(value, 363, BODY_SIZE);
            for (int i = 0; i < wrapped.size(); i++) {
                textAt(content, VALUE_X, y, wrapped.get(i), BODY_SIZE, 0.08f);
                y -= 22;
            }
            y -= LINE_HEIGHT - 22;
        }

        // 页脚
        textAt(content, MARGIN_LEFT, 84, "本回执由 Kaya 自动生成", 9, 0.55f);
        return content.toString();
    }

    /** 在指定坐标写一段文字，按字符类型自动切换中英文字体。 */
    private static void textAt(StringBuilder sb, float x, float y, String text,
                               float size, float gray) {
        if (text == null || text.isEmpty()) return;
        sb.append("BT ")
                .append(String.format(Locale.ROOT, "%.2f g", gray)).append(' ')
                .append("1 0 0 1 ").append(fmt(x)).append(' ').append(fmt(y)).append(" Tm\n");

        for (Run run : splitRuns(text)) {
            boolean ascii = isAscii(run.text);
            // 标题字号大于正文时用粗体拉丁字体，正文用常规字体
            String font = ascii ? (size >= 16 ? "/F3" : "/F2") : "/F1";
            sb.append(font).append(' ').append(fmt(size)).append(" Tf ")
                    .append(ascii ? hexAscii(run.text) : hexUcs2(run.text))
                    .append(" Tj\n");
        }
        sb.append("ET\n");
    }

    private static List<Run> splitRuns(String text) {
        List<Run> runs = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        Boolean currentAscii = null;
        for (char c : text.toCharArray()) {
            boolean ascii = c >= 32 && c <= 126;
            if (currentAscii == null) {
                currentAscii = ascii;
            }
            if (ascii != currentAscii) {
                runs.add(new Run(current.toString()));
                current.setLength(0);
                currentAscii = ascii;
            }
            current.append(c);
        }
        if (current.length() > 0) runs.add(new Run(current.toString()));
        return runs;
    }

    private static boolean isAscii(String text) {
        for (char c : text.toCharArray()) {
            if (c < 32 || c > 126) return false;
        }
        return true;
    }

    private record Run(String text) {
    }

    /** 单字节字体（Helvetica）用十六进制 ASCII。 */
    private static String hexAscii(String value) {
        StringBuilder hex = new StringBuilder("<");
        for (byte b : value.getBytes(StandardCharsets.US_ASCII)) {
            hex.append(String.format("%02X", b));
        }
        return hex.append('>').toString();
    }

    /** CID 中文字体用 UCS-2（UTF-16BE）十六进制。 */
    private static String hexUcs2(String value) {
        StringBuilder hex = new StringBuilder("<");
        for (byte b : value.getBytes(StandardCharsets.UTF_16BE)) {
            hex.append(String.format("%02X", b));
        }
        return hex.append('>').toString();
    }

    private static String fmt(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    /** 按估算宽度折行，避免长备注溢出页面。 */
    private static List<String> wrap(String text, float maxWidth, float size) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            result.add("-");
            return result;
        }
        StringBuilder line = new StringBuilder();
        float width = 0;
        for (char c : text.toCharArray()) {
            float w = (c >= 32 && c <= 126) ? size * 0.52f : size;
            if (width + w > maxWidth && line.length() > 0) {
                result.add(line.toString());
                line.setLength(0);
                width = 0;
            }
            line.append(c);
            width += w;
        }
        if (line.length() > 0) result.add(line.toString());
        return result;
    }

    /** 构建 ToUnicode 映射，保证中文可复制、可检索。 */
    private static String buildToUnicode(String title, List<String> lines) {
        Set<Character> chars = new LinkedHashSet<>();
        for (char c : title.toCharArray()) {
            if (c > 31) chars.add(c);
        }
        for (String line : lines) {
            for (char c : (line == null ? "" : line).toCharArray()) {
                if (c > 31) chars.add(c);
            }
        }

        StringBuilder cmap = new StringBuilder();
        cmap.append("/CIDInit /ProcSet findresource begin\n")
                .append("12 dict begin\n")
                .append("begincmap\n")
                .append("/CIDSystemInfo << /Registry (Adobe) /Ordering (UCS) /Supplement 0 >> def\n")
                .append("/CMapName /Adobe-Identity-UCS def\n")
                .append("/CMapType 2 def\n")
                .append("1 begincodespacerange\n<0000> <FFFF>\nendcodespacerange\n");

        List<Character> list = new ArrayList<>(chars);
        int index = 0;
        while (index < list.size()) {
            int count = Math.min(100, list.size() - index);
            cmap.append(count).append(" beginbfchar\n");
            for (int i = 0; i < count; i++) {
                String code = String.format("%04X", (int) list.get(index + i));
                cmap.append('<').append(code).append("> <").append(code).append(">\n");
            }
            cmap.append("endbfchar\n");
            index += count;
        }

        cmap.append("endcmap\n")
                .append("CMapName currentdict /CMap defineresource pop\n")
                .append("end\nend\n");
        return cmap.toString();
    }
}
