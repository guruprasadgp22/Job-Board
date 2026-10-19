package com.guru.jobboard.source;

import java.util.List;

import com.guru.jobboard.entity.Preference;

public interface JobSource {

    /** Stable identifier stored in the jobs.source column. */
    String name();

    default boolean enabled() {
        return true;
    }

    /**
     * True when the source returns every open job matching the search (not just the newest N),
     * so a job missing from its results for several days can be treated as closed.
     */
    default boolean completeListing() {
        return false;
    }

    /**
     * Results are shared between users whose preferences give the same key during one sync run.
     * Sources that ignore preferences return "" so they are fetched only once.
     */
    default String cacheKey(Preference preference) {
        return "";
    }

    /** Fetches candidate jobs. Throwing marks the source as failed for this sync (its jobs are then not treated as unlisted). */
    List<FetchedJob> fetch(Preference preference) throws Exception;
}
