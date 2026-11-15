package mudmk3.adapter.persistence;

import jakarta.persistence.EntityManagerFactory;
import mudmk3.TestFakesConfiguration;
import mudmk3.domain.item.SpiritStoneGrade;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * 옛 DB(귀속·거래 가능 열, 옛 enum CHECK 가 남은 표)를 H2에 흉내 내고 지우기를 돌린다. DDL을 쓰므로 트랜잭션 롤백 없이 돌리고 뒤에 치운다.
 * 서버가 뜰 때의 순서를 보려고 스키마가 생긴 직후 옛 CHECK 를 심으므로 다른 테스트와 DB를 나누지 않는다
 */
@SpringBootTest
@Import({TestFakesConfiguration.class, LegacySchemaCleanerTest.StaleCheckPlanter.class})
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:legacy-schema;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE")
record LegacySchemaCleanerTest(LegacySchemaCleaner legacySchemaCleaner, JdbcTemplate jdbcTemplate) {

    /** 영석 심기를 막는 옛 종류 CHECK(영석 없음). 정리가 영석 심기보다 늦게 돌면 서버가 뜨지 못한다 */
    private static final String STALE_CATEGORY_CHECK = "legacy_stale_category_check";

    private static final long OLD_CHARACTER = 9901L;
    private static final long OTHER_CHARACTER = 9902L;
    private static final String BRACELET_NAME = "옛 DB 저물탁";

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM inventory_entry WHERE inventory_id IN "
                + "(SELECT id FROM inventory WHERE character_id IN (?, ?))", OLD_CHARACTER, OTHER_CHARACTER);
        jdbcTemplate.update("DELETE FROM inventory WHERE character_id IN (?, ?)", OLD_CHARACTER, OTHER_CHARACTER);
        jdbcTemplate.execute("ALTER TABLE inventory_entry DROP CONSTRAINT IF EXISTS uk_inventory_entry_inventory_id_item_type_id_bound");
        jdbcTemplate.execute("ALTER TABLE inventory_entry DROP COLUMN IF EXISTS bound");
        jdbcTemplate.execute("ALTER TABLE item_type DROP COLUMN IF EXISTS tradable");
        jdbcTemplate.execute("ALTER TABLE item_type DROP CONSTRAINT IF EXISTS item_type_bag_grade_check");
        jdbcTemplate.update("DELETE FROM item_type WHERE name = ?", BRACELET_NAME);
        jdbcTemplate.execute("DROP TABLE IF EXISTS legacy_check_probe");
    }

    @Test
    void detectsEnumCheckClauses() {
        assertThat(LegacySchemaCleaner.isEnumCheck("((category)::text = ANY ((ARRAY['MATERIAL'::character varying, "
                + "'PILL'::character varying])::text[]))")).isTrue();
        assertThat(LegacySchemaCleaner.isEnumCheck("((bag_grade)::text = ANY ((ARRAY['LOW'::character varying, "
                + "'MIDDLE'::character varying, 'HIGH'::character varying])::text[]))")).isTrue();
        assertThat(LegacySchemaCleaner.isEnumCheck("\"category\" IN('MATERIAL', 'PILL')")).isTrue();
        assertThat(LegacySchemaCleaner.isEnumCheck("grade IN ('A', 'B')")).isTrue();

        assertThat(LegacySchemaCleaner.isEnumCheck("count IS NOT NULL")).isFalse();
        assertThat(LegacySchemaCleaner.isEnumCheck("\"count\" >= 1")).isFalse();
        assertThat(LegacySchemaCleaner.isEnumCheck("((base_value >= 0))")).isFalse();
        assertThat(LegacySchemaCleaner.isEnumCheck("((count >= 1) AND (count <= 99))")).isFalse();
        assertThat(LegacySchemaCleaner.isEnumCheck("(tier BETWEEN 0 AND 3)")).isFalse();
        assertThat(LegacySchemaCleaner.isEnumCheck("(kind IN (0, 1, 2))")).isFalse();
        assertThat(LegacySchemaCleaner.isEnumCheck("(grade IN ('A', 'B') AND count >= 1)")).isFalse();
        assertThat(LegacySchemaCleaner.isEnumCheck(null)).isFalse();
    }

    @Test
    void dropsEnumChecksOnly() {
        jdbcTemplate.execute("CREATE TABLE legacy_check_probe (id bigint PRIMARY KEY, grade varchar(16) CHECK (grade IN ('A', 'B')), "
                + "count int NOT NULL CHECK (count >= 1), code varchar(8) UNIQUE, item_type_id bigint REFERENCES item_type (id))");
        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO legacy_check_probe (id, grade, count) VALUES (1, 'C', 1)"))
                .isInstanceOf(DataIntegrityViolationException.class);

        legacySchemaCleaner.clean();

        jdbcTemplate.update("INSERT INTO legacy_check_probe (id, grade, count) VALUES (1, 'C', 1)");
        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO legacy_check_probe (id, grade, count) VALUES (2, 'A', 0)"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbcTemplate.queryForList("SELECT constraint_type FROM information_schema.table_constraints "
                + "WHERE LOWER(table_name) = 'legacy_check_probe'", String.class))
                .containsExactlyInAnyOrder("PRIMARY KEY", "CHECK", "UNIQUE", "FOREIGN KEY");
    }

    /**
     * H2 에서는 Hibernate 가 enum 열에 CHECK 를 만들지 않는다(PostgreSQL 은 만든다 — 실제 pgdata 로 따로 본다).
     * Hibernate 가 H2 에 만드는 숫자 CHECK({@code position >= 0})는 남는다
     */
    @Test
    void keepsHibernateNumericChecks() {
        long before = checkClauses().stream().filter(clause -> clause.contains(">= 0")).count();

        legacySchemaCleaner.clean();

        assertThat(before).isPositive();
        assertThat(checkClauses()).filteredOn(clause -> clause.contains(">= 0")).hasSize((int) before);
        assertThat(checkClauses()).noneMatch(LegacySchemaCleaner::isEnumCheck);
    }

    @Test
    void cleanIsIdempotent() {
        legacySchemaCleaner.clean();
        legacySchemaCleaner.clean();
    }

    /** 정리는 SmartInitializingSingleton 이라 ApplicationReadyEvent 의 영석 심기보다 먼저 돈다. 늦으면 심어 둔 옛 CHECK 가 영석 저장을 막는다 */
    @Test
    void runsBeforeApplicationReady() {
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM information_schema.table_constraints "
                + "WHERE LOWER(constraint_name) = ?", Long.class, STALE_CATEGORY_CHECK)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM item_type WHERE category = 'SPIRIT_STONE'", Long.class))
                .isEqualTo(SpiritStoneGrade.values().length);
    }

    @Test
    void mergesDuplicateEntriesAndDropsOldColumns() {
        jdbcTemplate.execute("ALTER TABLE item_type ADD COLUMN tradable BOOLEAN DEFAULT TRUE NOT NULL");
        jdbcTemplate.execute("ALTER TABLE inventory_entry ADD COLUMN bound BOOLEAN DEFAULT FALSE NOT NULL");
        jdbcTemplate.execute("ALTER TABLE inventory_entry ADD CONSTRAINT uk_inventory_entry_inventory_id_item_type_id_bound "
                + "UNIQUE (inventory_id, item_type_id, bound)");
        long low = stoneId("LOW");
        long middle = stoneId("MIDDLE");
        long old = insertInventory(OLD_CHARACTER);
        long other = insertInventory(OTHER_CHARACTER);
        long firstLow = insertEntry(old, low, false, 3);
        long firstMiddle = insertEntry(old, middle, false, 1);
        insertEntry(old, low, true, 4);
        long otherLow = insertEntry(other, low, true, 2);

        legacySchemaCleaner.clean();

        assertThat(entries(old)).extracting(row -> row.get("id"), row -> row.get("item_type_id"), row -> row.get("count"))
                .containsExactly(tuple(firstLow, low, 7), tuple(firstMiddle, middle, 1));
        assertThat(entries(other)).extracting(row -> row.get("id"), row -> row.get("count"))
                .containsExactly(tuple(otherLow, 2));
        assertThat(columnExists("inventory_entry", "bound")).isFalse();
        assertThat(columnExists("item_type", "tradable")).isFalse();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM information_schema.table_constraints "
                + "WHERE LOWER(constraint_name) = 'uk_inventory_entry_inventory_id_item_type_id_bound'", Long.class)).isZero();

        legacySchemaCleaner.clean();

        assertThat(entries(old)).extracting(row -> row.get("id"), row -> row.get("count"))
                .containsExactly(tuple(firstLow, 7), tuple(firstMiddle, 1));
    }

    @Test
    void cleanWithoutOldColumnsChangesNothing() {
        long low = stoneId("LOW");
        long inventory = insertInventory(OLD_CHARACTER);
        long entry = requireNonNull(jdbcTemplate.queryForObject(
                "SELECT id FROM FINAL TABLE (INSERT INTO inventory_entry (inventory_id, item_type_id, count) VALUES (?, ?, ?))",
                Long.class, inventory, low, 5));

        legacySchemaCleaner.clean();

        assertThat(entries(inventory)).extracting(row -> row.get("id"), row -> row.get("count"))
                .containsExactly(tuple(entry, 5));
    }

    /** 3차 PostgreSQL 이 만든 저물대 등급 CHECK(LOW·MIDDLE·HIGH 만)는 저물탁 등급 저장을 막으므로 지운다 */
    @Test
    void dropsStaleBagGradeCheck() {
        jdbcTemplate.execute("ALTER TABLE item_type ADD CONSTRAINT item_type_bag_grade_check "
                + "CHECK (bag_grade IN ('LOW', 'MIDDLE', 'HIGH'))");
        assertThatThrownBy(() -> insertBracelet())
                .isInstanceOf(DataIntegrityViolationException.class);

        legacySchemaCleaner.clean();

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM information_schema.table_constraints "
                + "WHERE LOWER(constraint_name) = 'item_type_bag_grade_check'", Long.class)).isZero();
        insertBracelet();
        assertThat(jdbcTemplate.queryForObject("SELECT bag_grade FROM item_type WHERE name = ?", String.class, BRACELET_NAME))
                .isEqualTo("BRACELET_LOW");

        legacySchemaCleaner.clean();
    }

    private void insertBracelet() {
        jdbcTemplate.update("INSERT INTO item_type (name, description, category, base_value, bag_grade) VALUES (?, '', 'BAG', 5000, 'BRACELET_LOW')",
                BRACELET_NAME);
    }

    private List<String> checkClauses() {
        return jdbcTemplate.queryForList("SELECT check_clause FROM information_schema.check_constraints", String.class);
    }

    private long stoneId(String grade) {
        return requireNonNull(jdbcTemplate.queryForObject("SELECT id FROM item_type WHERE spirit_stone_grade = ?", Long.class, grade));
    }

    private long insertInventory(long characterId) {
        jdbcTemplate.update("INSERT INTO inventory (character_id) VALUES (?)", characterId);
        return requireNonNull(jdbcTemplate.queryForObject("SELECT id FROM inventory WHERE character_id = ?", Long.class, characterId));
    }

    private long insertEntry(long inventoryId, long itemTypeId, boolean bound, int count) {
        return requireNonNull(jdbcTemplate.queryForObject(
                "SELECT id FROM FINAL TABLE (INSERT INTO inventory_entry (inventory_id, item_type_id, bound, count) VALUES (?, ?, ?, ?))",
                Long.class, inventoryId, itemTypeId, bound, count));
    }

    private List<Map<String, Object>> entries(long inventoryId) {
        return jdbcTemplate.queryForList(
                "SELECT id, item_type_id, count FROM inventory_entry WHERE inventory_id = ? ORDER BY id", inventoryId);
    }

    private boolean columnExists(String table, String column) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE LOWER(table_name) = ? AND LOWER(column_name) = ?", Long.class, table, column);
        return count != null && count > 0;
    }

    /** 스키마가 생긴 직후(엔티티 매니저 팩터리 초기화 뒤), 정리·영석 심기 전에 옛 종류 CHECK 를 심는다 */
    @TestConfiguration
    static class StaleCheckPlanter {

        @Bean
        static BeanPostProcessor staleCheckPlanter(ObjectProvider<DataSource> dataSource) {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String beanName) {
                    if (bean instanceof EntityManagerFactory) {
                        new JdbcTemplate(dataSource.getObject()).execute("ALTER TABLE item_type ADD CONSTRAINT IF NOT EXISTS "
                                + STALE_CATEGORY_CHECK + " CHECK (category IN ('MATERIAL', 'PILL', 'BAG'))");
                    }
                    return bean;
                }
            };
        }
    }
}
