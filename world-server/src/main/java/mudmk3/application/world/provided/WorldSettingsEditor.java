package mudmk3.application.world.provided;

import mudmk3.domain.world.WorldSettings;

/** 세계 설정(시작 방)을 바꾸는 기능을 제공한다 */
public interface WorldSettingsEditor {

    /** 시작 방을 정한다. 세계 설정 행이 없으면 만든다 */
    WorldSettings designateStartRoom(Long roomId);
}
