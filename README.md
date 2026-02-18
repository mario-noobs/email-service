# Email Service Library

A reusable Spring Boot email library with FreeMarker HTML templating. Drop it into any Spring Boot project to get template-based email sending with zero boilerplate.

## Features

- **Spring Boot Auto-Configuration** - works out of the box with `JavaMailSender`
- **FreeMarker Templates** - HTML emails with shared base layout and child templates
- **Builder API** - fluent `EmailRequest.builder()` for composing emails
- **Configurable** - `application.yml` properties for sender info, template path, enable/disable
- **Pre-built Templates** - welcome, password reset, and error alert templates included

## Project Structure

```
email-service/
├── build.gradle
├── settings.gradle
└── src/main/
    ├── java/com/mario/email/
    │   ├── EmailAutoConfiguration.java   # Auto-config (registers beans when JavaMailSender present)
    │   ├── EmailProperties.java          # @ConfigurationProperties("app.email")
    │   ├── EmailRequest.java             # Builder DTO for email parameters
    │   ├── EmailService.java             # Core service: render template → send MimeMessage
    │   ├── EmailTemplateEngine.java      # FreeMarker wrapper: template name → rendered HTML
    │   └── EmailException.java           # Runtime exception for send/render failures
    └── resources/
        ├── META-INF/spring/
        │   └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
        └── email-templates/
            ├── layout/base.ftl           # Shared HTML layout (header, body, footer, styles)
            ├── welcome.ftl               # Account creation email
            ├── password-reset.ftl        # Password reset link email
            └── error-alert.ftl           # System alert email (5xx errors, brute-force, etc.)
```

## Quick Start

### 1. Build & Publish to Maven Local

```bash
cd email-service
./gradlew build publishToMavenLocal
```

This publishes `com.mario:email-service:1.0.0` to `~/.m2/repository/`.

### 2. Add Dependency

**Gradle (from mavenLocal):**

```groovy
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation 'com.mario:email-service:1.0.0'
}
```

**Gradle (local JAR file — useful for Docker builds):**

```groovy
dependencies {
    implementation files('libs/email-service-1.0.0.jar')
    implementation 'org.freemarker:freemarker:2.3.32'  // transitive deps needed with files()
}
```

> **Note:** When using `files()`, transitive dependencies (FreeMarker, spring-boot-starter-mail) are not resolved automatically. Add them explicitly.

### 3. Configure SMTP

Add to your `application.yml`:

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true

app:
  email:
    from-address: noreply@your-domain.com
    from-name: Your App Name
    enabled: true
```

For local development with [MailHog](https://github.com/mailhog/MailHog):

```yaml
spring:
  mail:
    host: localhost
    port: 1025

app:
  email:
    from-address: noreply@localhost
    from-name: Dev App
```

### 4. Inject & Send

```java
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final EmailService emailService;

    public void sendWelcomeEmail(String to, String firstName, String role) {
        EmailRequest request = EmailRequest.builder()
                .to(to)
                .subject("Welcome to the Platform!")
                .templateName("welcome")
                .model("firstName", firstName)
                .model("email", to)
                .model("role", role)
                .model("loginUrl", "https://your-app.com/login")
                .build();

        emailService.send(request);
    }
}
```

That's it — no `@Bean` definitions needed. `EmailAutoConfiguration` registers `EmailService` and `EmailTemplateEngine` automatically when `JavaMailSender` is on the classpath.

## Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `app.email.from-address` | `noreply@example.com` | Default sender email address |
| `app.email.from-name` | `System` | Display name for the sender |
| `app.email.template-path` | `email-templates` | Classpath path to FreeMarker templates |
| `app.email.enabled` | `true` | Set `false` to disable sending (useful for tests) |

## API Reference

### EmailRequest

Build with the fluent builder:

```java
EmailRequest request = EmailRequest.builder()
        .to("user@example.com")                   // Required: recipient
        .subject("Subject Line")                   // Required: email subject
        .templateName("welcome")                   // Required: template name (without .ftl)
        .model("key", "value")                     // Template variable (repeatable)
        .cc("cc@example.com")                      // Optional: CC recipient (repeatable)
        .bcc("bcc@example.com")                    // Optional: BCC recipient (repeatable)
        .header("X-Custom-Header", "value")        // Optional: custom header (repeatable)
        .fromAddress("custom-sender@example.com")  // Optional: override default from-address
        .build();
```

### EmailService

```java
// Send an email — renders the template and sends via JavaMailSender
emailService.send(request);

// Throws EmailException on failure (render or send)
// No-ops silently when app.email.enabled=false
```

### EmailTemplateEngine

```java
// Render a template to HTML string (for preview, logging, etc.)
String html = templateEngine.render("welcome", Map.of(
    "firstName", "John",
    "email", "john@example.com",
    "role", "BASIC_USER",
    "loginUrl", "https://app.com/login"
));
```

## Included Templates

### `welcome` — Account Created

Notifies a new user that their account is ready.

**Variables:**

| Variable | Type | Description |
|----------|------|-------------|
| `firstName` | String | User's first name |
| `email` | String | User's email address |
| `role` | String | Assigned role name |
| `loginUrl` | String | URL to the login page |

**Example:**

```java
EmailRequest.builder()
    .to("john@example.com")
    .subject("Welcome!")
    .templateName("welcome")
    .model("firstName", "John")
    .model("email", "john@example.com")
    .model("role", "BASIC_USER")
    .model("loginUrl", "https://app.com/login")
    .build();
```

---

### `password-reset` — Reset Password Link

Sends a time-limited password reset link.

**Variables:**

| Variable | Type | Description |
|----------|------|-------------|
| `firstName` | String | User's first name |
| `resetUrl` | String | Password reset URL (with token) |
| `expiryMinutes` | int | Link expiry time in minutes |

**Example:**

```java
EmailRequest.builder()
    .to("john@example.com")
    .subject("Reset Your Password")
    .templateName("password-reset")
    .model("firstName", "John")
    .model("resetUrl", "https://app.com/reset?token=abc123")
    .model("expiryMinutes", 30)
    .build();
```

---

### `error-alert` — System Alert

Sends alert emails for server errors, brute-force detections, or any operational event. Supports severity-based styling and conditional detail fields.

**Variables:**

| Variable | Type | Required | Description |
|----------|------|----------|-------------|
| `alertType` | String | Yes | Alert category (e.g. `"Server Error"`, `"Brute Force"`) |
| `alertTitle` | String | Yes | Alert heading |
| `alertMessage` | String | Yes | Alert description |
| `timestamp` | String | Yes | When the event occurred |
| `severity` | String | Yes | `"critical"`, `"warning"`, or `"info"` — controls alert box color |
| `requestId` | String | No | Request trace ID |
| `action` | String | No | Semantic action (e.g. `"auth:login"`) |
| `actorIp` | String | No | Client IP address |
| `statusCode` | int | No | HTTP status code |
| `httpPath` | String | No | Request path |
| `failedAttempts` | int | No | Number of failed attempts (brute-force) |
| `windowMinutes` | int | No | Time window for failed attempts |

**Example — Server Error (5xx):**

```java
EmailRequest.builder()
    .to("admin@example.com")
    .subject("[CRITICAL] Server Error Detected")
    .templateName("error-alert")
    .model("alertType", "Server Error")
    .model("alertTitle", "HTTP 500 Internal Server Error")
    .model("alertMessage", "A server error occurred during request processing.")
    .model("timestamp", "2026-02-18T14:30:00")
    .model("severity", "critical")
    .model("requestId", "abc-123-def")
    .model("statusCode", 500)
    .model("httpPath", "/api/v1/face/register-identity")
    .model("actorIp", "192.168.1.100")
    .build();
```

**Example — Brute-Force Detection:**

```java
EmailRequest.builder()
    .to("admin@example.com")
    .subject("[WARNING] Brute Force Login Detected")
    .templateName("error-alert")
    .model("alertType", "Brute Force")
    .model("alertTitle", "Multiple Failed Login Attempts Detected")
    .model("alertMessage", "5 failed login attempts from the same IP address.")
    .model("timestamp", "2026-02-18T14:30:00")
    .model("severity", "warning")
    .model("actorIp", "10.0.0.50")
    .model("action", "auth:login")
    .model("failedAttempts", 5)
    .model("windowMinutes", 10)
    .build();
```

## Creating Custom Templates

### 1. Create a `.ftl` file

Place your template in the template path (default: `src/main/resources/email-templates/`).

Use the shared base layout for consistent styling:

```ftl
<#import "layout/base.ftl" as layout>
<@layout.layout title="Your Email Title">
  <h2>Hello, ${name}!</h2>
  <p>Your custom content here.</p>

  <table class="detail-table">
    <tr><td>Field</td><td>${someValue}</td></tr>
  </table>

  <a href="${actionUrl}" class="btn">Click Here</a>
</@layout.layout>
```

### 2. Available CSS classes from base layout

| Class | Usage |
|-------|-------|
| `.btn` | Blue call-to-action button |
| `.detail-table` | Two-column key-value table |
| `.alert-box` | Container for alert banners |
| `.alert-error` | Red alert (critical) |
| `.alert-warning` | Yellow alert (warning) |
| `.alert-info` | Blue alert (info) |

### 3. Send with your template

```java
emailService.send(EmailRequest.builder()
    .to("user@example.com")
    .subject("Your Subject")
    .templateName("your-template")    // filename without .ftl
    .model("name", "Alice")
    .model("someValue", "12345")
    .model("actionUrl", "https://app.com/action")
    .build());
```

## Overriding Beans

The auto-configuration uses `@ConditionalOnMissingBean`, so you can override any bean:

```java
@Configuration
public class CustomEmailConfig {

    @Bean
    public EmailTemplateEngine emailTemplateEngine(EmailProperties props) {
        // Custom FreeMarker configuration
        return new EmailTemplateEngine("custom-templates");
    }

    @Bean
    public EmailService emailService(JavaMailSender sender,
                                      EmailTemplateEngine engine,
                                      EmailProperties props) {
        // Custom email service with extra logging, metrics, etc.
        return new EmailService(sender, engine, props);
    }
}
```

## Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 17 | Runtime |
| Spring Boot | 3.3.4 | BOM / dependency management |
| Spring Mail | 3.x | SMTP email sending (`JavaMailSender`) |
| FreeMarker | 2.3.32 | HTML template rendering |
| Lombok | 1.18.x | Builder / getter boilerplate reduction |

## License

Internal library — part of the Face Recognition System.
