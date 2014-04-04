/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal;

import org.gridgain.grid.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.logger.log4j.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.lang.*;
import org.gridgain.grid.util.tostring.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;

import static org.gridgain.grid.GridSystemProperties.*;

/**
 *
 */
public class GridLoggerProxy extends GridMetadataAwareAdapter implements GridLogger, GridLifecycleAware,
    Externalizable {
    /** Global flag to enable/disable GridGain debug logging. */
    private static final boolean GG_DEBUG_ENABLED =
        Boolean.valueOf(X.getSystemOrEnv(GridSystemProperties.GG_DEBUG_ENABLED));

    /** */
    private static ThreadLocal<GridBiTuple<String, Object>> stash = new ThreadLocal<GridBiTuple<String, Object>>() {
        @Override protected GridBiTuple<String, Object> initialValue() {
            return F.t2();
        }
    };

    /** */
    private GridLogger impl;

    /** */
    private String gridName;

    /** */
    private String id8;

    /** */
    @GridToStringExclude
    private Object ctgr;

    /** Whether or not to log grid name. */
    private static final boolean logGridName = System.getProperty(GG_LOG_GRID_NAME) != null;

    /**
     * No-arg constructor is required by externalization.
     */
    public GridLoggerProxy() {
        // No-op.
    }

    /**
     *
     * @param impl Logger implementation to proxy to.
     * @param ctgr Optional logger category.
     * @param gridName Grid name (can be {@code null} for default grid).
     * @param id8 Node ID.
     */
    @SuppressWarnings({"IfMayBeConditional", "SimplifiableIfStatement"})
    public GridLoggerProxy(GridLogger impl, @Nullable Object ctgr, @Nullable String gridName, String id8) {
        assert impl != null;

        this.impl = impl;
        this.ctgr = ctgr;
        this.gridName = gridName;
        this.id8 = id8;
    }

    /** {@inheritDoc} */
    @Override public void start() throws GridException {
        U.startLifecycleAware(Collections.singleton(impl));
    }

    /** {@inheritDoc} */
    @Override public void stop() throws GridException {
        U.stopLifecycleAware(this, Collections.singleton(impl));
    }

    /** {@inheritDoc} */
    @Override public GridLogger getLogger(Object ctgr) {
        assert ctgr != null;

        return new GridLoggerProxy(impl.getLogger(ctgr), ctgr, gridName, id8);
    }

    /**
     * If using {@code log4j}, gets name of the file being logged to if one is configured or {@code null} otherwise.
     *
     * @return If using {@code log4j}, gets name of the file being logged to if one is configured
     *      or {@code null} otherwise.
     */
    @Nullable public String fileName() {
        if (!(impl instanceof GridLog4jLogger))
            return null;

        return ((GridLog4jLogger)impl).fileName();
    }

    /** {@inheritDoc} */
    @Override public void trace(String msg) {
        impl.trace(enrich(msg));
    }

    /** {@inheritDoc} */
    @Override public void debug(String msg) {
        impl.debug(enrich(msg));
    }

    /** {@inheritDoc} */
    @Override public void info(String msg) {
        impl.info(enrich(msg));
    }

    /** {@inheritDoc} */
    @Override public void warning(String msg) {
        impl.warning(enrich(msg));
    }

    /** {@inheritDoc} */
    @Override public void warning(String msg, Throwable e) {
        impl.warning(enrich(msg), e);
    }

    /** {@inheritDoc} */
    @Override public void error(String msg) {
        impl.error(enrich(msg));
    }

    /** {@inheritDoc} */
    @Override public void error(String msg, Throwable e) {
        impl.error(enrich(msg), e);
    }

    /** {@inheritDoc} */
    @Override public boolean isTraceEnabled() {
        return GG_DEBUG_ENABLED && impl.isTraceEnabled();
    }

    /** {@inheritDoc} */
    @Override public boolean isDebugEnabled() {
        return GG_DEBUG_ENABLED && impl.isDebugEnabled();
    }

    /** {@inheritDoc} */
    @Override public boolean isInfoEnabled() {
        return impl.isInfoEnabled();
    }

    /** {@inheritDoc} */
    @Override public boolean isQuiet() {
        return impl.isQuiet();
    }

    /**
     * Enriches the log message with grid name if {@link GridSystemProperties#GG_LOG_GRID_NAME}
     * system property is set.
     *
     * @param m Message to enrich.
     * @return Enriched message or the original one.
     */
    private String enrich(@Nullable String m) {
        return logGridName && m != null ? "<" + gridName + '-' + id8 + "> " + m : m;
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        U.writeString(out, gridName);
        out.writeObject(ctgr);
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection"})
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        GridBiTuple<String, Object> t = stash.get();

        t.set1(U.readString(in));
        t.set2(in.readObject());
    }

    /**
     * Reconstructs object on demarshalling.
     *
     * @return Reconstructed object.
     * @throws ObjectStreamException Thrown in case of demarshalling error.
     */
    protected Object readResolve() throws ObjectStreamException {
        try {
            GridBiTuple<String, Object> t = stash.get();

            String gridNameR = t.get1();
            Object ctgrR = t.get2();

            return GridGainEx.gridx(gridNameR).log().getLogger(ctgrR);
        }
        catch (IllegalStateException e) {
            throw U.withCause(new InvalidObjectException(e.getMessage()), e);
        }
        finally {
            stash.remove();
        }
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridLoggerProxy.class, this);
    }
}