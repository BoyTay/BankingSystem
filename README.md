# 🏦 Banking System — Design Patterns Demo

Dự án Java Maven mô phỏng hệ thống **Ngân hàng / Ví điện tử**, minh họa **9 Design Patterns**
thuộc 3 nhóm: Creational, Structural, và Behavioral.

> **Mục đích**: Bài tập môn Thiết kế phần mềm (Software Design Patterns).

---

## 📋 Yêu cầu hệ thống

- **Java**: 17+ (Maven và test đã chạy trên OpenJDK 25)
- **Build tool**: Maven 3.9+ (hoặc dùng Maven Wrapper `mvnw` đi kèm)
- Nếu `JAVA_HOME` chưa được đặt, các file `.bat` sẽ thử tìm JDK qua `javac` trong `PATH`.

## 🚀 Cách chạy ứng dụng

### Cách 1: Giao diện Đồ họa Desktop (JavaFX Modern UI) [Khuyên dùng]
Chạy trực tiếp file script:
- Nhấp đúp chuột vào `run-gui.bat` (hoặc `run.bat` rồi chọn 1).

Hoặc dùng lệnh terminal:
```powershell
$env:JAVA_HOME = "C:\duong-dan-den-jdk"
.\mvnw.cmd javafx:run
```

### Cách 2: Giao diện Console Terminal (CLI)
- Nhấp đúp chuột vào `run.bat` rồi chọn 2.

Hoặc dùng lệnh terminal:
```powershell
$env:JAVA_HOME = "C:\duong-dan-den-jdk"
.\mvnw.cmd exec:java -D"exec.mainClass=com.banking.Main"
```

---

## 🎯 Bảng ánh xạ Design Patterns

| #  | Pattern       | Nhóm        | Class(es)                                         | Use Case                                                        |
|----|---------------|-------------|---------------------------------------------------|-----------------------------------------------------------------|
| 1  | **Builder**   | Creational  | `Account.Builder`                                 | Xây dựng đối tượng Account với nhiều thuộc tính tùy chọn        |
| 2  | **Singleton** | Creational  | `DatabaseManager`                                 | Đảm bảo chỉ có một instance quản lý lưu trữ dữ liệu           |
| 3  | **Prototype** | Creational  | `TransferTemplate`                                | Nhân bản mẫu giao dịch chuyển khoản để tái sử dụng             |
| 4  | **Proxy**     | Structural  | `BankAccount`, `RealAccount`, `AccountProxy`      | Kiểm soát truy cập tài khoản dựa trên vai trò (RBAC)           |
| 5  | **Facade**    | Structural  | `BankingFacade`                                   | Một API cho nạp/rút/chuyển/hoàn tác; điều phối Command + Strategy + Observer |
| 6  | **Command**   | Behavioral  | `Command`, `TransferCommand`, `TransactionHistory`| Thực thi & hoàn tác (undo) giao dịch chuyển khoản              |
| 7  | **Observer**  | Behavioral  | `AccountObserver`, `SmsNotifier`, `EmailNotifier` | Thông báo SMS + Email khi có sự kiện trên tài khoản             |
| 8  | **State**     | Behavioral  | `AccountState`, `ActiveState`, `LockedState`      | Thay đổi hành vi deposit/withdraw theo trạng thái tài khoản     |
| 9  | **Strategy**  | Behavioral  | `FeeStrategy`, `StandardFeeStrategy`, `PremiumFeeStrategy`, `TieredFeeStrategy` | Tính phí theo loại tài khoản |

Sơ đồ lớp, sơ đồ tuần tự, lý do dùng từng pattern và giới hạn mô phỏng nằm trong [docs/DESIGN.md](docs/DESIGN.md).

---

## 📁 Cấu trúc dự án

```
src/main/java/com/banking/
├── model/
│   ├── Account.java              ← Builder pattern
│   ├── Transaction.java
│   └── enums/
│       ├── AccountType.java      (STANDARD, SAVINGS, PREMIUM)
│       └── AccountStatus.java    (ACTIVE, LOCKED, SUSPENDED, CLOSED)
├── pattern/
│   ├── creational/
│   │   ├── DatabaseManager.java  ← Singleton
│   │   └── TransferTemplate.java ← Prototype
│   ├── structural/
│   │   ├── BankAccount.java      ← Interface cho Proxy
│   │   ├── RealAccount.java      ← Real Subject
│   │   ├── AccountProxy.java     ← Proxy (RBAC)
│   │   └── BankingFacade.java    ← Facade
│   └── behavioral/
│       ├── Command.java          ← Command interface
│       ├── TransferCommand.java  ← Concrete Command
│       ├── TransactionHistory.java ← Invoker (undo stack)
│       ├── AccountObserver.java  ← Observer interface
│       ├── SmsNotifier.java      ← Concrete Observer
│       ├── EmailNotifier.java    ← Concrete Observer
│       ├── AccountState.java     ← State interface
│       ├── ActiveState.java      ← Concrete State
│       ├── LockedState.java      ← Concrete State
│       ├── FeeStrategy.java      ← Strategy interface
│       ├── StandardFeeStrategy.java  ← 0.1% fee
│       ├── PremiumFeeStrategy.java   ← 0% fee
│       └── TieredFeeStrategy.java    ← Tiered fee
├── service/
│   ├── AccountService.java
│   ├── TransactionService.java
│   └── NotificationService.java
├── ui/                           ← Giao diện đồ họa JavaFX (Modern Navy Theme)
│   ├── GuiLauncher.java          ← Launcher trung gian tương thích Java 17+
│   ├── MainApp.java              ← JavaFX Application entry point
│   ├── MainLayout.java           ← Sidebar Navy (#0A2342) + Content Switcher
│   ├── DashboardView.java        ← Tổng quan số dư thẻ & Lịch sử GD (Singleton, Observer)
│   ├── AccountView.java          ← Mở & Quản lý TK, Khóa/Mở (Builder, Strategy, State)
│   ├── DepositWithdrawView.java  ← Nạp & Rút tiền (State check, Observer, Strategy)
│   ├── TransferView.java         ← Chuyển khoản & Mẫu định kỳ (Facade, Command, Prototype)
│   ├── HistoryView.java          ← Lịch sử GD & Hoàn tác (Command undo stack)
│   ├── ProxyDemoView.java        ← Kiểm soát truy cập phân quyền RBAC (Protection Proxy)
│   ├── UIContext.java            ← Quản lý trạng thái & dữ liệu quan sát được (Observable)
│   └── UiUtils.java              ← Trợ giúp định dạng VND, huy hiệu Pattern & Hộp thoại
└── Main.java                     ← Console menu CLI truyền thống
```

---

## 🖥️ Các màn hình giao diện đồ họa (JavaFX)

1. **Dashboard (Tổng quan)**:
   - Thẻ hiển thị số dư tài khoản trực quan với hiệu ứng đổ bóng, huy hiệu gói tài khoản.
   - Bảng giao dịch gần đây được cập nhật thời gian thực.
   - Hộp nhật ký Observer (SMS, Email, UI feed) bắt kịp thời mọi biến động.
   - Minh họa: `Singleton`, `Observer`.

2. **Mở & Quản lý tài khoản**:
   - Biểu mẫu mở tài khoản tích hợp **Visual Code Inspector** hiển thị cú pháp Builder Pattern thực thi.
   - Bảng quản lý cho phép thao tác Khóa (Lock) / Mở khóa (Unlock) tức thì.
   - Minh họa: `Builder`, `Strategy`, `State`.

3. **Nạp & Rút tiền**:
   - Nạp tiền tự động kích hoạt thông báo qua đa kênh.
   - Rút tiền kiểm tra điều kiện trạng thái (`LockedState` sẽ lập tức chặn giao dịch và thông báo lỗi).
   - Minh họa: `State`, `Strategy`, `Observer`, `Facade`.

4. **Chuyển khoản & Mẫu giao dịch**:
   - Chuyển khoản liên tài khoản với tính phí tự động.
   - Khu vực Prototype Pattern cho phép lưu mẫu (`TransferTemplate`) và nhân bản độc lập (`clone()`).
   - Minh họa: `Facade`, `Command`, `Prototype`, `Strategy`.

5. **Lịch sử giao dịch & Hoàn tác**:
   - Bộ lọc giao dịch theo số tài khoản.
   - Nút **Hoàn tác (Undo)** lệnh chuyển khoản gần nhất nhờ ngăn xếp `TransactionHistory`.
   - Minh họa: `Command`.

6. **Phân quyền truy cập (Proxy Demo)**:
   - Thử nghiệm các vai trò: `ADMIN`, `USER`, `READONLY`.
   - Khi chọn `READONLY` và cố nạp/rút tiền, `AccountProxy` sẽ chủ động ném `SecurityException` và hiển thị cảnh báo bảo mật.
   - Minh họa: `Protection Proxy (RBAC)`.

---

## 🎮 Menu console

```
┌──────────────── MENU ────────────────┐
│  1. Mở tài khoản       (Builder)     │
│  2. Nạp tiền           (Observer)    │
│  3. Rút tiền           (State)       │
│  4. Chuyển khoản       (Facade+Cmd)  │
│  5. Chuyển khoản mẫu   (Prototype)   │
│  6. Khóa/Mở khóa TK   (State)       │
│  7. Xem lịch sử GD                   │
│  8. Hoàn tác GD cuối   (Command)     │
│  9. Demo Proxy         (Proxy)       │
│ 10. Danh sách tài khoản              │
│  0. Thoát                            │
└──────────────────────────────────────┘
```

---

## ✅ Kịch bản demo đầy đủ

1. **Mở 2 tài khoản**: STANDARD (Nguyễn A) + PREMIUM (Trần B) → *Builder + Strategy*
2. **Nạp tiền** vào cả 2 tài khoản → *Observer (SMS + Email)*
3. **Chuyển khoản** từ A → B → *Facade + Command + Strategy*
4. **Chuyển khoản mẫu** (Prototype) → clone template + execute
5. **Hoàn tác** giao dịch vừa chuyển → *Command undo*; lịch sử xuất hiện giao dịch hoàn tác liên kết mã giao dịch gốc. Nếu tài khoản nhận đã chi hết tiền, thao tác bị từ chối và lệnh vẫn nằm trong danh sách chờ undo.
6. **Khóa tài khoản** A → *State (LockedState)*
7. **Rút tiền** từ A (bị khóa) → in "Tài khoản bị khóa"
8. **Mở khóa** A → *State (ActiveState)*
9. **Demo Proxy** → ADMIN/USER/READONLY access control
10. **Xem lịch sử** → hiển thị toàn bộ giao dịch

---

## 📝 Ghi chú

- Dữ liệu chỉ lưu trong RAM qua `DatabaseManager` (Singleton), mất khi thoát ứng dụng. Đây là ứng dụng mô phỏng, không kết nối ngân hàng thật.
- Tiền lưu ở lõi bằng `BigDecimal`, làm tròn đến 1 VND; giao diện cũ vẫn truyền/nhận `double` và lõi giới hạn giá trị đến 9.000.000.000.000.000 VND.
- Proxy là màn hình minh họa phân quyền riêng; giao diện chính chưa có đăng nhập và vai trò người dùng.
- Chạy kiểm thử: `./mvnw test` (Windows: `.\mvnw.cmd test`).
- Không sử dụng Spring hay Lombok.
- Mỗi file pattern đều có comment dòng 1: `// Pattern: [Tên] — [Mô tả]`
