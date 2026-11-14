package mudmk3.adapter.persistence;

import mudmk3.TestFakesConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/** 옛 DB(귀속·거래 가능 열이 남은 표)를 H2에 흉내 내고 지우기를 돌린다. DDL을 쓰므로 트랜잭션 롤백 없이 돌리고 뒤에 치운다 */
@SpringBootTest
@Import(TestFakesConfiguration.class)
record LegacySchemaCleanerTest(LegacySchemaCleaner legacySchemaCleaner, JdbcTemplate jdbcTemplate) {

    private static final long OLD_CHARACTER = 9901L;
    private static final long OTHER_CHARACTER = 9902L;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM inventory_entry WHERE inventory_id IN "
                + "(SELECT id FROM inventory WHERE character_id IN (?, ?))", OLD_CHARACTER, OTHER_CHARACTER);
        jdbcTemplate.update("DELETE FROM inventory WHERE character_id IN (?, ?)", OLD_CHARACTER, OTHER_CHARACTER);
        jdbcTemplate.execute("ALTER TABLE inventory_entry DROP CONSTRAINT IF EXISTS uk_inventory_entry_inventory_id_item_type_id_bound");
        jdbcTemplate.execute("ALTER TABLE inventory_entry DROP COLUMN IF EXISTS bound");
        jdbcTemplate.execute("ALTER TABLE item_type DROP COLUMN IF EXISTS tradable");
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
}
