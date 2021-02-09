package org.tinylog.impl.format.placeholder;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.tinylog.impl.LogEntry;
import org.tinylog.impl.LogEntryValue;
import org.tinylog.impl.test.LogEntryBuilder;
import org.tinylog.impl.test.PlaceholderRenderer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class UptimePlaceholderTest {

	/**
	 * Verifies that the log entry value {@link LogEntryValue#UPTIME} is defined as required by the uptime placeholder.
	 */
	@Test
	void requiredLogEntryValues() {
		UptimePlaceholder placeholder = new UptimePlaceholder("HH:mm", false);
		assertThat(placeholder.getRequiredLogEntryValues()).containsExactly(LogEntryValue.UPTIME);
	}

	/**
	 * Verifies that the uptime is formatted as expected for all provided examples.
	 *
	 * @param uptime The uptime as {@link Duration} to test
	 * @param pattern The format pattern for formatting the passed uptime
	 * @param expected The expected formatted uptime
	 */
	@ParameterizedTest
	@ArgumentsSource(FormatPatternsProvider.class)
	void renderWithUptime(Duration uptime, String pattern, String expected) {
		UptimePlaceholder placeholder = new UptimePlaceholder(pattern, false);
		PlaceholderRenderer renderer = new PlaceholderRenderer(placeholder);
		LogEntry logEntry = new LogEntryBuilder().uptime(uptime).create();
		assertThat(renderer.render(logEntry)).isEqualTo(expected);
	}

	/**
	 * Verifies that {@code <uptime unknown>} will be output, if the uptime is not set.
	 */
	@Test
	void renderWithoutUptime() {
		UptimePlaceholder placeholder = new UptimePlaceholder("HH:mm", false);
		PlaceholderRenderer renderer = new PlaceholderRenderer(placeholder);
		LogEntry logEntry = new LogEntryBuilder().create();
		assertThat(renderer.render(logEntry)).isEqualTo("<uptime unknown>");
	}

	/**
	 * Verifies that the uptime will be formatted as expected for all provided examples and applied to a
	 * {@link PreparedStatement}, if formatting is enabled for SQL.
	 *
	 * @param uptime The uptime as {@link Duration} to test
	 * @param pattern The format pattern for formatting the passed uptime
	 * @param expected The expected formatted uptime
	 */
	@ParameterizedTest
	@ArgumentsSource(FormatPatternsProvider.class)
	void applyStringWithUptime(Duration uptime, String pattern, String expected) throws SQLException {
		PreparedStatement statement = mock(PreparedStatement.class);
		LogEntry logEntry = new LogEntryBuilder().uptime(uptime).create();
		new UptimePlaceholder(pattern, true).apply(statement, 42, logEntry);
		verify(statement).setString(42, expected);
	}

	/**
	 * Verifies that {@code null} will be applied to a {@link PreparedStatement}, if the uptime is not set.
	 */
	@Test
	void applyStringWithoutUptime() throws SQLException {
		PreparedStatement statement = mock(PreparedStatement.class);
		LogEntry logEntry = new LogEntryBuilder().create();
		new UptimePlaceholder("HH:mm", true).apply(statement, 42, logEntry);
		verify(statement).setString(42, null);
	}

	/**
	 * Verifies that the uptime will be converted into a {@link BigDecimal} as expected for all provided examples and
	 * applied to a {@link PreparedStatement}, if formatting is disabled for SQL.
	 *
	 * @param uptime The uptime as {@link Duration} to test
	 * @param expected The expected generated {@link BigDecimal}
	 */
	@ParameterizedTest
	@ArgumentsSource(BigDecimalsProvider.class)
	void applyBigDecimalWithUptime(Duration uptime, BigDecimal expected) throws SQLException {
		PreparedStatement statement = mock(PreparedStatement.class);
		LogEntry logEntry = new LogEntryBuilder().uptime(uptime).create();
		new UptimePlaceholder("s", false).apply(statement, 42, logEntry);
		verify(statement).setBigDecimal(42, expected);
	}

	/**
	 * Verifies that {@code null} will be applied to a {@link PreparedStatement}, if the uptime is not set.
	 */
	@Test
	void applyBigDecimalWithoutUptime() throws SQLException {
		PreparedStatement statement = mock(PreparedStatement.class);
		LogEntry logEntry = new LogEntryBuilder().create();
		new UptimePlaceholder("HH:mm", false).apply(statement, 42, logEntry);
		verify(statement).setBigDecimal(42, null);
	}

	/**
	 * Arguments provider for providing tuples of source {@link Duration}, format pattern, and the expected formatted
	 * uptime.
	 *
	 * @see #renderWithUptime(Duration, String, String)
	 * @see #applyStringWithUptime(Duration, String, String)
	 */
	private static class FormatPatternsProvider implements ArgumentsProvider {

		@Override
		public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
			/* START IGNORE CODE STYLE */
			return Stream.of(
				/* Minimum second fraction digits */
				Arguments.of(Duration.ofMillis(100).minusNanos(1), "S", "0"),
				Arguments.of(Duration.ofMillis(100)              , "S", "1"),
				Arguments.of(Duration.ofSeconds(1).minusNanos(1) , "S", "9"),
				Arguments.of(Duration.ofSeconds(1)               , "S", "0"),

				/* Maximum second fraction digits */
				Arguments.of(Duration.ofNanos(0)                , "SSSSSSSSS", "000000000"),
				Arguments.of(Duration.ofNanos(1)                , "SSSSSSSSS", "000000001"),
				Arguments.of(Duration.ofSeconds(1).minusNanos(1), "SSSSSSSSS", "999999999"),
				Arguments.of(Duration.ofSeconds(1)              , "SSSSSSSSS", "000000000"),

				/* Too many second fraction digits */
				Arguments.of(Duration.ofNanos(0)                , "SSSSSSSSSS", "0000000000"),
				Arguments.of(Duration.ofNanos(1)                , "SSSSSSSSSS", "0000000010"),
				Arguments.of(Duration.ofSeconds(1).minusNanos(1), "SSSSSSSSSS", "9999999990"),
				Arguments.of(Duration.ofSeconds(1)              , "SSSSSSSSSS", "0000000000"),

				/* Seconds only */
				Arguments.of(Duration.ofSeconds(1).minusNanos(1)  , "s",  "0"),
				Arguments.of(Duration.ofSeconds(1)                , "s",  "1"),
				Arguments.of(Duration.ofSeconds(10).minusNanos(1) , "s",  "9"),
				Arguments.of(Duration.ofSeconds(10)               , "s", "10"),
				Arguments.of(Duration.ofSeconds(100).minusNanos(1), "s", "99"),

				/* Seconds with fraction */
				Arguments.of(Duration.ofSeconds(1).minusNanos(1)  , "s.SSS",  "0.999"),
				Arguments.of(Duration.ofSeconds(1)                , "s.SSS",  "1.000"),
				Arguments.of(Duration.ofSeconds(10).minusNanos(1) , "s.SSS",  "9.999"),
				Arguments.of(Duration.ofSeconds(10)               , "s.SSS", "10.000"),
				Arguments.of(Duration.ofSeconds(100).minusNanos(1), "s.SSS", "99.999"),

				/* Minutes only */
				Arguments.of(Duration.ofMinutes(1).minusNanos(1)  , "m",  "0"),
				Arguments.of(Duration.ofMinutes(1)                , "m",  "1"),
				Arguments.of(Duration.ofMinutes(10).minusNanos(1) , "m",  "9"),
				Arguments.of(Duration.ofMinutes(10)               , "m", "10"),
				Arguments.of(Duration.ofMinutes(100).minusNanos(1), "m", "99"),

				/* Minutes with seconds */
				Arguments.of(Duration.ofMinutes(1).minusNanos(1)  , "m:ss",  "0:59"),
				Arguments.of(Duration.ofMinutes(1)                , "m:ss",  "1:00"),
				Arguments.of(Duration.ofMinutes(10).minusNanos(1) , "m:ss",  "9:59"),
				Arguments.of(Duration.ofMinutes(10)               , "m:ss", "10:00"),
				Arguments.of(Duration.ofMinutes(100).minusNanos(1), "m:ss", "99:59"),

				/* Hours only */
				Arguments.of(Duration.ofHours(1).minusNanos(1)  , "H",  "0"),
				Arguments.of(Duration.ofHours(1)                , "H",  "1"),
				Arguments.of(Duration.ofHours(10).minusNanos(1) , "H",  "9"),
				Arguments.of(Duration.ofHours(10)               , "H", "10"),
				Arguments.of(Duration.ofHours(100).minusNanos(1), "H", "99"),

				/* Hours with minutes */
				Arguments.of(Duration.ofHours(1).minusNanos(1)  , "H:mm",  "0:59"),
				Arguments.of(Duration.ofHours(1)                , "H:mm",  "1:00"),
				Arguments.of(Duration.ofHours(10).minusNanos(1) , "H:mm",  "9:59"),
				Arguments.of(Duration.ofHours(10)               , "H:mm", "10:00"),
				Arguments.of(Duration.ofHours(100).minusNanos(1), "H:mm", "99:59"),

				/* Days only */
				Arguments.of(Duration.ofDays(1).minusNanos(1)  , "d",  "0"),
				Arguments.of(Duration.ofDays(1)                , "d",  "1"),
				Arguments.of(Duration.ofDays(10).minusNanos(1) , "d",  "9"),
				Arguments.of(Duration.ofDays(10)               , "d", "10"),
				Arguments.of(Duration.ofDays(100).minusNanos(1), "d", "99"),

				/* Days with hours */
				Arguments.of(Duration.ofDays(1).minusNanos(1)  , "d:HH",  "0:23"),
				Arguments.of(Duration.ofDays(1)                , "d:HH",  "1:00"),
				Arguments.of(Duration.ofDays(10).minusNanos(1) , "d:HH",  "9:23"),
				Arguments.of(Duration.ofDays(10)               , "d:HH", "10:00"),
				Arguments.of(Duration.ofDays(100).minusNanos(1), "d:HH", "99:23"),

				/* Quotes */
				Arguments.of(Duration.ofDays(1).minusNanos(1), "HH'mm"    , "23'59"   ),
				Arguments.of(Duration.ofDays(1).minusNanos(1), "HH''mm"   , "23'59"   ),
				Arguments.of(Duration.ofDays(1).minusNanos(1), "HH'h'mm"  , "23h59"   ),
				Arguments.of(Duration.ofDays(1).minusNanos(1), "H 'hours'", "23 hours"),

				/* Full pattern */
				Arguments.of(Duration.ofDays(42).minusNanos(1), "ddd:HH:mm:ss.SSS", "041:23:59:59.999")
			);
			/* END IGNORE CODE STYLE */
		}

	}

	/**
	 * Arguments provider for providing tuples of source {@link Duration} and the expected generated
	 * {@link BigDecimal}.
	 *
	 * @see #applyBigDecimalWithUptime(Duration, BigDecimal)
	 */
	private static class BigDecimalsProvider implements ArgumentsProvider {

		@Override
		public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
			/* START IGNORE CODE STYLE */
			return Stream.of(
				Arguments.of(Duration.ofSeconds(0),               new BigDecimal( "0.000000000")),
				Arguments.of(Duration.ofSeconds(1).minusNanos(1), new BigDecimal( "0.999999999")),
				Arguments.of(Duration.ofSeconds(1),               new BigDecimal( "1.000000000")),
				Arguments.of(Duration.ofMinutes(1).minusNanos(1), new BigDecimal("59.999999999")),
				Arguments.of(Duration.ofMinutes(1),               new BigDecimal("60.000000000"))
			);
			/* END IGNORE CODE STYLE */
		}

	}

}