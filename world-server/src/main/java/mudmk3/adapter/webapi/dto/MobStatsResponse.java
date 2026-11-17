package mudmk3.adapter.webapi.dto;

import mudmk3.domain.mob.MobStats;

/** 계산한 몹 스탯(목록·상세 공통) */
public record MobStatsResponse(int qiBlood, int body, int power) {

    static MobStatsResponse of(MobStats stats) {
        return new MobStatsResponse(stats.qiBlood(), stats.body(), stats.power());
    }
}
