package org.tinylog.impl.backend;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ServiceLoader;

import org.junit.jupiter.api.Test;
import org.tinylog.core.Framework;
import org.tinylog.core.Level;
import org.tinylog.core.backend.LoggingBackend;
import org.tinylog.core.backend.LoggingBackendBuilder;
import org.tinylog.core.runtime.StackTraceLocation;
import org.tinylog.impl.writers.console.ConsoleWriter;
import org.tinylog.impl.writers.file.FileWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NativeLoggingBackendBuilderTest {

	/**
	 * Verifies that a {@link NativeLoggingBackend} with a sync {@link ConsoleWriter} can be created and used for
	 * logging.
	 */
	@Test
	void syncConsoleWriter() {
		PrintStream mockedOutputStream = mock(PrintStream.class);
		PrintStream originalOutputStream = System.out;
		System.setOut(mockedOutputStream);

		try {
			Framework framework = new Framework(false, false);
			try {
				framework.getConfiguration().set("level", "INFO");
				framework.getConfiguration().set("writer.type", "console");
				framework.getConfiguration().set("writer.pattern", "{level}:{message}");
				framework.startUp();

				LoggingBackend backend = framework.getLoggingBackend();
				assertThat(backend).isInstanceOf(NativeLoggingBackend.class);

				StackTraceLocation location = framework.getRuntime().getStackTraceLocationAtIndex(0);
				backend.log(location.push(), null, Level.DEBUG, null, "Hello World!", null, null);
				backend.log(location.push(), null, Level.INFO, null, "Hello World!", null, null);
			} finally {
				framework.shutDown();
			}

			verify(mockedOutputStream).print("INFO:Hello World!" + System.lineSeparator());
		} finally {
			System.setOut(originalOutputStream);
		}
	}

	/**
	 * Verifies that a {@link NativeLoggingBackend} with an async {@link FileWriter} can be created and used for
	 * logging.
	 */
	@Test
	void asyncFileWriter() throws IOException {
		Path logFile = Files.createTempFile("tinylog", ".log");
		logFile.toFile().deleteOnExit();

		try {
			Framework framework = new Framework(false, false);
			try {
				framework.getConfiguration().set("level", "INFO");
				framework.getConfiguration().set("writer.type", "file");
				framework.getConfiguration().set("writer.file", logFile.toAbsolutePath().toString());
				framework.getConfiguration().set("writer.pattern", "{level}:{message}");
				framework.startUp();

				LoggingBackend backend = framework.getLoggingBackend();
				assertThat(backend).isInstanceOf(NativeLoggingBackend.class);

				StackTraceLocation location = framework.getRuntime().getStackTraceLocationAtIndex(0);
				backend.log(location.push(), null, Level.DEBUG, null, "Hello World!", null, null);
				backend.log(location.push(), null, Level.INFO, null, "Hello World!", null, null);
			} finally {
				framework.shutDown();
			}

			assertThat(logFile).hasContent("INFO:Hello World!" + System.lineSeparator());
		} finally {
			Files.deleteIfExists(logFile);
		}
	}

	/**
	 * Verifies that the builder is registered as service.
	 */
	@Test
	void service() {
		assertThat(ServiceLoader.load(LoggingBackendBuilder.class)).anySatisfy(builder -> {
			assertThat(builder).isInstanceOf(NativeLoggingBackendBuilder.class);
			assertThat(builder.getName()).isEqualTo("tinylog");
		});
	}

}