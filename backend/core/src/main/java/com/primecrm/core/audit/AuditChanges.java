package com.primecrm.core.audit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class AuditChanges {

    public static final String OLD_KEY = "old";
    public static final String NEW_KEY = "new";
    public static final String REDACTED = "***";

    private static final String UNKNOWN_ENTITY = "Unknown";

    private static final Set<String> IGNORED_FIELDS =
            Set.of("id", "tenantid", "createdat", "updatedat", "createdby", "updatedby");

    private static final List<String> SENSITIVE_FRAGMENTS =
            List.of("password", "token", "secret", "credential", "hash");

    private AuditChanges() {
    }

    public static Map<String, Object> of(Object oldValue, Object newValue) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put(OLD_KEY, normalize(oldValue));
        entry.put(NEW_KEY, normalize(newValue));
        return entry;
    }

    public static Map<String, Object> snapshot(Object entity) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        if (entity == null) {
            return snapshot;
        }
        for (Class<?> type = entity.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!isAuditable(field) || snapshot.containsKey(field.getName())) {
                    continue;
                }
                snapshot.put(field.getName(), readValue(entity, field));
            }
        }
        return snapshot;
    }

    public static Map<String, Object> diff(Map<String, Object> before, Map<String, Object> after) {
        Map<String, Object> diff = new LinkedHashMap<>();
        Map<String, Object> previousState = before == null ? Map.of() : before;
        Map<String, Object> currentState = after == null ? Map.of() : after;
        currentState.forEach((field, currentValue) -> {
            Object previousValue = previousState.get(field);
            if (!Objects.equals(previousValue, currentValue)) {
                diff.put(field, of(previousValue, currentValue));
            }
        });
        return diff;
    }

    public static String entityName(Object entity) {
        if (entity == null) {
            return UNKNOWN_ENTITY;
        }
        String simpleName = entity.getClass().getSimpleName();
        int proxyMarker = simpleName.indexOf('$');
        String cleaned = proxyMarker > 0 ? simpleName.substring(0, proxyMarker) : simpleName;
        return cleaned.isBlank() ? UNKNOWN_ENTITY : cleaned;
    }

    public static UUID entityId(Object entity) {
        return readIdentifier(entity);
    }

    public static boolean isSensitive(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        String normalized = fieldName.toLowerCase(Locale.ROOT);
        return SENSITIVE_FRAGMENTS.stream().anyMatch(normalized::contains);
    }

    public static Object normalize(Object value) {
        if (value == null || value instanceof String || value instanceof Boolean) {
            return value;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.toPlainString();
        }
        if (value instanceof Number) {
            return value;
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        if (value instanceof UUID || value instanceof Temporal || value instanceof Date) {
            return value.toString();
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(AuditChanges::normalize).toList();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            map.forEach((key, entry) -> {
                String name = String.valueOf(key);
                normalized.put(name, isSensitive(name) ? REDACTED : normalize(entry));
            });
            return normalized;
        }
        UUID identifier = readIdentifier(value);
        return identifier != null ? identifier.toString() : String.valueOf(value);
    }

    private static boolean isAuditable(Field field) {
        if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
            return false;
        }
        String normalized = field.getName().toLowerCase(Locale.ROOT);
        return !IGNORED_FIELDS.contains(normalized) && !isSensitive(normalized);
    }

    private static Object readValue(Object entity, Field field) {
        try {
            field.setAccessible(true);
            return normalize(field.get(entity));
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }

    private static UUID readIdentifier(Object value) {
        if (value == null) {
            return null;
        }
        try {
            Method getter = value.getClass().getMethod("getId");
            Object identifier = getter.invoke(value);
            return identifier instanceof UUID uuid ? uuid : null;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }
}
