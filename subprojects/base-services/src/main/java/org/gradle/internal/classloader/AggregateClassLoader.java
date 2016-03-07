package org.gradle.internal.classloader;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@linkplain MultiParentClassLoader} whose parents are kept to a minimum by checking if each new parent is already
 * part of an existing parent's {@linkplain ClassLoader} hierarchy and vice-versa. Delegates (parents) are ordered such
 * that the one with the shortest hierarchy is checked first.
 */
public class AggregateClassLoader extends MultiParentClassLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(AggregateClassLoader.class);

    private final NavigableSet<CLEntry> delegates = new ConcurrentSkipListSet<CLEntry>();

    public AggregateClassLoader() {
        super();
    }

    @Override
    protected Collection<CLEntry> getParentsInternal() {
        return Collections.unmodifiableCollection(delegates);
    }

    @Override
    public void addParent(ClassLoader parent) {
        Iterator<CLEntry> it = delegates.descendingIterator();
        while (it.hasNext()) {
            CLEntry cle = it.next();
            if (cle.delegatesTo(parent)) {
                LOGGER.debug("Assimilating ClassLoader ({}) with existing delegate: {}", parent, cle.getParent());
                return;
            } else if (cle.isDelegateOf(parent)) {
                LOGGER.debug("Assimilating existing delegate ClassLoader ({}) with new parent: {}", cle.getParent(),
                        parent);
                it.remove();
                break;
            }
        }
        delegates.add(new CLEntry(parent));
    }

    private static class CLEntry extends ClassLoader implements Comparable<CLEntry> {
        private int depth = -1;

        CLEntry(ClassLoader parent) {
            super(parent);
            int d = 0;
            for (ClassLoader cl = getParent(); cl != null; cl = cl.getParent()) {
                ++d;
            }
            depth = d;
        }

        boolean delegatesTo(ClassLoader other) {
            return aDelegatesToB(getParent(), other);
        }

        boolean isDelegateOf(ClassLoader other) {
            return aDelegatesToB(other, getParent());
        }

        @Override
        public int compareTo(CLEntry o) {
            return Integer.compare(depth, o.depth);
        }

    }

    private static boolean aDelegatesToB(ClassLoader a, ClassLoader b) {
        for (; a != null; a = a.getParent()) {
            if (a.equals(b)) {
                return true;
            }
        }
        return false;
    }

}
