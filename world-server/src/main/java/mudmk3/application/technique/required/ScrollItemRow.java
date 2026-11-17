package mudmk3.application.technique.required;

import mudmk3.application.technique.provided.ScrollItem;

/** 공법이 담긴 아이템 종류 한 줄(어느 공법인지 함께). 서비스가 공법별 {@link ScrollItem} 으로 나눈다 */
public record ScrollItemRow(Long techniqueId, Long id, String name) {
}
