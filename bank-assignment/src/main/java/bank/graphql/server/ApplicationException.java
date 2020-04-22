package bank.graphql.server;

import java.util.List;
import java.util.Map;

import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.language.SourceLocation;

/**
 * Custom exception indicating an applicaton specific exception happened
 *
 */
public class ApplicationException extends RuntimeException implements GraphQLError {
    private static final long serialVersionUID = 1L;
    private final Map<String, Object> extensions;

    public ApplicationException(Throwable t) {
        super(t);
        extensions = Map.of("exception", t.getClass().getName());
    }

    @Override
    public Map<String, Object> getExtensions() {
        return extensions;
    }

    @Override
    public List<SourceLocation> getLocations() {
        return null;
    }

    @Override
    public ErrorType getErrorType() {
        return ErrorType.DataFetchingException;
    }
}