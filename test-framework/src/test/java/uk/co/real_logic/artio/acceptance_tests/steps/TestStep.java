package uk.co.real_logic.artio.acceptance_tests.steps;

import org.agrona.LangUtil;
import org.junit.Assert;
import uk.co.real_logic.artio.DebugLogger;
import uk.co.real_logic.artio.acceptance_tests.Environment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;

import static java.util.stream.Collectors.toList;
import static uk.co.real_logic.artio.LogTag.FIX_TEST;

public interface TestStep
{
    static List<TestStep> load(final Path path)
    {
        try
        {
            return Files
                .lines(path, StandardCharsets.ISO_8859_1)
                .filter((line) -> line.length() > 0)
                .map(
                    (line) ->
                    {
                        if (line.matches("^[ \t]*#.*"))
                        {
                            return new PrintCommentStep(line);
                        }
                        else if (line.startsWith("I"))
                        {
                            return new InitiateMessageStep(line);
                        }
                        else if (line.startsWith("E"))
                        {
                            return new ExpectMessageStep(line);
                        }
                        else if (line.matches("^i\\d*,?CONNECT"))
                        {
                            return new ConnectToServerStep(line);
                        }
                        else if (line.matches("^iSET_SESSION.*"))
                        {
                            return new ConfigureSessionStep(line);
                        }
                        else if (line.matches("^e\\d*,?DISCONNECT"))
                        {
                            return new ExpectDisconnectStep(line);
                        }

                        DebugLogger.log(FIX_TEST, "Unknown line: " + line);
                        return null;
                    })
                .filter(Objects::nonNull)
                .collect(toList());
        }
        catch (final IOException ex)
        {
            LangUtil.rethrowUnchecked(ex);
            return null;
        }
    }

    void run(Environment environment) throws Exception;

    default void perform(final Environment environment)
    {
        try
        {
            run(environment);
        }
        catch (final Exception ex)
        {
            ex.printStackTrace();
            Assert.fail(ex.getMessage());
        }
    }

    default int getClientId(final Matcher matcher, final String line)
    {
        if (matcher.matches())
        {
            if (matcher.group(1) != null)
            {
                return Integer.parseInt(matcher.group(1));
            }
        }
        else
        {
            Assert.fail("incorrect command: " + line);
        }

        return 1;
    }

    default int getClientId(final Matcher headerMatcher)
    {
        if (headerMatcher.matches())
        {
            return Integer.parseInt(headerMatcher.group(1));
        }
        else
        {
            return 1;
        }
    }
}
