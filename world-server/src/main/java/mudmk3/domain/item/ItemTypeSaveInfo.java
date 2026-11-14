package mudmk3.domain.item;

import org.jspecify.annotations.Nullable;

/**
 * 아이템 종류를 만들거나 고칠 때 받는 값. 검사는 도메인({@link ItemType})이 한다.
 * 묘사가 null이면 빈 문자열로, 저물대 등급은 종류가 저물대일 때만 정한다
 */
public record ItemTypeSaveInfo(
        String name,
        @Nullable String description,
        ItemCategory category,
        long baseValue,
        boolean tradable,
        @Nullable BagGrade bagGrade
) {
}
