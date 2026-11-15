package mudmk3.adapter.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mudmk3.support.stereotype.Adapter;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/**
 * 엔티티에서 뺀 옛 열을 서버가 뜰 때 DB에서 지운다(H2·PostgreSQL). {@code ddl-auto: update} 는 열을 지우지 않는데,
 * 남은 NOT NULL·기본값 없는 열은 새 행 저장을 깨뜨린다.
 * <ul>
 *   <li>{@code inventory_entry.bound}(귀속): 같은 (inventory_id, item_type_id) 행을 수를 더해 가장 작은 id 한 행으로 합친 뒤,
 *       그 열이 든 유니크 제약과 열을 지운다</li>
 *   <li>{@code item_type.tradable}(거래 가능): 열을 지운다</li>
 *   <li>enum CHECK: Hibernate 가 enum 열마다 만드는 값 목록 CHECK(PostgreSQL {@code = ANY (ARRAY[…])}, H2 {@code IN (…)}).
 *       {@code ddl-auto: update} 는 CHECK 를 고치지 않아 enum 에 값을 더하면 저장을 막으므로 모두 지운다(3차 저물대 등급
 *       {@code item_type_bag_grade_check} 가 4차 저물탁 등급 {@code BRACELET_*} 을 막은 일 포함). 값 검사는 Java enum 매핑이 한다.
 *       숫자 비교·NOT NULL CHECK 와 유니크·FK 는 남긴다</li>
 * </ul>
 * 옛 열·제약이 없으면 아무것도 하지 않으므로 몇 번 돌려도 같다. 영석 심기({@code ApplicationReadyEvent})보다 먼저 돈다
 */
@Slf4j
@Adapter
@RequiredArgsConstructor
public class LegacySchemaCleaner implements SmartInitializingSingleton {

    private static final String INVENTORY_ENTRY = "inventory_entry";
    private static final String BOUND = "bound";
    private static final String ITEM_TYPE = "item_type";
    private static final String TRADABLE = "tradable";

    private static final String LITERAL = "'[^']*'(?:::[a-z ]+)?";
    private static final String LITERAL_LIST = LITERAL + "(?:\\s*,\\s*" + LITERAL + ")*";
    private static final String COLUMN = "\\(*\\s*\"?\\w+\"?\\s*\\)*(?:::[a-z ]+)?";
    private static final String POSTGRESQL_ANY_ARRAY =
            "=\\s*ANY\\s*\\(\\s*\\(*\\s*ARRAY\\s*\\[\\s*" + LITERAL_LIST + "\\s*]\\s*\\)*(?:::[a-z ]+\\[])?\\s*\\)";
    private static final String H2_IN = "IN\\s*\\(\\s*" + LITERAL_LIST + "\\s*\\)";
    /** 한 열을 문자열 상수 목록과 비교하는 CHECK 정의(Hibernate 의 enum CHECK) */
    private static final Pattern ENUM_CHECK = Pattern.compile(
            "^\\s*\\(*\\s*" + COLUMN + "\\s*(?:" + POSTGRESQL_ANY_ARRAY + "|" + H2_IN + ")\\s*\\)*\\s*$",
            Pattern.CASE_INSENSITIVE);

    private static final String CHECK_CONSTRAINTS = """
            SELECT tc.table_name, tc.constraint_name, cc.check_clause
              FROM information_schema.table_constraints tc
              JOIN information_schema.check_constraints cc
                ON cc.constraint_schema = tc.constraint_schema AND cc.constraint_name = tc.constraint_name
             WHERE tc.constraint_type = 'CHECK'
               AND LOWER(tc.constraint_schema) = LOWER(?)
            """;

    private static final String MERGE_COUNTS = """
            UPDATE inventory_entry e
               SET count = (SELECT SUM(d.count) FROM inventory_entry d
                             WHERE d.inventory_id = e.inventory_id AND d.item_type_id = e.item_type_id)
             WHERE e.id = (SELECT MIN(m.id) FROM inventory_entry m
                            WHERE m.inventory_id = e.inventory_id AND m.item_type_id = e.item_type_id)
               AND EXISTS (SELECT 1 FROM inventory_entry o
                            WHERE o.inventory_id = e.inventory_id AND o.item_type_id = e.item_type_id AND o.id <> e.id)
            """;

    private static final String DELETE_DUPLICATES = """
            DELETE FROM inventory_entry e
             WHERE e.id > (SELECT MIN(m.id) FROM inventory_entry m
                            WHERE m.inventory_id = e.inventory_id AND m.item_type_id = e.item_type_id)
            """;

    private static final String UNIQUE_CONSTRAINTS_WITH_COLUMN = """
            SELECT DISTINCT tc.constraint_name
              FROM information_schema.table_constraints tc
              JOIN information_schema.key_column_usage k
                ON k.constraint_schema = tc.constraint_schema AND k.constraint_name = tc.constraint_name
             WHERE tc.constraint_type = 'UNIQUE'
               AND LOWER(tc.table_schema) = LOWER(?)
               AND LOWER(tc.table_name) = ?
               AND LOWER(k.column_name) = ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void afterSingletonsInstantiated() {
        clean();
    }

    /** 옛 열·제약을 지운다. 지울 것이 없으면 아무것도 하지 않는다 */
    public void clean() {
        transactionTemplate.executeWithoutResult(_ -> {
            if (hasColumn(INVENTORY_ENTRY, BOUND)) {
                int merged = jdbcTemplate.update(MERGE_COUNTS);
                int deleted = jdbcTemplate.update(DELETE_DUPLICATES);
                for (String constraint : uniqueConstraintsWith(INVENTORY_ENTRY, BOUND)) {
                    jdbcTemplate.execute("ALTER TABLE " + INVENTORY_ENTRY + " DROP CONSTRAINT " + quote(constraint));
                }
                jdbcTemplate.execute("ALTER TABLE " + INVENTORY_ENTRY + " DROP COLUMN " + BOUND);
                log.info("옛 귀속 열을 지웠습니다: 합친 묶음 {}개, 지운 행 {}개", merged, deleted);
            }
            if (hasColumn(ITEM_TYPE, TRADABLE)) {
                jdbcTemplate.execute("ALTER TABLE " + ITEM_TYPE + " DROP COLUMN " + TRADABLE);
                log.info("옛 거래 가능 열을 지웠습니다.");
            }
            int droppedChecks = dropEnumChecks();
            if (droppedChecks > 0) {
                log.info("enum CHECK 제약 {}개를 지웠습니다.", droppedChecks);
            }
        });
    }

    private boolean hasColumn(String table, String column) {
        Boolean found = jdbcTemplate.execute((ConnectionCallback<Boolean>) connection -> {
            DatabaseMetaData metaData = connection.getMetaData();
            String schema = connection.getSchema();
            return exists(metaData, schema, table, column)
                    || exists(metaData, schema, table.toUpperCase(Locale.ROOT), column.toUpperCase(Locale.ROOT));
        });
        return Boolean.TRUE.equals(found);
    }

    private static boolean exists(DatabaseMetaData metaData, String schema, String table, String column) throws SQLException {
        try (ResultSet columns = metaData.getColumns(null, schema, table, column)) {
            return columns.next();
        }
    }

    private List<String> uniqueConstraintsWith(String table, String column) {
        return jdbcTemplate.queryForList(UNIQUE_CONSTRAINTS_WITH_COLUMN, String.class, currentSchema(), table, column);
    }

    /** PostgreSQL 은 CHECK 이름이 표마다 따로라 이름 조인이 겹칠 수 있어 (표, 이름)으로 한 번씩만 지운다 */
    private int dropEnumChecks() {
        List<TableConstraint> enumChecks = jdbcTemplate.query(CHECK_CONSTRAINTS,
                        (row, _) -> new CheckConstraint(row.getString(1), row.getString(2), row.getString(3)), currentSchema())
                .stream()
                .filter(check -> isEnumCheck(check.clause()))
                .map(check -> new TableConstraint(check.table(), check.name()))
                .distinct()
                .toList();
        for (TableConstraint check : enumChecks) {
            jdbcTemplate.execute("ALTER TABLE " + quote(check.table()) + " DROP CONSTRAINT " + quote(check.name()));
        }
        return enumChecks.size();
    }

    /** CHECK 정의가 한 열을 문자열 상수 목록과 비교하는 꼴(PostgreSQL {@code = ANY (ARRAY[…])}, H2 {@code IN (…)})인지 */
    static boolean isEnumCheck(@Nullable String clause) {
        return clause != null && ENUM_CHECK.matcher(clause).matches();
    }

    private String currentSchema() {
        return requireNonNull(jdbcTemplate.execute((ConnectionCallback<String>) connection -> connection.getSchema()));
    }

    private record CheckConstraint(String table, String name, @Nullable String clause) {
    }

    private record TableConstraint(String table, String name) {
    }

    private static String quote(String identifier) {
        return '"' + identifier.replace("\"", "\"\"") + '"';
    }
}
