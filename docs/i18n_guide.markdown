# 🌐 Internationalization Implementation Guide

**Default Language:** Vietnamese (vi)  
**Secondary Language:** English (en)  
**Message Files:** `messages.properties` (Vietnamese), `messages_en.properties` (English)

## 🛠️ Resources

### **1. I18nService**

Handles message translation and localization.

```java
@Autowired
private I18nService i18nService;

String message = i18nService.msg("error.user.not.existed");
String price = i18nService.formatPrice(new BigDecimal("100000"), "VND");
String desc = i18nService.getLocalizedField(tutorEntity, "desc");
```

### **2. LocalizationHelper**

Simplifies MapStruct field localization.

```java
@Mapper(componentModel = "spring", uses = {LocalizationHelper.class})
public interface YourMapper {
    @Mapping(target = "desc", expression = "java(localizationHelper.getLocalizedField(entity, \"desc\"))")
    YourResponse toResponse(YourEntity entity, @Context LocalizationHelper localizationHelper);
}
```

### **3. GlobalExceptionHandler**

Auto-translates error messages based on locale.

## 🎯 Implementation Patterns

### **Pattern 1: Auto Error Translation**

Exceptions translated by `GlobalExceptionHandler`.

```java
@GetMapping("/endpoint")
public ApiResponse<String> yourEndpoint() {
    throw new AppException(ErrorCode.USER_NOT_EXISTED); // Auto-translated
}
```

### **Pattern 2: Manual Translation**

Use `I18nService` for custom messages.

```java
@RequiredArgsConstructor
@RestController
public class YourController {
    private final I18nService i18nService;

    @GetMapping("/welcome")
    public ApiResponse<String> welcome() {
        return ApiResponse.<String>builder()
                .code(1000)
                .message(i18nService.msg("welcome.message"))
                .result("success")
                .build();
    }
}
```

### **Pattern 3: Entity Field Localization**

Use MapStruct with `LocalizationHelper`.

```java
@Entity
public class Product {
    private String nameEn, nameVi, descEn, descVi;
}

public class ProductResponse {
    private String name, desc;
}

@Mapper(componentModel = "spring", uses = {LocalizationHelper.class})
public interface ProductMapper {
    @Mapping(target = "name", expression = "java(localizationHelper.getLocalizedField(entity, \"name\"))")
    @Mapping(target = "desc", expression = "java(localizationHelper.getLocalizedField(entity, \"desc\"))")
    ProductResponse toResponse(Product entity, @Context LocalizationHelper localizationHelper);
}

@RestController
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final ProductMapper productMapper;
    private final LocalizationHelper localizationHelper;

    @GetMapping("/products")
    public ApiResponse<List<ProductResponse>> getProducts() {
        List<Product> entities = productService.getAllProducts();
        return ApiResponse.<List<ProductResponse>>builder()
                .code(1000)
                .message("success")
                .result(productMapper.toResponseList(entities, localizationHelper))
                .build();
    }
}
```

## 🌍 Locale Configuration

- **Default (No headers):** Vietnamese (`nameVi`, `descVi`, `messages.properties`)
- **English Request:** `Accept-Language: en` (`nameEn`, `descEn`, `messages_en.properties`)
- **Query Param:** `?lang=en`

## 📁 Message Files

**messages.properties (Vietnamese):**

```properties
error.user.not.existed=Không tìm thấy người dùng
welcome.message=Chào mừng đến với EduConnect
```

**messages_en.properties (English):**

```properties
error.user.not.existed=User not found
welcome.message=Welcome to EduConnect
```

## 🚀 Quick Steps

1. **Add Dependencies:**

   ```java
   @RestController
   @RequiredArgsConstructor
   public class YourController {
       private final I18nService i18nService;
       private final YourService yourService;
       private final YourMapper yourMapper;
       private final LocalizationHelper localizationHelper;
   }
   ```

2. **Create Entity:** Add `fieldEn`, `fieldVi`.
3. **Create DTO:** Single `field` for localized output.
4. **Create Mapper:** Use `LocalizationHelper` for field mapping.
5. **Implement Controller:** Return localized responses.

## ✅ Best Practices

- Use `GlobalExceptionHandler` for errors.
- Name fields consistently: `fieldEn`, `fieldVi`.
- Use hierarchical message keys: `error.user.not.existed`.
- Fallback: English → Vietnamese.
- Test with `/test-i18n/*` endpoints.

## 🧪 Testing

```bash
# Vietnamese
curl -X GET "https://api.educonnect.dev/your-endpoint"

# English
curl -X GET "https://api.educonnect.dev/your-endpoint" -H "Accept-Language: en"
```

## 🎯 Summary

- Auto-translated errors
- Localized entity fields
- Vietnamese-first
- Easy to maintain

## Todo

- Format currency
