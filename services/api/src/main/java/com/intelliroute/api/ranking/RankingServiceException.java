package com.intelliroute.api.ranking;

/**
 * Raised when the preference-ranking service cannot produce a usable
 * ranking — unreachable, timed out, or returned an invalid response.
 * Callers are expected to fall back to local ranking.
 */
public class RankingServiceException extends RuntimeException {

    public RankingServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public RankingServiceException(String message) {
        super(message);
    }
}
