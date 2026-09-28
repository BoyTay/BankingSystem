# 🏦 Banking System — Design Patterns Demo

Dự án Java Maven mô phỏng hệ thống **Ngân hàng / Ví điện tử**, minh họa **9 Design Patterns**
thuộc 3 nhóm: Creational, Structural, và Behavioral.

> **Mục đích**: Bài tập môn Mẫu thiết kế (Design Patterns).

---

## 📋 Yêu cầu hệ thống

- **Java**: 17+ (Maven và test đã chạy trên OpenJDK 25)
- **Build tool**: Maven 3.9+ (hoặc dùng Maven Wrapper `mvnw` đi kèm)
- Nếu `JAVA_HOME` chưa được đặt, các file `.bat` sẽ thử tìm JDK qua `javac` trong `PATH`.

## 🚀 Chạy ứng dụng

Trên Windows, mở PowerShell trong thư mục dự án và dùng **một lệnh duy nhất**:

```powershell
.\run-gui.bat
```

Cần JDK 17+; script tự tìm JDK qua `javac` nếu chưa đặt `JAVA_HOME`. Lần đầu ứng dụng yêu cầu tạo quản trị viên. Dữ liệu lưu tại `%USERPROFILE%\.vietbank\banking.db`. Ứng dụng sẽ từ chối mở phiên thứ hai dùng cùng tệp dữ liệu; hãy đóng cửa sổ đang chạy trước khi mở lại.

Các tệp `run.bat`, `compose.yaml` và `build-desktop.ps1` phục vụ thử nghiệm CLI, Docker và đóng gói; không cần dùng chúng cho luồng chạy desktop ở trên.

**Trạng thái UI:** luồng chạy chính dùng `MainApp` → `novabank_login.fxml` → `NovaBankShell`. Các màn Tổng quan, Tài khoản, Chuyển khoản và Lịch sử là bộ NovaBank FXML, đọc dữ liệu từ SQLite qua `UIContext` và gọi service/facade khi giao dịch. Thanh điều hướng chung mở thêm các màn JavaFX hiện có: Mở tài khoản, Nạp/Rút, Mẫu chuyển, Người dùng, Hồ sơ và Proxy. Ứng dụng mô phỏng chỉ xử lý VND; xuất lịch sử tài khoản ở định dạng CSV.

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
├── ui/                           ← Giao diện đồ họa JavaFX
│   ├── GuiLauncher.java          ← Launcher trung gian tương thích Java 17+
│   ├── MainApp.java              ← JavaFX Application entry point
│   ├── NovaBankShell.java        ← Điều hướng và phân quyền cho bộ FXML
│   ├── NovaBank*Controller.java  ← Đăng nhập, tổng quan, tài khoản, chuyển khoản, lịch sử
│   ├── MainLayout.java           ← Khung JavaFX trước đây
│   ├── DashboardView.java        ← Dashboard JavaFX trước đây
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

1. **Dashboard NovaBank (Tổng quan)**:
   - Hiển thị tổng số dư VND, phân bổ theo loại tài khoản, biểu đồ và giao dịch gần đây từ SQLite.
   - Cập nhật khi dữ liệu tài khoản hoặc giao dịch thay đổi.
   - Minh họa: `Singleton`, `Observer`.

2. **Tài khoản NovaBank & Mở tài khoản**:
   - Màn NovaBank cho chọn tài khoản, xem số dư và xuất lịch sử CSV.
   - ADMIN mở tài khoản trong màn JavaFX hiện có và khóa/mở khóa qua `AccountService`.
   - Minh họa: `Builder`, `Strategy`, `State`.

3. **Nạp & Rút tiền**:
   - Nạp tiền tự động kích hoạt thông báo qua đa kênh.
   - Rút tiền kiểm tra điều kiện trạng thái (`LockedState` sẽ lập tức chặn giao dịch và thông báo lỗi).
   - Minh họa: `State`, `Strategy`, `Observer`, `Facade`.

4. **Chuyển khoản NovaBank & Mẫu giao dịch**:
   - Chuyển khoản VND liên tài khoản qua `BankingFacade` với phí tự động và bước xác nhận.
   - Mục **Transfer templates** mở màn JavaFX cho phép lưu mẫu (`TransferTemplate`) và nhân bản độc lập (`clone()`).
   - Minh họa: `Facade`, `Command`, `Prototype`, `Strategy`.

5. **Lịch sử giao dịch NovaBank & Hoàn tác**:
   - Bộ lọc theo tài khoản, ngày, loại và số tiền; xuất CSV.
   - Nút **Hoàn tác (Undo)** lệnh chuyển khoản gần nhất nhờ ngăn xếp `TransactionHistory`.
   - Minh họa: `Command`.

6. **Phân quyền truy cập (Proxy Demo)**:
   - Thử nghiệm các vai trò: `ADMIN`, `USER`, `READONLY`.
   - Khi chọn `READONLY` và cố nạp/rút tiền, `AccountProxy` sẽ chủ động ném `SecurityException` và hiển thị cảnh báo bảo mật.
   - Minh họa: `Protection Proxy (RBAC)`.

---

## 🎮 Menu console

```
1. Mở tài khoản (ADMIN)
2. Nạp tiền
3. Rút tiền
4. Chuyển khoản
5. Chuyển khoản mẫu
6. Khóa/Mở khóa tài khoản (ADMIN)
7. Xem lịch sử giao dịch
8. Hoàn tác chuyển khoản cuối
9. Demo Proxy (ADMIN)
10. Danh sách tài khoản
11. Tạo người dùng (ADMIN)
12. Đổi mật khẩu
0. Thoát
```

Menu chỉ hiển thị các mục vai trò đăng nhập được phép dùng.

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

- Dữ liệu tài khoản, giao dịch và người dùng được lưu trong SQLite tại `%USERPROFILE%\.vietbank\banking.db` trên Windows (hoặc `~/.vietbank/banking.db`). Tắt ứng dụng trước khi sao lưu hoặc chép tệp này sang máy khác.
- Lần chạy đầu, ứng dụng yêu cầu tạo tài khoản ADMIN và mật khẩu tối thiểu 8 ký tự. ADMIN tạo thêm STAFF hoặc VIEWER tại mục **Người dùng**. STAFF thực hiện giao dịch; VIEWER chỉ xem. Mỗi người có thể đổi mật khẩu tại **Tài khoản đăng nhập**. Nếu quên mật khẩu ADMIN, hiện chưa có quy trình khôi phục tự động.
- Ứng dụng không tự tạo tài khoản hoặc tiền mẫu. Để bật dữ liệu trình diễn trên cơ sở dữ liệu trống, thêm JVM option `-Dbanking.demo.seed=true` khi chạy GUI.
- Có thể đổi vị trí dữ liệu bằng JVM option `-Dbanking.data.file=đường_dẫn_tệp.db`. Không chạy hai phiên ứng dụng trên cùng một tệp dữ liệu.
- Tiền lưu ở lõi bằng `BigDecimal`, làm tròn đến 1 VND; giao diện cũ vẫn truyền/nhận `double` và lõi giới hạn giá trị đến 9.000.000.000.000.000 VND.
- Proxy vẫn là màn hình minh họa pattern riêng. Đăng nhập ADMIN/STAFF/VIEWER kiểm soát các chức năng trong GUI và CLI.
- Đây là ứng dụng quản lý mô phỏng dùng cục bộ cho đồ án; giao dịch nạp/rút và SMS/Email không kết nối dịch vụ tài chính hay hệ thống gửi tin thật.
- Chạy kiểm thử: `./mvnw test` (Windows: `.\mvnw.cmd test`).
- Không sử dụng Spring hay Lombok.
- Mỗi file pattern đều có comment dòng 1: `// Pattern: [Tên] — [Mô tả]`
