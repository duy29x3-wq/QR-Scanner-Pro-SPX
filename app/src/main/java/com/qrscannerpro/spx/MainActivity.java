
package com.qrscannerpro.spx;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.*;
import java.util.regex.*;
import org.json.*;

public class MainActivity extends AppCompatActivity {

    private LinearLayout root;
    private TextView output, history;
    private String last = "";
    private Uri photoUri;
    private final int orange = Color.rgb(238, 105, 25);

    private final ActivityResultLauncher<String> pick =
        registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) analyze(uri);
            }
        );

    private final ActivityResultLauncher<Intent> capture =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK
                        && photoUri != null) {
                    analyze(photoUri);
                }
            }
        );

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        ui();
    }

    private TextView txt(String s, int size) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(Color.BLACK);
        t.setPadding(5, 12, 5, 12);
        return t;
    }

    private void btn(String s, Runnable action) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(orange)
        );
        root.addView(b);
        b.setOnClickListener(v -> action.run());
    }

    private void ui() {
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 28, 24, 28);
        scroll.addView(root);
        setContentView(scroll);

        TextView title = txt("QR Scanner Pro • SPX", 25);
        title.setTextColor(orange);
        root.addView(title);

        root.addView(txt(
            "Đọc mã vạch, QR và chữ trên tem SPX", 15
        ));

        btn("CHỤP TEM SPX", this::takePhoto);

        btn("QUÉT TỪ ẢNH ALBUM",
            () -> pick.launch("image/*"));

        output = txt("Chưa có kết quả", 16);
        output.setTextIsSelectable(true);
        root.addView(output);

        btn("SAO CHÉP KẾT QUẢ", () -> {
            if (!last.isEmpty()) {
                android.content.ClipboardManager cm =
                    (android.content.ClipboardManager)
                    getSystemService(CLIPBOARD_SERVICE);

                cm.setPrimaryClip(
                    ClipData.newPlainText("SPX", last)
                );

                Toast.makeText(this,
                    "Đã sao chép", Toast.LENGTH_SHORT).show();
            }
        });

        btn("CHIA SẺ", () -> {
            if (!last.isEmpty()) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, last);
                startActivity(Intent.createChooser(i, "Chia sẻ"));
            }
        });

        root.addView(txt("LỊCH SỬ QUÉT", 18));
        history = txt("", 13);
        root.addView(history);
        refreshHistory();
    }

    private void takePhoto() {
        try {
            android.content.ContentValues values =
                new android.content.ContentValues();

            values.put(
                MediaStore.Images.Media.DISPLAY_NAME,
                "SPX_" + System.currentTimeMillis() + ".jpg"
            );
            values.put(
                MediaStore.Images.Media.MIME_TYPE,
                "image/jpeg"
            );

            photoUri = getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            );

            if (photoUri == null)
                throw new Exception("Không tạo được ảnh");

            Intent intent =
                new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            intent.putExtra(
                MediaStore.EXTRA_OUTPUT, photoUri
            );

            intent.addFlags(
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            capture.launch(intent);

        } catch (Exception e) {
            error(e.getMessage());
        }
    }

    private void analyze(Uri uri) {
        output.setText("Đang nhận diện tem SPX...");

        try {
            InputImage image =
                InputImage.fromFilePath(this, uri);

            BarcodeScanner scanner =
                BarcodeScanning.getClient();

            TextRecognizer recognizer =
                TextRecognition.getClient(
                    TextRecognizerOptions.DEFAULT_OPTIONS
                );

            Task<List<Barcode>> barcodeTask =
                scanner.process(image);

            Task<com.google.mlkit.vision.text.Text> textTask =
                recognizer.process(image);

            Tasks.whenAllComplete(barcodeTask, textTask)
                .addOnCompleteListener(done -> {

                    ArrayList<String> codes = new ArrayList<>();
                    String ocr = "";

                    if (barcodeTask.isSuccessful()) {
                        for (Barcode b : barcodeTask.getResult()) {
                            String value = b.getRawValue();
                            if (value != null && !value.isEmpty()) {
                                codes.add(value);
                            }
                        }
                    }

                    if (textTask.isSuccessful()) {
                        ocr = textTask.getResult().getText();
                    }

                    scanner.close();
                    recognizer.close();

                    if (codes.isEmpty() && ocr.trim().isEmpty()) {
                        error("Không đọc được. Hãy chụp rõ hơn.");
                        return;
                    }

                    String tracking = findTracking(codes, ocr);

                    StringBuilder result = new StringBuilder();

                    if (tracking != null) {
                        result.append("MÃ VẬN ĐƠN:\n")
                              .append(tracking)
                              .append("\n\n");
                    } else {
                        result.append(
                            "CHƯA XÁC ĐỊNH ĐƯỢC MÃ VẬN ĐƠN\n\n"
                        );
                    }

                    result.append("MÃ QR / MÃ VẠCH:\n");

                    for (String code : codes) {
                        result.append(code).append("\n");
                    }

                    result.append("\nNỘI DUNG OCR:\n")
                          .append(ocr);

                    show(result.toString());
                });

        } catch (Exception e) {
            error(e.getMessage());
        }
    }

    private String findTracking(
        List<String> codes, String ocr
    ) {
        Pattern pattern = Pattern.compile(
            "SPXVN[A-Z0-9]{8,25}",
            Pattern.CASE_INSENSITIVE
        );

        for (String code : codes) {
            Matcher m = pattern.matcher(
                code.replaceAll("[\\s-]", "")
            );

            if (m.find()) {
                return m.group().toUpperCase(Locale.ROOT);
            }
        }

        Matcher m = pattern.matcher(
            ocr.replaceAll("(?i)SPX\\s+VN", "SPXVN")
        );

        if (m.find()) {
            return m.group().toUpperCase(Locale.ROOT);
        }

        return null;
    }

    private void show(String value) {
        last = value;
        output.setText(value);

        try {
            JSONArray old = new JSONArray(
                getPreferences(0).getString("history", "[]")
            );

            JSONArray next = new JSONArray();
            JSONObject entry = new JSONObject();

            entry.put("time", System.currentTimeMillis());
            entry.put("text", value);
            next.put(entry);

            for (int i = 0; i < Math.min(29, old.length()); i++) {
                next.put(old.getJSONObject(i));
            }

            getPreferences(0).edit()
                .putString("history", next.toString())
                .apply();

            refreshHistory();

        } catch (Exception ignored) {}
    }

    private void refreshHistory() {
        if (history == null) return;

        try {
            JSONArray arr = new JSONArray(
                getPreferences(0).getString("history", "[]")
            );

            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < Math.min(10, arr.length()); i++) {
                String value =
                    arr.getJSONObject(i).optString("text");

                sb.append(value.substring(
                    0, Math.min(100, value.length())
                )).append("\n\n");
            }

            history.setText(
                sb.length() == 0 ? "Chưa có lịch sử" : sb.toString()
            );

        } catch (Exception ignored) {}
    }

    private void error(String message) {
        output.setText("Lỗi: " + message);
    }
                     }
