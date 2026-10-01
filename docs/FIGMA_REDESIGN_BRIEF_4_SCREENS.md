# NovaBank — Brief thiết kế lại 4 màn hình cho Figma

> **Mục đích:** Tạo bản thiết kế desktop có thể bàn giao để triển khai bằng JavaFX. Dùng bốn ảnh chụp đã cung cấp làm mốc hiện trạng, đồng thời đọc phần “Đúng với chức năng hiện có” trước khi vẽ luồng tương tác. Đây là brief thiết kế; chưa phải yêu cầu sửa mã.

**Ảnh tham chiếu cần gửi kèm brief cho người thiết kế/Figma:** ảnh 1 `codex-clipboard-41ba37b8-d7ee-42eb-911a-9ee16872e63b.png` (Hồ sơ), ảnh 2 `codex-clipboard-9f9faeee-ad51-4d45-a463-86d29503a534.png` (Mở tài khoản), ảnh 3 `codex-clipboard-663fb1ef-cdb3-4433-82be-b2a51ea6b4a2.png` (Proxy), ảnh 4 `codex-clipboard-1b13c3c0-f251-4eef-8683-4d450052ace8.png` (Người dùng). Ảnh hiện nằm trong thư mục tạm của máy gửi, nên cần đính kèm trực tiếp khi chuyển brief.

## 1. Phạm vi và nguyên tắc

- **Bốn màn:** Hồ sơ/đổi mật khẩu (ảnh 1), Mở & quản lý tài khoản (ảnh 2), Proxy Demo (ảnh 3), Quản lý người dùng (ảnh 4).
- **Luồng ứng dụng:** `NovaBankShell` hiện hành. Giữ sidebar, logo, ngôn ngữ hình ảnh và quan hệ điều hướng với Overview, Transfer & Pay, Transfer templates, Cash, Transactions, Accounts. Không thiết kế lại `MainLayout` cũ hay các màn ngoài phạm vi.
- **Đối tượng:** sinh viên trình diễn 9 design patterns trước giảng viên; người xem phải nhận ra thao tác, kết quả và pattern liên quan trong vài giây. Vẫn ưu tiên tác vụ ngân hàng dễ hiểu hơn thuật ngữ kỹ thuật.
- **Nền tảng:** ứng dụng desktop JavaFX, chuột và bàn phím. Thiết kế frame chính **1440 × 900**; thêm bản kiểm tra **1024 × 768**. Nội dung dài cuộn theo vùng chính; sidebar không che nội dung và badge trạng thái cuối sidebar phải luôn truy cập được.
- **Ngôn ngữ:** dùng tiếng Việt nhất quán cho điều hướng và hành động; giữ tên lớp/pattern tiếng Anh trong badge, tooltip hoặc phần giải thích học thuật. Ví dụ “Tổng quan”, “Chuyển tiền”, “Mẫu chuyển tiền”, “Giao dịch”, “Tài khoản”, “Người dùng”, “Mô phỏng Proxy”, “Mở tài khoản”, “Đăng xuất”.

## 2. Hiện trạng nhìn thấy trong bốn ảnh

| Màn | Điểm tốt cần giữ | Vấn đề cần giải quyết |
|---|---|---|
| Hồ sơ | Sidebar thống nhất; form đổi mật khẩu đơn giản | Phần lớn diện tích trống; tiêu đề chưa mô tả rõ mục đích; nút hành động quá yếu; badge SQLite có thể nằm sát hoặc dưới mép cửa sổ thấp |
| Mở tài khoản | Form, mô tả phí theo loại tài khoản, đoạn mã Builder, bảng trạng thái cùng trên một trang | Inspector hẹp và cuộn ngang; mã dễ bị hiểu là mã nguồn đang chạy; bảng nhiều cột nhưng thiếu nhấn mạnh dòng mới tạo; chữ/badge ở đầu trang dàn ngang quá dài |
| Proxy Demo | Vai trò, thao tác và audit log đã tạo thành tình huống demo | “Vai trò” dễ bị nhầm với vai trò đăng nhập; kết quả cho phép/từ chối chỉ nằm trong log và dialog; đoạn giải thích bị cắt ở mép phải; nhãn “thử nghiệm” chưa nói rõ nạp/rút có thể đổi số dư |
| Người dùng | Form tạo người dùng ngắn, có chọn vai trò | Tiêu đề “Quản lý” nhưng không thấy danh sách/kết quả trên trang; khoảng trống lớn; chưa có hướng dẫn rõ về vai trò và phản hồi sau tạo |

Ảnh chụp có thông báo của Snipping Tool ở góc dưới ở ảnh 2/3; đó là lớp phủ của hệ điều hành, **không** đưa vào thiết kế NovaBank.

## 3. Hướng hình ảnh chung

Giữ bản sắc hiện có: sidebar xanh navy, logo xanh ngọc, nền làm việc xám rất nhạt, thẻ trắng, nét viền nhẹ. Thiết kế mới nên gọn, tin cậy, ưu tiên dữ liệu và kết quả thao tác; tránh gradient trang trí hoặc nhiều màu nhấn cạnh tranh. Các màu sau là **mốc từ CSS hiện tại**, Figma cần chuẩn hóa thành semantic tokens thay vì rải màu trực tiếp trên từng màn:

| Token đề xuất | Giá trị tham chiếu | Cách dùng |
|---|---|---|
| `nav/surface` | `#1E2D4A` | Sidebar |
| `brand/primary` | `#00C476` | Logo và điểm nhấn thành công |
| `canvas` | `#F8FAFC` | Nền vùng nội dung |
| `surface` | `#FFFFFF` | Card, form, bảng |
| `text/primary` | `#1E293B` | Tiêu đề và nội dung chính |
| `text/secondary` | `#475569` | Mô tả |
| `border` | `#E2E8F0` | Viền card và trường nhập |
| `info` | `#2563EB` | Thông tin và đường dẫn |
| `danger` | `#DC2626` | Từ chối, khóa, lỗi |
| `warning` | `#D97706` | Cảnh báo thao tác ghi |

- Dùng lưới khoảng cách 4/8 px: padding card 24 px, khoảng giữa section 24 px, khoảng giữa field 12–16 px. Giữ sidebar khoảng 248–280 px, vùng nội dung tối thiểu có padding 24–32 px.
- Tiêu đề trang khoảng 24 px; tiêu đề card 16–18 px; nội dung/form 14 px; nhãn phụ và trạng thái không nhỏ hơn 12 px. Chọn một hệ font sans phù hợp desktop, ưu tiên font đã có trong ứng dụng (`Segoe UI`/`Inter`) để triển khai thực tế.
- Nút chính phải nổi bật và nhất quán; nút nguy hiểm dùng màu cảnh báo khi thao tác thực sự thay đổi dữ liệu. Không chỉ dùng màu để biểu đạt trạng thái: luôn có chữ và biểu tượng.
- Định nghĩa component chung trong Figma: `SidebarItem`, `PageHeader`, `PatternBadge`, `Card`, `TextField`, `PasswordField`, `Select`, `RadioGroup`, `PrimaryButton`, `SecondaryButton`, `DangerButton`, `StatusBadge`, `InlineAlert`, `Toast`, `TableRow`, `EmptyState`, `AuditEntry`.
- Có trạng thái default, hover, focus bàn phím, disabled, loading, success và error cho các control cần thiết. Text phải xuống dòng hoặc có tooltip hợp lý; không để cắt nội dung quan trọng hay cuộn ngang cả trang.

## 4. Màn 1 — Hồ sơ & bảo mật

### Mục tiêu

Người dùng nhận ra tài khoản/vai trò đang đăng nhập và đổi mật khẩu an toàn, với phản hồi ngay tại form.

### Bố cục đề xuất

1. `PageHeader`: **Hồ sơ & bảo mật**; mô tả ngắn “Xem tài khoản đăng nhập và thay đổi mật khẩu”.
2. Trong vùng nội dung có chiều rộng vừa phải (khoảng 560–680 px), đặt `IdentityCard`: tên đăng nhập và vai trò. Chỉ hiển thị dữ liệu đã có; không tự thêm email, số điện thoại hay ngày đăng nhập giả.
3. `PasswordCard`: hai field có nhãn ở phía trên (“Mật khẩu hiện tại”, “Mật khẩu mới”), gợi ý “Tối thiểu 8 ký tự”, nút chính **Đổi mật khẩu**. Không để placeholder thay vai trò của nhãn.
4. Vùng phản hồi ngay trong card: lỗi gắn với field hoặc thông báo thành công; vẫn có thể giữ dialog hệ thống khi triển khai nếu cần, nhưng prototype Figma cần thể hiện phản hồi tại chỗ.

### Trạng thái cần vẽ

Form mặc định; thiếu mật khẩu; mật khẩu mới dưới 8 ký tự; mật khẩu hiện tại sai; đổi thành công và hai field được xóa. Không hiển thị mật khẩu dưới dạng văn bản thường trong ảnh bàn giao.

### Đúng với chức năng hiện có

`ProfileView` hiện cho phép đổi mật khẩu bằng hai field và dùng dialog thành công/lỗi. Phản hồi inline là **đề xuất UI mới**, cần code khi triển khai.

## 5. Màn 2 — Mở & quản lý tài khoản

### Mục tiêu

Người xem thấy rõ chuỗi **nhập dữ liệu → chọn loại tài khoản/chiến lược phí → xem Builder minh họa → tạo tài khoản → thấy dòng mới trong bảng → khóa/mở khóa**.

### Bố cục đề xuất

1. `PageHeader`: **Mở & quản lý tài khoản**; badge nhỏ `Builder`, `Strategy`, `State` xuống dòng được khi cửa sổ hẹp.
2. Hàng đầu gồm form bên trái (ưu tiên chiều rộng khoảng 60%) và `Builder Preview` bên phải (40%). Ở 1024 px, xếp hai card theo chiều dọc để tránh mã cuộn ngang.
3. Form có tên chủ tài khoản, loại `STANDARD / SAVINGS / PREMIUM`, mô tả phí tương ứng, tiền nạp ban đầu và nút **Mở tài khoản**. Chiến lược phí cần đổi tức thì khi đổi loại; tier Savings ghi rõ ngưỡng dựa trên **số tiền giao dịch**: ≤1 triệu 0,1%; ≤10 triệu 0,05%; >10 triệu 0,02%.
4. Inspector đặt tên **“Minh họa lời gọi Builder”**, vì nội dung hiện tại được tạo từ các field và mang số tài khoản mẫu `ACC####`; không gắn nhãn “mã nguồn thực thi” hoặc trình bày như stack trace. Dùng font monospace 12–13 px, vùng đọc đủ cao, wrap dòng hợp lý hoặc chuyển sang các bước có nhãn; có thể kèm chú thích “Giá trị thực được cấp khi tạo”.
5. Bảng tài khoản toàn chiều ngang bên dưới: số tài khoản, chủ tài khoản, loại, số dư, chiến lược phí, trạng thái, thao tác. Cột trạng thái dùng badge chữ **Đang hoạt động/Đã khóa**. Dòng mới tạo được highlight tạm thời và cuộn vào tầm nhìn; giữ nút khóa/mở khóa theo từng dòng.
6. Sau tạo thành công, hiện số tài khoản vừa cấp và số dư trong một result banner ở trên bảng; cho phép tìm nhanh dòng vừa tạo mà không phụ thuộc duy nhất vào toast/dialog.

### Trạng thái cần vẽ

Trống; điền hợp lệ; đổi từng loại tài khoản và mô tả phí; tên trống; tiền nạp sai; tạo thành công và dòng mới; tài khoản hoạt động; tài khoản bị khóa; lỗi dịch vụ.

### Đúng với chức năng hiện có

`AccountView` đã có Builder form, mô tả Strategy, code preview, bảng và nút State. Highlight dòng mới, result banner và bố cục thích ứng là **đề xuất UI mới**. Preview là chuỗi minh họa được dựng từ form, không phải bản chụp chính xác câu lệnh mà `AccountService` đã chạy.

## 6. Màn 3 — Mô phỏng Proxy

### Mục tiêu

Trong một lượt nhìn phải thấy **vai trò mô phỏng**, phương thức gọi qua Proxy và kết quả **được phép/bị từ chối**. Demo dễ làm: chọn `READONLY`, bấm xem số dư (cho phép), bấm nạp/rút (bị từ chối).

### Bố cục đề xuất

1. `PageHeader`: **Mô phỏng phân quyền Proxy** và badge `Proxy`.
2. Cột trái là `Scenario Card`: tài khoản mục tiêu, **Vai trò mô phỏng** `ADMIN / USER / READONLY`, số tiền thử nghiệm và ba hành động **Xem số dư**, **Nạp tiền**, **Rút tiền**. Bên dưới radio có chú thích: “Vai trò ở đây dùng để trình diễn AccountProxy; không thay đổi vai trò đăng nhập NovaBank”.
3. Ngay dưới hàng nút đặt `Outcome Panel` lớn: trạng thái, tên phương thức, vai trò, lý do và số dư trước/sau nếu có thay đổi. Mẫu kết quả `READONLY + deposit()` phải ghi **Bị từ chối — không đổi số dư**; `READONLY + getBalance()` ghi **Được phép**.
4. Cột phải là `Audit Console` có dòng log theo thời gian, màu/biểu tượng phân biệt thành công và từ chối; chữ xuống dòng, không cần cuộn ngang để đọc thông điệp. Nút **Xóa log** chỉ xóa phần hiển thị log.
5. `How it works` ở dưới dùng sơ đồ ngắn `Người dùng → AccountProxy → kiểm tra quyền → RealAccount` và tối đa ba ý giải thích; cho văn bản xuống dòng đầy đủ, không cắt ở mép phải.
6. Cảnh báo dễ thấy cạnh hành động ghi: **“Nạp/rút qua Proxy hiện thay đổi số dư của tài khoản được chọn trong dữ liệu mô phỏng.”** Nếu thiết kế muốn sandbox không đổi dữ liệu, đánh dấu đó là thay đổi nghiệp vụ riêng, không giả định đã có.

### Trạng thái cần vẽ

Chưa thao tác; thiếu tài khoản; số tiền không hợp lệ; `READONLY` xem số dư; `READONLY` bị chặn nạp/rút; `USER/ADMIN` nạp/rút thành công; lỗi nghiệp vụ khác quyền truy cập; log trống sau khi xóa. Với thao tác ghi được phép, prototype nên có bước xác nhận để người dùng nhận ra số dư sẽ thay đổi.

### Đúng với chức năng hiện có

`ProxyDemoView` cho chọn vai trò giả lập độc lập với đăng nhập; `AccountProxy` chặn `READONLY` đối với nạp/rút. Nạp/rút hợp lệ hiện đi qua `RealAccount` và cập nhật tài khoản được chọn; `Outcome Panel` và bước xác nhận là **đề xuất UI mới**.

## 7. Màn 4 — Quản lý người dùng

### Mục tiêu

Admin tạo tài khoản đăng nhập và nhìn thấy kết quả rõ ràng, không để trang trống sau form.

### Bố cục đề xuất

1. `PageHeader`: **Người dùng & quyền truy cập**; phụ đề ngắn nêu `ADMIN` quản trị, `STAFF` giao dịch, `VIEWER` chỉ xem.
2. Card **Tạo người dùng** có nhãn rõ cho tên đăng nhập, mật khẩu, vai trò và nút **Tạo người dùng**. Gợi ý tên hợp lệ 3–32 ký tự chữ/số/gạch dưới; mật khẩu tối thiểu 8 ký tự. Không hiển thị mật khẩu trong bất kỳ danh sách hoặc thông báo nào.
3. Ngay dưới form có `Creation Result` hiển thị tên và vai trò vừa tạo; trường hợp trùng tên hoặc dữ liệu sai có lỗi tại field. Đây là phần có thể thiết kế trước mà không cần danh sách backend.
4. Vùng **Danh sách người dùng** là hướng mở rộng đề xuất: bảng tên đăng nhập, vai trò, trạng thái và tìm kiếm. Chỉ vẽ dữ liệu mẫu được gắn nhãn **sample**, không thể hiện nút sửa/xóa/vô hiệu hóa như tính năng đã hoạt động.

### Trạng thái cần vẽ

Form mặc định; sai định dạng tên; mật khẩu ngắn; thiếu vai trò; tên đã tồn tại; tạo thành công. Nếu vẽ danh sách: loading, empty, có dữ liệu và kết quả tìm kiếm trống.

### Đúng với chức năng hiện có

`UserView` hiện chỉ có form tạo và dialog kết quả; `AuthService` hiện chưa có API liệt kê người dùng. **Danh sách người dùng, tìm kiếm và trạng thái tài khoản cần thiết kế + bổ sung API/JavaFX sau**, không được mô tả là tính năng sẵn có. Nếu phạm vi triển khai không thêm backend, dùng `Creation Result` và bảng giải thích vai trò thay cho danh sách.

## 8. Sidebar và trạng thái SQLite dùng chung

- Giữ logo và nhóm điều hướng. Nhóm `ADMIN TOOLS` đổi nhãn hiển thị thành **Quản trị** nếu chọn giao diện tiếng Việt.
- Sidebar có ba vùng: logo trên; menu có thể cuộn ở giữa; tài khoản, đăng xuất và `DatabaseStatus` ghim ở dưới. Ở frame 1024 × 768, badge SQLite vẫn hiển thị hoặc truy cập được mà không bị cắt.
- Badge có ít nhất hai biến thể: **“SQLite sẵn sàng”** và **“SQLite không khả dụng”**, kèm biểu tượng/chữ; không dùng chấm màu đơn lẻ. Tooltip/chi tiết có thể cho thấy `DatabaseManager.getInstance()` và mã định danh Singleton phục vụ demo.
- Trạng thái phải phản ánh kiểm tra kết nối thực; Figma chỉ mô tả các trạng thái, không biến nó thành nhãn luôn xanh. Nếu không kết nối, nội dung chính cần có empty/error state như ứng dụng hiện tại.

## 9. Luồng prototype Figma cần nối

1. Sidebar → **Hồ sơ & bảo mật** → nhập mật khẩu → lỗi hoặc thành công.
2. Sidebar → **Mở tài khoản** → đổi loại tài khoản để thấy Strategy đổi → nhập form → xem Builder Preview đổi → tạo → banner kết quả và dòng mới → khóa/mở để thấy State.
3. Sidebar → **Mô phỏng Proxy** → chọn `READONLY` → xem số dư thành công → thử nạp tiền → Outcome Panel báo từ chối và audit log ghi lại.
4. Đổi vai trò mô phỏng sang `USER` → nạp tiền → xác nhận → Outcome Panel và số dư cập nhật.
5. Sidebar → **Người dùng** → tạo `STAFF` → xem kết quả; thêm nhánh tên trùng và mật khẩu ngắn.

Các đường nối trên cần đủ để người xem thử cả trạng thái thành công lẫn thất bại, không chỉ một ảnh tĩnh mỗi màn.

## 10. Bàn giao mong muốn từ Figma

- Một trang `Foundations` cho màu, chữ, khoảng cách, grid và quy tắc responsive desktop.
- Một trang `Components` với biến thể và trạng thái của các component dùng chung.
- Bốn frame chính 1440 × 900 và bốn frame 1024 × 768; thêm các frame trạng thái tối thiểu nêu ở từng màn.
- Prototype nối đúng năm luồng ở mục 9, có ghi chú phân biệt **chức năng đã có** và **đề xuất cần phát triển**.
- Chú thích bàn giao: tên component, padding/gap, kích thước tối thiểu, quy tắc wrap/scroll, focus order, nội dung thông báo và hành vi khi dữ liệu trống/lỗi.
- Checklist duyệt: không cắt chữ; không cuộn ngang toàn trang; trạng thái lỗi/thành công có chữ rõ; badge SQLite hiện ở cửa sổ thấp; Proxy không che giấu việc nạp/rút làm đổi dữ liệu; mật khẩu không xuất hiện ở preview hay bảng.

## 11. Prompt ngắn có thể dán vào Figma AI

> Thiết kế lại bốn màn desktop JavaFX của NovaBank theo brief này và bốn ảnh hiện trạng: Hồ sơ & bảo mật, Mở & quản lý tài khoản, Mô phỏng Proxy, Người dùng & quyền truy cập. Giữ nhận diện navy–xanh ngọc và sidebar chung; thiết kế ở 1440 × 900 và 1024 × 768. Làm rõ thao tác và kết quả để demo Builder, Strategy, State, Proxy và Singleton; dùng tiếng Việt nhất quán. Tạo components, variants, trạng thái lỗi/thành công và prototype cho các luồng trong mục 9. Không vẽ danh sách người dùng hoặc sandbox Proxy như chức năng đã có: nếu đưa vào, đánh dấu là đề xuất cần phát triển. Ưu tiên khả năng triển khai bằng JavaFX, chữ dễ đọc và không cắt nội dung.

## 12. Nguồn đối chiếu trong repository

- `src/main/java/com/banking/ui/NovaBankShell.java`: sidebar, điều hướng, badge SQLite.
- `src/main/java/com/banking/ui/ProfileView.java`: hồ sơ/đổi mật khẩu.
- `src/main/java/com/banking/ui/AccountView.java`: Builder/Strategy/State và bảng tài khoản.
- `src/main/java/com/banking/ui/ProxyDemoView.java`: vai trò giả lập, thao tác qua Proxy và audit log.
- `src/main/java/com/banking/ui/UserView.java`, `src/main/java/com/banking/service/AuthService.java`: form tạo và giới hạn API người dùng.
- `src/main/resources/com/banking/ui/novabank.css`: màu và thành phần thị giác hiện có.
