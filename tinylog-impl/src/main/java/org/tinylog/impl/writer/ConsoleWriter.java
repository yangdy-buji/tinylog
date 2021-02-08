package org.tinylog.impl.writer;

import java.io.PrintStream;
import java.util.EnumSet;
import java.util.Set;

import org.tinylog.core.Level;
import org.tinylog.impl.LogEntry;
import org.tinylog.impl.LogEntryValue;
import org.tinylog.impl.format.Placeholder;

/**
 * Synchronous writer that outputs formatted log entries to {@link System#out} and {@link System#err} respectively.
 */
public class ConsoleWriter implements Writer {

	private static final int BUILDER_CAPACITY = 1024;

	private final Placeholder placeholder;
	private final int threshold;

	/**
	 * @param placeholder The placeholder for formatting log entries
	 * @param threshold Log entries with a severity less than this threshold are output to {@link System#out}. Log
	 *                  entries with a severity greater than or equal to this threshold are output to
	 *                  {@link System#err}.
	 */
	public ConsoleWriter(Placeholder placeholder, Level threshold) {
		this.placeholder = placeholder;
		this.threshold = threshold.ordinal();
	}

	@Override
	public Set<LogEntryValue> getRequiredLogEntryValues() {
		Set<LogEntryValue> values = EnumSet.noneOf(LogEntryValue.class);
		values.addAll(placeholder.getRequiredLogEntryValues());
		values.add(LogEntryValue.LEVEL);
		return values;
	}

	@Override
	public void log(LogEntry entry) {
		StringBuilder builder = new StringBuilder(BUILDER_CAPACITY);
		placeholder.render(builder, entry);

		PrintStream stream = entry.getSeverityLevel().ordinal() <= threshold ? System.err : System.out;
		stream.print(builder.toString());
	}

	@Override
	public void close() {
		// Ignore
	}

}