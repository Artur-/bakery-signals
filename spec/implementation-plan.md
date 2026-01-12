# Bakery Signals - Implementation Plan

## Overview
Build a bakery order management system using **Vaadin 25 + Spring Boot 4 + PostgreSQL** with **experimental Vaadin Signals** for reactive state management. The application enables storefront staff to take orders and bakery staff to manage production, with real-time multi-user synchronization.

## Scope (Simplified Core)
- **Order States**: New → Ready → Delivered (+ Cancelled)
- **Core Features**: Order CRUD, basic dashboard, user/product/customer management
- **Security**: Spring Security with database users, role-based access (Admin, Employee)
- **Testing**: TestBench UI Unit Tests (browser-less, fast)
- **No bulletin board** (can add later)

## Tech Stack
- Vaadin 25.1-SNAPSHOT (already configured)
- Spring Boot 4.0.0 (already configured)
- Java 21
- PostgreSQL database
- **Vaadin Signals** (IN_MEMORY_SHARED) for reactive state
- Flyway for database migrations
- **TestBench UI Unit Tests** (browser-less, 600 tests in 7 seconds)

---

## Project Structure (Feature-Based Packaging)

```
src/main/java/com/example/
├── Application.java                    [EXISTS]
├── config/
│   ├── SecurityConfig.java             [CREATE] Spring Security + Vaadin integration
│   └── PushConfig.java                 [CREATE] WebSocket push for real-time updates
├── security/
│   ├── domain/
│   │   ├── User.java                   [CREATE] JPA entity with BCrypt password
│   │   ├── Role.java                   [CREATE] Enum: ADMIN, EMPLOYEE
│   │   └── UserRepository.java         [CREATE] Spring Data repository
│   ├── service/
│   │   ├── UserService.java            [CREATE] User management
│   │   └── CustomUserDetailsService.java [CREATE] Spring Security integration
│   └── ui/
│       ├── LoginView.java              [CREATE] Login page
│       └── UserManagementView.java     [CREATE] User CRUD (admin only)
├── orders/
│   ├── domain/
│   │   ├── Order.java                  [CREATE] Main entity with state transitions
│   │   ├── OrderItem.java              [CREATE] Embedded order line items
│   │   ├── OrderState.java             [CREATE] Enum: NEW, READY, DELIVERED, CANCELLED
│   │   ├── PickupLocation.java         [CREATE] Enum: STOREFRONT, PRODUCTION_FACILITY
│   │   └── OrderRepository.java        [CREATE] Spring Data with custom queries
│   ├── service/
│   │   ├── OrderService.java           [CREATE] Business logic + signal updates
│   │   └── OrderSignals.java           [CREATE] ⭐ CRITICAL - Static signals utility class
│   └── ui/
│       ├── DashboardView.java          [CREATE] Reactive dashboard with stats
│       ├── OrderListView.java          [CREATE] Reactive order grid
│       ├── NewOrderView.java           [CREATE] Order creation wizard
│       └── OrderDetailsView.java       [CREATE] View/edit order with state transitions
├── products/
│   ├── domain/
│   │   ├── Product.java                [CREATE] JPA entity
│   │   └── ProductRepository.java      [CREATE] Repository
│   ├── service/
│   │   └── ProductService.java         [CREATE] Business logic
│   └── ui/
│       └── ProductManagementView.java  [CREATE] Product CRUD (admin only)
├── customers/
│   ├── domain/
│   │   ├── Customer.java               [CREATE] JPA entity
│   │   └── CustomerRepository.java     [CREATE] Repository
│   ├── service/
│   │   └── CustomerService.java        [CREATE] Business logic
│   └── ui/
│       └── CustomerManagementView.java [CREATE] Customer CRUD (admin only)
└── shared/
    └── ui/
        └── MainLayout.java             [CREATE] App shell with navigation

src/main/resources/
├── application.properties              [MODIFY] Add PostgreSQL, JPA, Flyway config
├── db/migration/
│   ├── V1__create_users_table.sql      [CREATE] Users with BCrypt password
│   ├── V2__create_products_table.sql   [CREATE] Products catalog
│   ├── V3__create_customers_table.sql  [CREATE] Customer data
│   ├── V4__create_orders_table.sql     [CREATE] Orders + order_items tables
│   └── V5__insert_initial_data.sql     [CREATE] Admin user + sample data
└── META-INF/resources/styles.css       [MODIFY] Custom styles

src/test/java/com/example/
├── orders/
│   ├── service/
│   │   └── OrderServiceTest.java       [CREATE] Service unit tests
│   └── ui/
│       ├── OrderListViewTest.java      [CREATE] UI unit test (browser-less)
│       ├── NewOrderViewTest.java       [CREATE] UI unit test
│       └── DashboardViewTest.java      [CREATE] UI unit test
```

---

## Data Model

### User Entity (Security)
```java
@Entity @Table(name = "users")
- id (Long, PK)
- username (String, unique)
- passwordHash (String, BCrypt)
- fullName (String)
- role (Role enum: ADMIN, EMPLOYEE)
- active (boolean)
- version (Long, optimistic locking)
```

### Product Entity
```java
@Entity @Table(name = "products")
- id (Long, PK)
- name (String)
- description (String)
- price (BigDecimal)
- available (boolean)
- version (Long)
```

### Customer Entity
```java
@Entity @Table(name = "customers")
- id (Long, PK)
- name (String)
- phone (String)
- email (String)
- billingInfo (String)
- notes (String)
- version (Long)
```

### Order Entity (Core Domain)
```java
@Entity @Table(name = "orders")
- id (Long, PK)
- dueDate (LocalDate) ⭐ Critical field - first in form
- state (OrderState enum)
- items (List<OrderItem>, OneToMany cascade)
- customer (Customer, ManyToOne)
- totalPrice (BigDecimal)
- discount (BigDecimal)
- paid (boolean)
- pickupLocation (PickupLocation enum)
- notes (String)
- createdAt (LocalDateTime)
- createdBy (User, ManyToOne)
- stateChangedAt (LocalDateTime)
- version (Long, optimistic locking)

Business methods:
- markReady() - NEW → READY
- markDelivered() - READY → DELIVERED
- cancel() - Any → CANCELLED (except DELIVERED)
```

### OrderItem Entity
```java
@Entity @Table(name = "order_items")
- id (Long, PK)
- product (Product, ManyToOne)
- quantity (Integer)
- pricePerUnit (BigDecimal) - snapshot at order time
- customizationSpecs (String) - e.g., "Happy Birthday John"
```

---

## Vaadin Signals Architecture (CRITICAL)

### OrderSignals.java - Reactive State Hub
**Purpose**: Central reactive state management using `SignalFactory.IN_MEMORY_SHARED`

**Signals (static fields)**:
```java
public class OrderSignals {
    // Shared signals across all users (static fields)
    private static final ListSignal<Order> orders =
        SignalFactory.IN_MEMORY_SHARED.list("orders", Order.class);
    private static final NumberSignal todayOrderCount =
        SignalFactory.IN_MEMORY_SHARED.number("todayOrderCount");
    private static final NumberSignal newOrderCount =
        SignalFactory.IN_MEMORY_SHARED.number("newOrderCount");
    private static final NumberSignal readyOrderCount =
        SignalFactory.IN_MEMORY_SHARED.number("readyOrderCount");
}
```

**Key Methods** (static):
- `refreshAll(OrderRepository)` - Reload all orders from DB, update signals
- `refreshOrder(OrderRepository, Long id)` - Update single order in signal
- `addOrder(Order)` - Add new order to signal
- `updateDashboardStats()` - Recalculate stats from order list
- Getter methods returning read-only signals: `getOrdersSignal()`, `getTodayOrderCountSignal()`, etc.

**Integration**: OrderService calls static methods after DB operations to trigger UI updates

**Important**: No @Component annotation - this is a utility class with static signals and methods

### PushConfig.java
```java
@Push(value = PushMode.AUTOMATIC, transport = Transport.WEBSOCKET_XHR)
public class PushConfig implements AppShellConfigurator {}
```
Enables real-time push updates across all users.

### Reactive UI Pattern (DashboardView example)
```java
// Setup in view constructor:
ComponentEffect.effect(this, () -> {
    // Call static method to get read-only signal
    todayCount.setText(OrderSignals.getTodayOrderCountSignal().value().toString());
});

// This automatically re-runs when the signal changes!

// In OrderService (after creating order):
orderService.save(order);
OrderSignals.addOrder(order); // Static method call to update signals
```

**Important**:
- Signals are **static fields** in OrderSignals utility class
- Use `ComponentEffect.effect()` to bind signals to UI
- Effect is active while component is attached
- Signals update via Push when any user modifies data
- OrderService calls `OrderSignals.staticMethod()` after DB operations
- Must use `UI.access()` for thread-safe UI updates from signals

---

## Security Configuration

### Spring Security Setup
- **SecurityConfig.java**: VaadinSecurityConfigurer + BCryptPasswordEncoder
- **CustomUserDetailsService.java**: Load users from database
- **LoginView.java**: Vaadin LoginForm with Spring Security integration

### Authorization
- **Admin Role**: Full access to all features (users, products, customers, orders)
- **Employee Role**: Can manage orders, cannot manage products or change user roles
- Use `@RolesAllowed({"ROLE_ADMIN", "ROLE_EMPLOYEE"})` on views and service methods

### Initial Admin User
Migration V5 creates:
- Username: `admin`
- Password: `admin` (BCrypt hashed)
- Role: ADMIN
- ⚠️ Change password in production!

---

## REVISED Implementation Plan (Vertical Slices)

Each phase now delivers a complete, shippable feature with full stack implementation.

### ✅ Phase 1: Authentication & Foundation (COMPLETED)
**Shippable Product**: Secure application shell with working authentication

**What's Working**:
- ✅ User login/logout (admin/admin)
- ✅ PostgreSQL database with Flyway migrations
- ✅ Spring Security + Vaadin 25 integration
- ✅ MainLayout with navigation structure
- ✅ Database tables ready (users, products, customers, orders)
- ✅ Seeded with admin user + sample data

**Business Value**: Secure foundation ready for feature development

---

### Phase 2: Product Catalog Management (Next)
**Shippable Product**: Fully tested product catalog management

**Full Stack Implementation**:
1. **Backend (Already exists from Phase 1 migrations)**:
   - ✅ Product entity, repository (table ready)

2. **Service Layer** (NEW):
   - ProductService with CRUD operations
   - Validation for product data

3. **UI Layer** (NEW):
   - ProductManagementView (admin only)
   - Grid showing all products with search/filter
   - Form for create/edit with validation
   - Delete with confirmation dialog
   - Add to MainLayout navigation

4. **Tests** (NEW):
   - ProductServiceTest (unit tests)
   - ProductManagementViewTest (UI unit test - browser-less)

**Acceptance Criteria**:
- Admin can view all products in a grid
- Admin can create new products (name, description, price, availability)
- Admin can edit existing products
- Admin can delete products (with confirmation)
- Form validation works (required fields, positive prices)
- Changes persist to database
- ✅ All tests pass

**Business Value**: Bakery staff can maintain their product catalog with confidence

---

### Phase 3: Customer Management
**Shippable Product**: Fully tested customer database management

**Full Stack Implementation**:
1. **Backend** (Already exists):
   - ✅ Customer entity, repository

2. **Service Layer** (NEW):
   - CustomerService with CRUD operations
   - Validation for customer data

3. **UI Layer** (NEW):
   - CustomerManagementView (admin only)
   - Grid with search by name/phone/email
   - Form for create/edit
   - Delete with confirmation
   - Add to MainLayout navigation

4. **Tests** (NEW):
   - CustomerServiceTest (unit tests)
   - CustomerManagementViewTest (UI unit test)

**Acceptance Criteria**:
- Admin can view/search customers
- Admin can add new customers (name, phone, email, billing info, notes)
- Admin can edit customer information
- Admin can delete customers
- Form validation works
- ✅ All tests pass

**Business Value**: Centralized customer information for order taking

---

### Phase 4: Core Order Management (CRITICAL - Vaadin Signals!)
**Shippable Product**: Fully tested order lifecycle with real-time updates

**Full Stack Implementation**:
1. **Backend** (Partial from Phase 1):
   - ✅ Order entities, repository
   - OrderState enum: NEW, READY, DELIVERED, CANCELLED
   - PickupLocation enum
   - State transition validation in Order entity

2. **Service Layer** (NEW):
   - OrderService with CRUD + state transitions
   - **OrderSignals utility class** (static IN_MEMORY_SHARED signals)
   - Business logic for order calculations

3. **UI Layer** (NEW):
   - **DashboardView** (home page, replaces HomeView):
     - Stats cards: Today's orders, NEW count, READY count
     - Grid of today's orders
     - Reactive updates using ComponentEffect
   - **OrderListView**:
     - Grid with all orders
     - Filters: state, date range, customer search
     - Click row → navigate to details
     - Real-time updates via signals
   - **NewOrderView**:
     - Due date picker (required, first field)
     - Product selection dialog (with quantity/customization)
     - Customer ComboBox (autocomplete, lazy loading)
     - Order items list
     - Total calculation
     - Pickup location, discount, paid checkbox, notes
     - Save button
   - **OrderDetailsView**:
     - Read-only order info
     - Order items grid
     - State transition buttons based on current state
     - Confirmation dialog for cancel
   - Add all to MainLayout navigation

4. **Tests** (NEW):
   - OrderServiceTest (state transitions, validation, optimistic locking)
   - OrderSignalsTest (signal behavior)
   - DashboardViewTest (reactive updates)
   - OrderListViewTest (filtering, reactive grid)
   - NewOrderViewTest (validation, order creation)
   - OrderDetailsViewTest (state transitions)

**Acceptance Criteria**:
- Dashboard shows real-time order stats
- Employees can create new orders with all fields
- Orders appear in list view immediately (reactive)
- Employees can view order details
- Employees can transition orders: NEW→READY→DELIVERED
- Employees can cancel orders
- Multiple users see updates in real-time
- Optimistic locking prevents concurrent update conflicts
- ✅ All tests pass (including signal behavior tests)

**Business Value**: Complete order management system operational!

---

### Phase 5: User Management
**Shippable Product**: Fully tested multi-user system

**Full Stack Implementation**:
1. **Backend** (Already exists):
   - ✅ User entity, service, repository

2. **UI Layer** (NEW):
   - UserManagementView (admin only)
   - Grid showing all users
   - Form for create/edit
   - Role selection (Admin, Employee)
   - Active/inactive toggle
   - Password change dialog
   - Add to MainLayout navigation

3. **Tests** (NEW):
   - UserServiceTest (authentication, authorization, password hashing)
   - UserManagementViewTest (UI unit test)

**Acceptance Criteria**:
- Admin can view all users
- Admin can create new employee accounts
- Admin can change user roles
- Admin can activate/deactivate users
- Admin can reset passwords
- Employees cannot access user management
- ✅ All tests pass

**Business Value**: Multiple employees can use the system with proper access control

---

### Phase 6: Production Readiness & Integration Testing
**Shippable Product**: Production-ready application with end-to-end verification

**Implementation**:
1. **Integration Tests**:
   - Multi-user scenario tests (concurrent order updates)
   - End-to-end workflow tests (create order → transition → complete)
   - Signal synchronization tests across sessions

2. **Production Configuration**:
   - application-prod.properties (external DB, security hardening)
   - Environment-based configuration
   - Docker Compose for deployment
   - Production database migration strategy

3. **Documentation**:
   - README with setup instructions
   - Deployment guide
   - User manual (basic operations)
   - API documentation (if needed)

4. **Performance & Security**:
   - Review and optimize database queries
   - Security audit (OWASP top 10)
   - Load testing (if needed)
   - Backup/restore procedures

**Acceptance Criteria**:
- Integration tests pass
- Code coverage > 70% (aggregated from all phases)
- Can deploy to production environment
- Documentation complete and reviewed
- Performance is acceptable
- Security vulnerabilities addressed
- ✅ Ready to ship!

**Business Value**: Reliable, maintainable, production-ready system

---

## OLD Implementation Steps (REFERENCE ONLY)

### Phase 1: Foundation (Priority 1)
1. **Database Setup**
   - Add dependencies to pom.xml: postgresql, spring-boot-starter-data-jpa, flyway-core, flyway-database-postgresql
   - Modify `application.properties`: PostgreSQL URL, JPA config, Flyway enabled
   - Create migration scripts V1-V5 in `src/main/resources/db/migration/`
   - Verify: Run app, check Flyway creates tables

2. **Security Infrastructure**
   - Create User entity with BCrypt password hashing
   - Create UserRepository, CustomUserDetailsService, UserService
   - Create SecurityConfig with VaadinSecurityConfigurer
   - Create LoginView with Vaadin LoginForm
   - Verify: Can login with admin/admin

3. **Base UI Structure**
   - Create PushConfig for WebSocket push
   - Create MainLayout with AppLayout + SideNav
   - Add navigation items (Dashboard, Orders, New Order)
   - Verify: Can navigate between pages after login

### Phase 2: Domain Model (Priority 2)
4. **Product Management**
   - Create Product entity, ProductRepository, ProductService
   - Create ProductManagementView (Grid + Form for CRUD)
   - Add to MainLayout navigation (admin only)
   - Verify: Admin can create/edit products

5. **Customer Management**
   - Create Customer entity, CustomerRepository, CustomerService
   - Create CustomerManagementView (Grid + Form for CRUD)
   - Add to MainLayout navigation (admin only)
   - Verify: Admin can create/edit customers

6. **Order Domain**
   - Create OrderState, PickupLocation enums
   - Create OrderItem entity
   - Create Order entity with state transition methods
   - Create OrderRepository with custom queries
   - Create OrderService (without signals initially)
   - Verify: Can save orders to database

### Phase 3: Signals Integration (Priority 1 - CRITICAL)
7. **OrderSignals Implementation**
   - Create OrderSignals utility class with static IN_MEMORY_SHARED signals
   - Implement static methods for signal updates
   - Integrate in OrderService: call `OrderSignals.staticMethod()` after DB ops
   - Test in console: verify signals update when orders change
   - Verify: Open two browser tabs, create order in one, check signal in other

8. **DashboardView (Reactive)**
   - Create DashboardView with stats cards
   - Use `ComponentEffect.effect()` to bind signals to UI
   - Add Grid showing today's orders
   - Verify: Stats update in real-time when orders change in another tab

9. **OrderListView (Reactive)**
   - Create OrderListView with Grid
   - Add filters (state, date, search)
   - **Make grid and filters package-protected** (for UI unit testing)
   - Use `ComponentEffect.effect()` to reactively update grid
   - Verify: Grid updates in real-time when orders change

### Phase 4: Order Workflow (Priority 2)
10. **NewOrderView**
    - Create wizard-style form: Due Date → Products → Customer → Details
    - Add product selection dialog with quantity/customization
    - Add customer ComboBox with autocomplete (lazy loading)
    - Add pickup location, notes, discount, paid checkbox
    - Calculate and display total
    - Save order via OrderService
    - Verify: Can create complete order, appears in dashboard immediately

11. **OrderDetailsView**
    - Create view with order details (read-only FormLayout)
    - Add Grid for order items
    - Add state transition buttons based on current state
    - Add confirmation dialog for cancellation
    - Update state via OrderService
    - Verify: State changes update in all open browser tabs

### Phase 5: User Management (Priority 3)
12. **UserManagementView**
    - Create admin-only view (Grid + Form)
    - CRUD operations for users
    - Password field with BCrypt hashing
    - Role selection (Admin, Employee)
    - Active/inactive toggle
    - Verify: Admin can create employees, employees can login

### Phase 6: Testing (Priority 2)
13. **Service Unit Tests**
    - Add spring-boot-starter-test, spring-security-test dependencies
    - Create OrderServiceTest: test state transitions, validation
    - Create UserServiceTest: test authentication, authorization
    - Verify: `mvn test` passes

14. **UI Unit Tests (Browser-less)**
    - Add vaadin-testbench-unit-junit5 dependency
    - Create OrderListViewTest (extends UIUnitTest):
      - Test filtering by state, date
      - Test search functionality
      - Test navigation to order details
      - Make grid and filters package-protected in view
    - Create NewOrderViewTest:
      - Test order creation wizard flow
      - Test product selection
      - Test customer autocomplete
      - Test validation (due date, required fields)
    - Create DashboardViewTest:
      - Test stats display
      - Test reactive updates (mock signal changes)
      - Test navigation to orders
    - Verify: All UI tests pass in ~seconds (no browser needed!)

### Phase 7: Polish (Priority 3)
15. **Styling and UX**
    - Add custom CSS for stat cards, mobile responsiveness
    - Add loading indicators during data fetch
    - Improve error messages (user-friendly)
    - Add confirmation dialogs for destructive actions
    - Verify: Works on mobile, tablet, desktop

16. **Production Config**
    - Create application-prod.properties with external DB config
    - Add Docker Compose for PostgreSQL (development)
    - Document deployment process
    - Verify: Can run in production mode

---

## Critical Files (Start Here)

### 1. application.properties
**File**: `src/main/resources/application.properties`
**Priority**: P0 (foundation)
```properties
# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/bakery
spring.datasource.username=bakery_user
spring.datasource.password=bakery_pass

# JPA
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Flyway
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true

# Vaadin Push
vaadin.push-mode=automatic
```

### 2. OrderSignals.java
**File**: `src/main/java/com/example/orders/service/OrderSignals.java`
**Priority**: P0 (core reactive feature)
- **Utility class** with static signals (no @Component)
- Use `SignalFactory.IN_MEMORY_SHARED` for shared signals (static final fields)
- Static methods: `refreshAll()`, `addOrder()`, `refreshOrder()`, `updateDashboardStats()`
- Static getters returning read-only signals via `asReadonly()`

### 3. Order.java
**File**: `src/main/java/com/example/orders/domain/Order.java`
**Priority**: P0 (domain model)
- JPA entity with `@Version` for optimistic locking
- Business methods: `markReady()`, `markDelivered()`, `cancel()`
- Throw `IllegalStateException` for invalid transitions

### 4. SecurityConfig.java
**File**: `src/main/java/com/example/config/SecurityConfig.java`
**Priority**: P0 (security)
- Use `VaadinSecurityConfigurer`
- BCrypt password encoder
- Enable method security: `@EnableMethodSecurity(jsr250Enabled = true)`

### 5. DashboardView.java
**File**: `src/main/java/com/example/orders/ui/DashboardView.java`
**Priority**: P1 (reference implementation for reactive UI)
- Use `ComponentEffect.effect()` to bind signals
- Demonstrate reactive stats updates
- Reference for other views

---

## Dependencies to Add (pom.xml)

```xml
<!-- PostgreSQL -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Spring Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- Flyway -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>

<!-- TestBench UI Unit Tests (browser-less) -->
<dependency>
    <groupId>com.vaadin</groupId>
    <artifactId>vaadin-testbench-unit-junit5</artifactId>
    <scope>test</scope>
</dependency>

<!-- Spring Security Test -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Bean Validation -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

---

## Potential Challenges & Solutions

### 1. Signal Thread Safety
**Issue**: Multiple users updating orders simultaneously
**Solution**: Use `@Version` for optimistic locking, handle `OptimisticLockException` with user-friendly error

### 2. Push Connection Drops
**Issue**: WebSocket connections may drop
**Solution**: Use WEBSOCKET_XHR transport (fallback), add manual refresh button, use `UI.access()` properly

### 3. Signal Memory Management
**Issue**: IN_MEMORY_SHARED signals persist for JVM lifetime
**Solution**: Implement pagination, periodic cleanup, monitor memory usage

### 4. Concurrent State Transitions
**Issue**: Two employees changing state simultaneously
**Solution**: Optimistic locking + clear error message "Order was updated by another user"

### 5. UI Unit Test Data Isolation
**Issue**: Tests sharing static signals may interfere with each other
**Solution**: Use `@DirtiesContext` or clear signals in `@BeforeEach`, run tests in sequence if needed

---

## Testing Strategy

### Service Unit Tests (JUnit 5)
- OrderServiceTest: state transitions, validation
- UserServiceTest: authentication, authorization
- Use `@SpringBootTest` + `@Transactional`
- Use `@WithMockUser` for security context

### UI Unit Tests (TestBench - Browser-less)
- Extend `UIUnitTest` base class (no browser needed!)
- **Very fast**: 600 tests in 7 seconds (vs 1-2 hours for browser tests)
- Test views directly without servlet container
- Use `navigate(View.class)` to navigate to views
- Use `test(component)` to get component testers
- Use `$(Component.class).first()` for component queries
- Make components package-protected for direct access in tests

**Example UI Unit Tests**:
```java
class OrderListViewTest extends UIUnitTest {

    @Test
    void filterByState_displaysCorrectOrders() {
        // Navigate to view (no browser needed!)
        OrderListView view = navigate(OrderListView.class);

        // Get component tester and interact
        test(view.stateFilter).selectItem(OrderState.NEW);

        // Verify grid shows only NEW orders
        GridTester<Order> gridTester = test(view.grid);
        List<Order> displayedOrders = gridTester.getRenderedItems();
        assertTrue(displayedOrders.stream()
            .allMatch(o -> o.getState() == OrderState.NEW));
    }

    @Test
    void clickOrderRow_navigatesToOrderDetails() {
        OrderListView view = navigate(OrderListView.class);

        // Click first row
        GridTester<Order> gridTester = test(view.grid);
        Order firstOrder = gridTester.getRenderedItems().get(0);
        gridTester.clickRow(firstOrder);

        // Verify navigation to order details
        // (UI unit tests can verify navigation happened)
        assertTrue(getCurrentView() instanceof OrderDetailsView);
    }
}

class NewOrderViewTest extends UIUnitTest {

    @Test
    void createOrder_withValidData_succeeds() {
        NewOrderView view = navigate(NewOrderView.class);

        // Fill in form (components are package-protected)
        test(view.dueDatePicker).setValue(LocalDate.now().plusDays(1));
        // ... add products, select customer, etc.

        // Click create button
        test(view.createButton).click();

        // Verify navigation to order list
        assertTrue(getCurrentView() instanceof OrderListView);

        // Verify notification shown
        Notification notification = $(Notification.class).first();
        assertEquals("Order created successfully!",
            test(notification).getText());
    }

    @Test
    void createOrder_withPastDueDate_showsValidationError() {
        NewOrderView view = navigate(NewOrderView.class);

        // Try to set past date
        test(view.dueDatePicker).setValue(LocalDate.now().minusDays(1));
        test(view.createButton).click();

        // Verify validation error
        assertTrue(view.dueDatePicker.isInvalid());
        assertEquals("Due date cannot be in the past",
            view.dueDatePicker.getErrorMessage());
    }
}
```

**Key Benefits**:
- No browser or servlet container needed
- Tests run in milliseconds
- Direct access to component state
- Easy to test validation and error handling
- Can test reactive signal updates by mocking signal changes

### Integration Tests
- Test security configuration end-to-end
- Test signal synchronization between threads
- Test concurrent state transitions with optimistic locking

---

## Success Criteria

✅ Users can login with database-backed authentication
✅ Employees can create orders (due date → products → customer)
✅ Employees can view orders and change states (New → Ready → Delivered)
✅ Dashboard updates in real-time when orders change (multi-user)
✅ Admins can manage users, products, customers
✅ State transitions are validated (can't skip states)
✅ Concurrent updates are handled gracefully (optimistic locking)
✅ All service unit tests pass
✅ All UI unit tests pass (browser-less, fast)
✅ Mobile responsive design works

---

## Next Steps After Implementation

1. **Enhanced Features** (future iterations):
   - Bulletin board for status messages
   - Email notifications when orders are ready
   - PDF export for order receipts
   - Advanced reporting and analytics
   - Full 8-state workflow from original spec

2. **Production Deployment**:
   - Docker containerization
   - Cloud deployment (AWS/Azure/GCP)
   - External PostgreSQL (RDS/Cloud SQL)
   - HTTPS with SSL certificates
   - Production monitoring and logging

3. **Performance Optimization**:
   - Database indexing optimization
   - Grid lazy loading for large datasets
   - Signal cleanup strategies
   - Caching for product/customer lookups
   - Load testing with Gatling

---

## Estimated Timeline

- **Phase 1 (Foundation)**: 2 days
- **Phase 2 (Domain Model)**: 2 days
- **Phase 3 (Signals)**: 2 days ⭐ Most critical
- **Phase 4 (Order Workflow)**: 2 days
- **Phase 5 (User Management)**: 1 day
- **Phase 6 (Testing)**: 2 days
- **Phase 7 (Polish)**: 1 day

**Total**: ~12 days for full implementation

**MVP (Phases 1-4 only)**: ~8 days for working order system
