# Chức năng Nhắc lịch - Booking Billiards App

## Tổng quan
Chức năng nhắc lịch giúp người dùng nhận thông báo trước khi đến giờ đặt bàn, đảm bảo không bỏ lỡ lịch hẹn.

## Các tính năng

### 1. Đặt nhắc lịch khi đặt bàn
- Khi đặt bàn, người dùng có thể chọn thời gian nhắc lịch:
  - 15 phút trước
  - 30 phút trước  
  - 1 giờ trước
  - Không nhắc lịch

### 2. Thông báo nhắc lịch
- Hiển thị notification với:
  - Tiêu đề: "⏰ Nhắc lịch đặt bàn"
  - Nội dung: Thông tin bàn và thời gian đặt
  - Icon: ic_notification
  - Priority: HIGH

### 3. Hủy nhắc lịch
- Tự động hủy nhắc lịch khi:
  - Người dùng hủy đặt bàn
  - Booking bị xóa

## Cách sử dụng

### Đặt nhắc lịch
1. Vào trang đặt bàn
2. Chọn thời gian và thông tin đặt bàn
3. Nhấn nút "Nhắc lịch" để chọn thời gian nhắc
4. Xác nhận đặt bàn
5. Hệ thống sẽ tự động đặt nhắc lịch

### Hủy nhắc lịch
1. Vào trang "Bàn đã đặt"
2. Nhấn nút "Hủy đặt bàn" trên booking cần hủy
3. Xác nhận hủy
4. Nhắc lịch sẽ tự động bị hủy

## Cấu trúc code

### Files chính:
- `ReminderReceiver.kt`: Xử lý sự kiện nhắc lịch và hiển thị notification
- `ReminderManager.kt`: Quản lý việc đặt/hủy nhắc lịch
- `ReminderDialog.kt`: Dialog chọn thời gian nhắc lịch
- `BookingFragment.kt`: Tích hợp chức năng đặt nhắc lịch
- `BookedTableAdapter.kt`: Tích hợp chức năng hủy nhắc lịch

### Layout files:
- `dialog_reminder_setting.xml`: Layout dialog chọn thời gian nhắc
- `fragment_booking.xml`: Thêm nút nhắc lịch
- `item_booked_table.xml`: Thêm nút hủy đặt bàn

## Permissions cần thiết

### AndroidManifest.xml:
```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
```

### Runtime permissions (Android 13+):
- `POST_NOTIFICATIONS`: Để hiển thị notification

## Lưu ý kỹ thuật

### 1. AlarmManager
- Sử dụng `AlarmManager.RTC_WAKEUP` để đảm bảo nhắc lịch hoạt động ngay cả khi thiết bị ở chế độ doze
- Sử dụng `setExactAndAllowWhileIdle()` để đảm bảo độ chính xác

### 2. Notification Channel
- Tạo notification channel với importance HIGH
- Bật vibration và lights cho notification

### 3. PendingIntent
- Sử dụng `FLAG_UPDATE_CURRENT` và `FLAG_IMMUTABLE` cho PendingIntent
- Sử dụng bookingId.hashCode() làm requestCode để tránh conflict

### 4. Timezone handling
- Xử lý cả format "yyyy-MM-dd'T'HH:mm:ss" và "HH:mm"
- Đảm bảo tính toán thời gian chính xác

## Testing

### Test cases:
1. Đặt bàn với nhắc lịch 15 phút trước
2. Đặt bàn với nhắc lịch 30 phút trước  
3. Đặt bàn với nhắc lịch 1 giờ trước
4. Đặt bàn không có nhắc lịch
5. Hủy đặt bàn và kiểm tra nhắc lịch bị hủy
6. Test notification hiển thị đúng thời gian

### Debug:
- Log được thêm vào ReminderManager để theo dõi việc đặt/hủy nhắc lịch
- Kiểm tra logcat với tag "ReminderManager" và "ReminderReceiver" 