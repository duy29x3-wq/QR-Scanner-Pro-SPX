# QR Scanner Pro SPX V2

Ứng dụng Android Java, đọc QR/mã vạch và OCR chữ trên tem SPX bằng Google ML Kit. Quét QR/mã vạch nhanh dùng ZXing. Chụp ảnh tem SPX với camera hệ thống hoặc chọn ảnh trong thư viện. Ưu tiên chuỗi vận đơn SPXVN... từ barcode trước, sau đó OCR; nếu không tìm thấy hiển thị cảnh báo, không tự bịa mã. Lịch sử lưu cục bộ.

## Build APK chỉ bằng điện thoại
1. Giải nén ZIP, tải **nội dung** thư mục `QR-Scanner-Pro-SPX` lên một GitHub repository mới. Đảm bảo `.github/workflows/build-apk.yml` được giữ đúng đường dẫn (thư mục dấu chấm có thể bị ẩn trong trình quản lý tệp).
2. GitHub → Actions → Build QR Scanner Pro APK → Run workflow. Hoặc push lên nhánh main để chạy tự động.
3. Sau khi workflow thành công, vào run → Artifacts → QR-Scanner-Pro-APK → tải ZIP, giải nén `app-debug.apk` rồi cài trên Android.
4. Nếu GitHub từ chối upload cả thư mục, có thể tạo repository bằng trình duyệt và upload từng tệp theo đúng đường dẫn hoặc dùng ứng dụng Git hỗ trợ push thư mục.

## Ghi chú
- Google ML Kit barcode scanning 17.3.0, text recognition Latin 16.0.1. Thư viện tải khi build, nhận diện trên máy khi sử dụng.
- OCR không đảm bảo đúng 100%, đặc biệt với ảnh mờ/nghiêng. Kiểm tra lại mã trước khi dùng trong nghiệp vụ.
- Mã vận đơn ngoài mẫu `SPXVN...` vẫn được hiển thị trong danh sách mã đọc được, nhưng không tự gán là vận đơn.
- Camera chụp tem tạo ảnh trong thư viện điện thoại.
- Chưa kiểm thử APK trên thiết bị thật trong môi trường này.
