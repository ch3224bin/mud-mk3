package mudmk3;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

/**
 * 조회가 낸 쿼리 수를 세는 테스트 도우미. Hibernate {@link Statistics} 를 감싼다
 * (테스트 설정 {@code hibernate.generate_statistics: true}). 통계는 앱 전체에 하나이므로 {@link #reset()} 한 뒤 센다
 */
public final class QueryCounter {

    private final Statistics statistics;

    public QueryCounter(EntityManagerFactory entityManagerFactory) {
        this.statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    public void reset() {
        statistics.clear();
    }

    /** {@link #reset()} 뒤에 준비한 JDBC 문 수 */
    public long prepared() {
        return statistics.getPrepareStatementCount();
    }
}
