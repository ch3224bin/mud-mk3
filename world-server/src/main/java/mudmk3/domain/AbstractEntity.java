package mudmk3.domain;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.proxy.HibernateProxy;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/** 모든 엔티티가 상속하는 id와 HibernateProxy에 안전한 equals/hashCode 기반 */
@MappedSuperclass
public abstract class AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Nullable Long id;

    public @Nullable Long getId() {
        return id;
    }

    @Override
    public final boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractEntity that)) return false;
        if (effectiveClass(this) != effectiveClass(that)) return false;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return effectiveClass(this).hashCode();
    }

    private static Class<?> effectiveClass(Object o) {
        return o instanceof HibernateProxy proxy
                ? proxy.getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();
    }
}
