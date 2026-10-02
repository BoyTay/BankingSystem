# Kịch bản kiểm thử chức năng 9 Design Patterns — NovaBank

## 1. Mục tiêu và trạng thái hiện tại

Tài liệu này dùng để **chạy thử thủ công trên giao diện JavaFX** và ghi nhận liệu cả mã Java lẫn hành vi người dùng của 9 pattern đã hoàn chỉnh hay chưa. Luồng được kiểm tra là `MainApp → novabank_login.fxml → NovaBankShell`; `MainLayout` cũ nằm ngoài phạm vi. Singleton được chấm là `DatabaseManager`; `UIContext` chỉ là Singleton bổ sung phục vụ giao diện.

Tại thời điểm cập nhật, **20/20 test Maven đã đạt**, gồm bài test tự động thao tác các control JavaFX thật trên `NovaBankShell`. Các ô kiểm thử thủ công trong mục 3 vẫn để trống cho những bước chưa được bài test tự động bao phủ; không suy từ một phép kiểm tra cốt lõi sang toàn bộ ca phụ của pattern.

### Kết quả chạy tự động — 02/10/2026

Lệnh chạy: `.\mvnw.cmd test`. Báo cáo Surefire: `target/surefire-reports/TEST-com.banking.NovaBankFxmlTest.xml`; log chi tiết của lần chạy: `target/pattern-full-suite.log`. Bài test tạo dữ liệu trong SQLite kiểm thử do Maven cấu hình hoặc thư mục tạm của test, mở JavaFX `Stage`, tải FXML, thay đổi control và bấm nút/hộp xác nhận; không sử dụng cơ sở dữ liệu người dùng.

- [x] **Singleton:** trạng thái sidebar chứa `Singleton @...` và giữ nguyên sau khi chuyển màn.
- [x] **Builder:** nhập tên và 200.000 VND trên form `Open Account`, bấm **Mở tài khoản**, nhận số tài khoản mới và nhãn `Đã lưu vào SQLite`.
- [x] **Strategy:** đổi chính tài khoản vừa tạo `STANDARD → PREMIUM → STANDARD` qua control `Accounts`, xác nhận hộp thoại, kiểm tra SQLite tải lại gói PREMIUM, phí xem trước chuyển 10.000 VND đổi `0 → 10 VND`.
- [x] **State:** bấm khóa rồi mở khóa trên `Accounts`; khi khóa, màn `Cash` vô hiệu nút rút tiền và báo tài khoản bị khóa.
- [x] **Proxy:** tại `Proxy Demo`, vai trò READONLY xem được số dư; hai nút Nạp/Rút hiển thị từ chối, audit log tăng và số dư không đổi.
- [x] **Prototype:** lưu mẫu A → B rồi bấm **Sao chép**; danh sách tăng một mục và bản sao có dấu `(Bản sao)`.
- [x] **Observer:** sau nhiều lần chuyển màn, nạp 1.000 VND qua `Cash` tạo đúng một thông báo UI cho tài khoản nguồn; danh sách Overview dùng chính `notificationLogs`.
- [x] **Facade:** chuyển 10.000 VND qua màn `Transfer & Pay` và hộp xác nhận; phí 10 VND, tài khoản nguồn giảm 10.010 VND, tài khoản nhận tăng 10.000 VND, ledger có giao dịch mới.
- [x] **Command:** bấm **Hoàn tác** trên `Transactions`; hai số dư trở lại trước chuyển khoản, bộ đếm Undo giảm một và bản ghi đảo tham chiếu mã giao dịch gốc.

**Chấm tự động: 9/9 pattern đạt luồng cốt lõi; 20/20 test đạt, 0 failure/error.** Kết quả này là kiểm tra tương tác JavaFX tự động, chưa đánh giá bằng mắt bố cục, khả năng đọc và cảm nhận thao tác. Bài test tạo `NovaBankShell` với người dùng ADMIN giả lập sau khi xác nhận `MainApp` khởi động; thao tác đăng nhập bằng mật khẩu thật không nằm trong 9 phép kiểm tra trên.

**Các ca vẫn cần người kiểm tra trong mục 3:** Singleton khi SQLite lỗi; Builder tên trống; Strategy khởi động lại toàn bộ ứng dụng và quyền đổi gói của người không phải ADMIN; State thử chuyển tiền khi khóa; Prototype xóa bản sao và giữ mẫu gốc; Observer hai lần nạp liên tiếp sau refresh; Facade lỗi lưu giao dịch; Command khôi phục stack sau khởi động lại và trường hợp tài khoản nhận thiếu tiền để Undo.

**Cảnh báo không làm hỏng test:** JavaFX ghi cảnh báo CSS cho `-fx-max-width: -Infinity` và `-fx-pref-width: -Infinity` tại `.header-badge-tag` trong `novabank.css`; cần xem lại bố cục badge bằng mắt, không tính cảnh báo này là lỗi logic của pattern.

### Quy ước ghi kết quả

- **Đạt:** hành động tạo đúng thay đổi ở UI, số dư/phí/trạng thái khớp dữ liệu thực, và không có lỗi ngoài dự kiến.
- **Không đạt:** bất kỳ kết quả bắt buộc nào sai; ghi bước tái hiện, tài khoản, số tiền, thời điểm, ảnh màn hình và lỗi hiển thị.
- **Chưa kiểm:** chưa thao tác trên ứng dụng; không suy từ test tự động sang kết quả GUI.
- Với giao dịch, ghi **số dư trước và sau**, mã giao dịch, phí ở form/xác nhận/lịch sử. Đợi UI cập nhật xong trước khi so sánh.

### Đối chiếu mã hiện tại trước khi chạy GUI

- **Singleton — mã đạt:** `DatabaseManager` dùng instance tĩnh và constructor private; `NovaBankShell` gọi `getInstance()` rồi xác minh SQLite thật để cập nhật trạng thái.
- **Builder — mã đạt:** `Account.Builder` được `AccountService.openAccount()` dùng khi form mở tài khoản gửi yêu cầu.
- **Prototype — mã đạt với cấu trúc hiện tại:** `TransferTemplate.clone()` được nút Sao chép gọi; bản mẫu hiện chỉ chứa kiểu nguyên thủy và `String` bất biến.
- **Proxy — mã đạt trong màn demo:** `AccountProxy` và `RealAccount` cùng interface, Proxy chặn vai trò READONLY trước khi chuyển tiếp; các luồng chuyển tiền thông thường không đi qua Proxy.
- **Facade — mã đạt:** một lệnh `BankingFacade.transfer()` điều phối Strategy, Command, ghi lịch sử và Observer.
- **Command — mã đạt:** `TransferCommand.execute()/undo()` đi qua `TransactionHistory` và nút Hoàn tác của màn Transactions.
- **Observer — mã đạt:** UI observer được gắn khi tải/làm mới tài khoản; `UIContext` có guard theo identity để tránh gắn trùng.
- **State — mã đạt:** `ActiveState`/`LockedState` được đổi và lưu qua `AccountService`; luồng giao dịch chặn tài khoản nguồn bị khóa.
- **Strategy — mã đạt:** tài khoản đang tồn tại đổi được gói và `FeeStrategy`, lưu xuống SQLite; phép tính phí ở form và giao dịch lấy từ strategy hiện hành của tài khoản nguồn.

Các dòng “mã đạt” là kết quả đọc mã và test tự động, **không thay cho dấu Đạt của kiểm thử GUI** ở từng mục dưới đây.

## 2. Chuẩn bị phiên kiểm thử

1. Đăng nhập bằng tài khoản **ADMIN** trong ứng dụng đang dùng cơ sở dữ liệu kiểm thử. Không ghi mật khẩu vào tài liệu hoặc ảnh chụp.
2. Đóng các cửa sổ NovaBank khác đang dùng cùng SQLite, rồi mở ứng dụng qua cách chạy Maven/project thông thường. Không xóa hay sửa thủ công file cơ sở dữ liệu.
3. Xác nhận thanh bên hiển thị `SQLite sẵn sàng · Singleton @...`; nếu không, dừng các bước ghi dữ liệu và ghi lỗi kết nối.
4. Chọn hai tài khoản hoạt động khác nhau. Nếu chưa đủ hai, tạo tài khoản thử qua bước Builder bên dưới. Ký hiệu tài khoản nguồn là **A**, tài khoản nhận là **B**.
5. Đề xuất tạo **A = STANDARD**, tên `PATTERN DEMO A`, nạp ban đầu **200.000 VND**. B có thể là tài khoản có sẵn hoặc tài khoản thử thứ hai. Dùng số tiền nhỏ để các bước sau dễ đối chiếu.
6. Chụp/ghi số dư A, B và số lệnh hoàn tác trước phiên thử. Lịch sử giao dịch có thể đã chứa dữ liệu cũ; khi kiểm tra Command, chỉ so sánh **lệnh vừa tạo** và thay đổi của bộ đếm.

**Phiếu dữ liệu phiên chạy**

- Ngày/giờ: `________________`
- Bản Git/commit hoặc mô tả working tree: `________________`
- Người kiểm: `________________`
- Tài khoản A / loại gói / số dư đầu: `________________`
- Tài khoản B / loại gói / số dư đầu: `________________`
- Số lệnh hoàn tác đầu phiên: `________________`
- Thư mục ảnh/log minh chứng: `________________`

## 3. Chạy từng pattern theo thứ tự

### 3.1 Singleton — `DatabaseManager`

**Đường đi mã:** `NovaBankShell.refreshDatabaseStatus()` → `DatabaseManager.getInstance()` → `verifyStorage()` → SQLite. `UIContext` không được dùng để chấm Singleton chính.

**Thao tác**

1. Sau đăng nhập, ghi lại chuỗi `Singleton @...` và màu trạng thái SQLite ở cuối thanh bên.
2. Chuyển `Overview → Accounts → Transactions → Overview`; ghi lại chuỗi sau mỗi lần chuyển.
3. Nếu có môi trường kiểm thử lỗi kết nối riêng, thử tại đó và kiểm tra trạng thái lỗi. **Không cố phá hoặc xóa SQLite của dữ liệu đang dùng chỉ để tạo lỗi.**

**Kết quả cần đạt**

- Trạng thái “sẵn sàng” xuất hiện sau khi kết nối SQLite thật thành công, không phải dòng chữ cố định.
- Mã instance sau dấu `@` giữ nguyên trong cùng tiến trình; điều hướng không tạo `DatabaseManager` khác.
- Khi xác minh kết nối thất bại, thanh bên báo `SQLite không khả dụng` và nội dung ngân hàng không tiếp tục tải như thể dữ liệu vẫn sẵn sàng.

**Kết quả thực tế:** `________________`  **[ ] Đạt  [ ] Không đạt  [ ] Chưa kiểm**

### 3.2 Builder — mở tài khoản bằng form

**Đường đi mã:** `Open Account / AccountView.handleOpenAccount()` → `AccountService.openAccount()` → `new Account.Builder(...).type(...).balance(...).status(...).feeStrategy(...).state(...).build()` → `DatabaseManager.saveAccount()`.

**Thao tác**

1. Vào `Open Account`, nhập `PATTERN DEMO A`, chọn `STANDARD`, nhập nạp ban đầu `200000` và ghi lại phí/gói đang hiển thị.
2. Nếu có phần `Xem cách hệ thống hoạt động`, mở ra để quan sát các bước Builder trước/sau khi tạo.
3. Bấm **Mở tài khoản** một lần; ghi số tài khoản mới làm **A**.
4. Vào `Accounts` hoặc danh sách ở `Open Account` tìm A; kiểm tra chủ tài khoản, gói, số dư và trạng thái.
5. Thử nhấn tạo khi tên trống trên một form mới, kiểm tra ứng dụng báo lỗi và không sinh tài khoản.

**Kết quả cần đạt**

- A được tạo đúng một lần, có số tài khoản mới, `STANDARD`, `ACTIVE` và số dư 200.000 VND nếu nạp thành công.
- Luồng kỹ thuật sau tạo hiển thị `Đã lưu vào SQLite`; nếu nạp thất bại, UI phải phân biệt rõ **tài khoản đã tạo** với **tiền chưa nạp**.
- Tên trống bị chặn trước khi tạo; bấm tạo không làm xuất hiện bản ghi rỗng hoặc trùng.
- Sau khi đóng và mở lại ứng dụng ở bước Strategy, A vẫn tồn tại với dữ liệu đã lưu.

**Tài khoản A:** `________________`  **Kết quả thực tế:** `________________`  **[ ] Đạt  [ ] Không đạt  [ ] Chưa kiểm**

### 3.3 Strategy — đổi gói của chính tài khoản A lúc chạy

**Đường đi mã:** `Accounts / NovaBankAccountsController.handleApplyPlan()` → `AccountService.changeAccountType()` → `Account.setType()` + `Account.setFeeStrategy()` → `DatabaseManager.saveAccount()`; phí chuyển tiền lấy từ `from.getFeeStrategy()` trong `BankingFacade` và màn Transfer.

**Thao tác**

1. Vào `Accounts`, chọn A đang là `STANDARD`. Ở thẻ **Đổi gói tài khoản**, kiểm tra nút **Lưu thay đổi gói** đang vô hiệu khi vẫn chọn gói hiện tại.
2. Chọn `PREMIUM`. Đọc dòng phí mẫu cho giao dịch **5.000.000 VND** trước khi lưu.
3. Bấm **Lưu thay đổi gói**, xác nhận trong hộp thoại; kiểm tra gói/biểu phí của A cập nhật.
4. Vào `Transfer & Pay`, chọn A làm nguồn, B làm đích, nhập `10000` nhưng **chưa xác nhận giao dịch**. Kiểm tra phí ở tóm tắt là `0 VND`; mở hộp xác nhận để kiểm tra phí tại đó, rồi hủy.
5. Đóng và mở lại ứng dụng; đăng nhập, vào `Accounts` kiểm tra A vẫn là `PREMIUM` và phí dự kiến vẫn bằng 0.
6. Đổi A trở lại `STANDARD`, xác nhận lưu. Vào `Transfer & Pay`, nhập lại `10000`; phí dự kiến phải là **10 VND**. Để nguyên A ở `STANDARD` cho các bước Facade/Command.

**Kết quả cần đạt**

- Trước khi đổi, ví dụ 5.000.000 VND của `STANDARD` là **5.000 VND**; khi chọn `PREMIUM` dòng xem trước đổi thành **0 VND** nhưng dữ liệu thực chỉ đổi sau khi bấm lưu/xác nhận.
- Cùng tài khoản A dùng chiến lược mới ngay trong phiên chạy; chuyển 10.000 VND bằng `PREMIUM` có phí 0, bằng `STANDARD` có phí 10 VND.
- Form, hộp xác nhận và lịch sử/biên nhận phải cùng dùng phí của tài khoản nguồn tại thời điểm giao dịch.
- Khởi động lại vẫn khôi phục đúng gói và chiến lược từ SQLite; không chỉ đổi nhãn tạm thời trên UI.
- Người dùng không phải ADMIN không thấy thao tác đổi gói trên màn `Accounts`.

**Phí quan sát:** `STANDARD ____ / PREMIUM ____ / STANDARD sau đổi lại ____`  **[ ] Đạt  [ ] Không đạt  [ ] Chưa kiểm**

### 3.4 State — khóa và mở khóa A

**Đường đi mã:** `Accounts / handleToggleAccountState()` → `AccountService.lockAccount()/unlockAccount()` → `Account.setState(new LockedState()/ActiveState())` → lưu SQLite. `BankingFacade` kiểm tra trạng thái trước khi chuyển/rút.

**Thao tác**

1. Chọn A trong `Accounts`, ghi trạng thái đang hoạt động; bấm **Tạm khóa tài khoản**.
2. Kiểm tra nhãn trạng thái, mô tả bảo vệ và tên nút đổi thành hành động mở khóa.
3. Vào `Transfer & Pay`, chọn A làm nguồn, B làm đích, thử chuyển `10000`. Ghi thông báo lỗi, số dư và số dòng lịch sử mới.
4. Vào `Cash`, thử rút `1000` từ A; kiểm tra UI chặn và số dư giữ nguyên. Việc **nạp** vào tài khoản khóa được thiết kế cho phép, không lấy đó làm lỗi State.
5. Quay lại `Accounts`, bấm **Mở khóa tài khoản**; thử lại luồng chuyển tiền ở bước Facade sau.

**Kết quả cần đạt**

- A chuyển `ACTIVE → LOCKED → ACTIVE` rõ ràng trên UI và trong dữ liệu lưu.
- Khi khóa, chuyển tiền và rút tiền từ A bị chặn; không thêm giao dịch thành công, không trừ số dư và không tăng stack Undo.
- Sau khi mở khóa, A lại có thể làm tài khoản nguồn.

**Kết quả thực tế:** `________________`  **[ ] Đạt  [ ] Không đạt  [ ] Chưa kiểm**

### 3.5 Proxy — kiểm soát truy cập trên màn mô phỏng

**Đường đi mã:** `Proxy Demo / ProxyDemoView` → `new AccountProxy(new RealAccount(...), vaiTrò)` → `BankAccount` → `BankingFacade` nếu được cho phép.

**Thao tác**

1. Vào `Proxy Demo`, chọn A, ghi số dư hiện tại, chọn vai trò `READONLY`.
2. Bấm `getBalance()`; ghi kết quả và dòng audit.
3. Nhập `1000`, bấm `deposit()` rồi `withdraw()` dưới vai trò `READONLY`.
4. Chọn `ADMIN`, bấm `getBalance()` để xác nhận thao tác đọc vẫn được phép. Nếu muốn kiểm tra thao tác ghi được phép, nạp thử `1000` và ghi lại khoản tăng này trong số dư nền trước bước Facade.

**Kết quả cần đạt**

- `READONLY` xem được số dư, nhưng `deposit()` và `withdraw()` bị từ chối rõ ràng; số dư giữ nguyên sau hai lệnh bị chặn.
- Audit console và vùng kết quả cho biết hành động, vai trò, cho phép/từ chối và số dư trước/sau.
- Vai trò tại màn này là **vai trò mô phỏng để trình diễn Proxy**, không phải vai trò đăng nhập áp dụng cho mọi giao dịch thông thường; không trình bày đây là tầng phân quyền toàn hệ thống.

**Số dư trước/sau thao tác bị chặn:** `____ / ____`  **[ ] Đạt  [ ] Không đạt  [ ] Chưa kiểm**

### 3.6 Prototype — nhân bản mẫu chuyển tiền

**Đường đi mã:** `Transfer templates / NovaBankTemplatesController.handleSaveTemplate()` tạo `TransferTemplate`; nút **Sao chép** gọi `handleCloneTemplate()` → `TransferTemplate.clone()` → thêm bản sao vào danh sách.

**Thao tác**

1. Vào `Transfer templates`, chọn A và B, số tiền `10000`, nội dung `Mẫu kiểm thử Prototype`; lưu mẫu.
2. Tìm mẫu vừa lưu và bấm **Sao chép** đúng một lần.
3. Kiểm tra có hai mục riêng: mẫu gốc và mục mới mang dấu `(Bản sao)`; số tài khoản và số tiền giống nhau.
4. Nếu cần chứng minh độc lập trên UI, xóa **bản sao** và kiểm tra mẫu gốc vẫn tồn tại.

**Kết quả cần đạt**

- Nút sao chép làm tăng số mục thêm 1 và hiển thị bản sao riêng, không chỉ báo toast.
- Xóa bản sao không xóa mẫu gốc. Các trường hiện tại của `TransferTemplate` là `String` và `double`, nên `clone()` tạo đối tượng độc lập với dữ liệu hiện có.
- Mẫu trong màn này được lưu trong trạng thái UI hiện tại; **không lấy việc tồn tại sau khởi động lại làm điều kiện đạt Prototype**, vì mã hiện không lưu mẫu xuống SQLite.

**Số mẫu trước/sau sao chép:** `____ / ____`  **[ ] Đạt  [ ] Không đạt  [ ] Chưa kiểm**

### 3.7 Observer — một sự kiện, một thông báo UI cho mỗi tài khoản

**Đường đi mã:** `Cash / DepositWithdrawView` → `BankingFacade.deposit()` → `NotificationService.notifyAccountEvent()` → `Account.notifyObservers()` → `UIContext.attachUiObserver()` → danh sách `Thông báo tài khoản (Observer)` trên Overview. `UIContext` dùng identity map để tránh gắn trùng UI observer.

**Thao tác**

1. Chuyển qua `Overview → Accounts → Cash → Overview` vài lần để kích hoạt refresh/load lại trang.
2. Ghi số lượng thông báo liên quan tới A và thời điểm hiện tại; vào `Cash`, nạp **1.000 VND** cho A đúng một lần.
3. Quay lại `Overview`, tìm thông báo nạp tiền mới của A. Phân biệt thông báo Observer với các log kỹ thuật có tiền tố khác.
4. Lặp lại điều hướng/refresh, nạp thêm **1.000 VND** đúng một lần và kiểm tra chỉ thêm **một** thông báo UI cho sự kiện nạp mới của A.

**Kết quả cần đạt**

- Sau mỗi lần nạp, số dư A tăng đúng 1.000 VND và bảng Observer cập nhật không cần khởi động lại.
- Mỗi giao dịch nạp tạo đúng **một** dòng thông báo UI của A cho giao dịch đó, kể cả sau nhiều lần vào lại màn; không có 2–3 dòng trùng vì gắn observer lặp.
- SMS/Email notifier cũng được gọi trong mã, nhưng chúng ghi ra console; trong UI, tiêu chí là bảng thông báo tài khoản.

**Số dòng mới sau lần nạp 1 / lần nạp 2:** `____ / ____`  **[ ] Đạt  [ ] Không đạt  [ ] Chưa kiểm**

### 3.8 Facade — một lần chuyển tiền, nhiều kết quả nghiệp vụ

**Đường đi mã:** `Transfer & Pay / NovaBankTransferController.handleReviewTransfer()` → `BankingFacade.transfer()` → Strategy tính phí, Command thực thi, `TransactionService` ghi sổ và Observer phát thông báo.

**Thao tác**

1. Đảm bảo A đã mở khóa và đang là `STANDARD`. Ghi số dư A = **S**, B = **T**, số lệnh Undo = **U**.
2. Vào `Transfer & Pay`, chọn A → B, nhập **10.000 VND**, nội dung `Kiểm thử Facade Command`; ghi phí ở tóm tắt form.
3. Bấm **Xác nhận chuyển khoản →**, đọc phí trong hộp xác nhận rồi xác nhận giao dịch **một lần**.
4. Vào `Transactions`, tìm giao dịch theo nội dung/mã; đọc số tiền, phí và hai tài khoản. Vào `Accounts` kiểm tra số dư.

**Kết quả cần đạt**

- Với A `STANDARD`, phí phải là **10 VND** tại form và hộp xác nhận; bản ghi giao dịch cũng ghi phí 10 VND.
- Sau giao dịch: A = **S − 10.010 VND**, B = **T + 10.000 VND**; chỉ có một bản ghi chuyển khoản mới.
- Bộ đếm Undo tăng từ **U lên U + 1**; Overview có sự kiện Observer cho A và B.
- Bất kỳ lỗi lưu giao dịch nào phải không để lại thay đổi số dư nửa chừng hoặc một lệnh Undo giả.

**S/T/U trước:** `____ / ____ / ____`  **Sau:** `____ / ____ / ____`  **Mã giao dịch:** `____`  **[ ] Đạt  [ ] Không đạt  [ ] Chưa kiểm**

### 3.9 Command — hoàn tác giao dịch vừa tạo

**Đường đi mã:** `Transactions / NovaBankHistoryController.handleUndo()` → `BankingFacade.undoLastTransfer()` → `TransactionHistory.undoLast()` → `TransferCommand.undo()` → ghi bản ghi hoàn tác.

**Thao tác**

1. Ngay sau bước Facade, ghi số dư A/B và bộ đếm `↶ Hoàn tác (… lệnh)`.
2. Bấm **Hoàn tác** một lần. Không nạp/rút/chuyển thêm giữa bước Facade và bước này để phép đối chiếu số dư rõ ràng.
3. Tìm bản ghi hoàn tác trong `Transactions`, ghi mã giao dịch gốc mà nó liên kết; kiểm tra số dư A/B và bộ đếm.
4. Đóng/mở lại ứng dụng, xác nhận giao dịch gốc và bản ghi hoàn tác còn trong lịch sử, không bị khôi phục thành lệnh Undo đang chờ.

**Kết quả cần đạt**

- A tăng lại **10.010 VND** và B giảm **10.000 VND**, tức trở về số dư ngay trước giao dịch ở bước Facade.
- Có bản ghi hoàn tác riêng tham chiếu mã giao dịch gốc; không xóa lịch sử gốc.
- Bộ đếm lệnh Undo giảm 1 và không thể hoàn tác lại chính giao dịch đó sau khi khởi động lại.
- Nếu B không đủ tiền để đảo giao dịch, UI phải báo thất bại và giữ nguyên số dư, lịch sử và lệnh Undo. Đây là ca kiểm thử phụ, chỉ chạy trên dữ liệu thử nghiệm riêng.

**Số dư A/B và Undo trước/sau:** `________________`  **[ ] Đạt  [ ] Không đạt  [ ] Chưa kiểm**

## 4. Kiểm tra chéo sau phiên chạy

- [ ] Cả 9 phần trên có ảnh hoặc ghi chú kết quả thực tế; không đánh dấu “đạt” chỉ dựa vào mã nguồn.
- [ ] Tài khoản A/B, số tiền, phí và mã giao dịch nhất quán giữa `Transfer & Pay`, `Accounts`, `Transactions` và `Overview`.
- [ ] Đổi gói, khóa/mở khóa, chuyển tiền và Undo còn nhất quán sau khi khởi động lại; riêng mẫu Prototype không có yêu cầu lưu lâu dài hiện tại.
- [ ] Không có thông báo Observer bị nhân đôi sau điều hướng nhiều lần.
- [ ] UI không hiển thị thao tác quản trị như mở tài khoản/đổi gói/Proxy Demo cho người dùng không phải ADMIN.
- [ ] Tất cả lỗi được ghi theo mẫu bên dưới và gắn ảnh; không sửa dữ liệu thật để tạo tình huống lỗi.

### Mẫu ghi lỗi

```text
Pattern / bước:
Ngày giờ:
Tài khoản / gói / trạng thái:
Số dư trước:
Thao tác và số tiền:
Kết quả cần đạt:
Kết quả thực tế:
Thông báo lỗi / mã giao dịch:
Ảnh hoặc log:
Mức độ: chặn demo / sai dữ liệu / sai hiển thị
```

## 5. Kết luận phiên kiểm thử

- Số pattern đạt luồng JavaFX tự động: `9 / 9`
- Số pattern hoàn tất toàn bộ ca thủ công ở mục 3: `____ / 9`
- Ca thủ công không đạt hoặc chưa kiểm: `________________`
- Lỗi cần sửa trước khi demo/bảo vệ: `________________`
- Người xác nhận và thời điểm: `________________`

**Kết luận hiện tại:** 9 pattern đều đạt phép kiểm tra tương tác JavaFX cốt lõi trong cùng một phiên test, toàn suite đạt 20/20; các ca phụ và đánh giá trực quan thủ công vẫn là “Chưa kiểm” cho đến khi thực hiện các bước còn trống trong tài liệu này.
