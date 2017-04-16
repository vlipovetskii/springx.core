package vlfsoft.common.spring.util;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Stream;

import vlfsoft.common.jdbc.StreamJdbcQuery;

final public class SpringStreamJdbcUtils {

    private SpringStreamJdbcUtils() {
    }

    /**
     * works with {@link ResultSet} as with Stream
     */
    public static class StreamResultSetExtractor implements ResultSetExtractor<Void> {

        final StreamResultSetRowExtractor mStreamResultSetRowExtractor;

        // PROBLEM: JdbcTemplate calls extractData from StreamResultSetExtractor<RowType>, extractData returns Stream<RowType>,
        // JdbcTemplate calls JdbcUtils.closeResultSet(rs); and thus ResulSet is closed before Stream calls method hasNext, next.
        // As a result: "java.sql.SQLException: Operation not allowed after ResultSet closed".
        // With RxJava there is no this problem because of see. "RxJava vs Streams" in Java development.docx
        // In more details: Stream starts to process elements of stream after extractData finished, when first stream method is called
        // In RptRepositoryTest this method is collect.
        // RxJava Observable emits event for each row in ResultSet inside extractData:
        // See. ObservableResultSetExtractor.onNext and RxJdbcQuery.onNext
        // Solution: Wait until Stream is implemented directly in the JdbcTemplate.
        @Override
        public Void extractData(ResultSet aResultSet) throws DataAccessException {
            mStreamResultSetRowExtractor.extractData(StreamJdbcQuery.getStreamOf(aResultSet));
            return null;
        }

        public StreamResultSetExtractor(final StreamResultSetRowExtractor aStreamResultSetRowExtractor) {
            mStreamResultSetRowExtractor = aStreamResultSetRowExtractor;
        }

    }

    public interface StreamResultSetRowExtractor {
        void extractData(Stream<StreamJdbcQuery.ResultSetRow> aStreamResultSetRow);
    }

    /**
     * See <a href="http://stackoverflow.com/questions/21162753/jdbc-resultset-i-need-a-getdatetime-but-there-is-only-getdate-and-gettimestamp">JDBC ResultSet: I need a getDateTime, but there is only getDate and getTimeStamp</a>
     * To avoid dependencies doubled this method from vlfsoft.common.nui.rxjdbc.RxJdbcQuery
     */
    public static Date getDateTime(final ResultSet aResultSet, int i) throws SQLException {
        Timestamp timestamp = aResultSet.getTimestamp(i);
        if (timestamp == null) return null;
        return new java.util.Date(timestamp.getTime());
    }

    /**
     * To avoid dependencies doubled this method from vlfsoft.common.spring.util.SpringRxJdbcUtils
     */
    public static <T> Optional<T> getOptionalColumnValue(T aColumnValue, ResultSet aResultSet) throws SQLException {
        return aResultSet.wasNull() ? Optional.empty() : Optional.of(aColumnValue);
    }

}
